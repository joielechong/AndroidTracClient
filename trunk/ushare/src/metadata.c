/*
[B * metadata.c : GeeXboX uShare CDS Metadata DB.
 * Originally developped for the GeeXboX project.
 * Copyright (C) 2005-2007 Benjamin Zores <ben@geexbox.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdbool.h>

#include <upnp/upnp.h>
#include <upnp/upnptools.h>

#include "mime.h"
#include "metadata.h"
#include "util_iconv.h"
#include "content.h"
#include "gettext.h"
#include "trace.h"

#define TITLE_UNKNOWN "unknown"

#define MAX_URL_SIZE 32

#include <pthread.h>
#include "odbc.h"

typedef struct meta_thread_data_t {
  pthread_t threadid1;
  pthread_t threadid2;
  struct ushare_t *ut;
  int initial_wait;
  int loop_wait;
  int verify_wait;
  pthread_mutex_t db_mutex;
} meta_thread_data;

static meta_thread_data mtd;

static char *getExtension (const char *filename) {
  char *str = NULL;
  
  str = strrchr (filename, '.');
  if (str)
    str++;
  
  return str;
}

static struct mime_type_t *getMimeType (const char *extension) {
  extern struct mime_type_t MIME_Type_List[];
  struct mime_type_t *list;
  
  if (!extension)
    return NULL;
  
  list = MIME_Type_List;
  while (list->extension) {
    if (!strcasecmp (list->extension, extension))
      return list;
    list++;
  }
  
  return NULL;
}

static bool is_valid_extension (const char *extension) {
  if (!extension)
    return false;
  
  if (getMimeType (extension))
    return true;
  
  return false;
}

/* Seperate recursive free() function in order to avoid freeing off
 * the parents child list within the freeing of the first child, as
 * the only entry which is not part of a childs list is the root entry
 */
static void _upnp_entry_free (struct upnp_entry_t *entry) {
//  struct upnp_entry_t **childs;
  
  if (!entry)
    return;
  
  if (entry->fullpath)
    free (entry->fullpath);
  if (entry->title)
    free (entry->title);
  if (entry->artist)
    free (entry->artist);
  if (entry->album)
    free (entry->album);
  if (entry->date)
    free (entry->date);
  if (entry->resolution)
    free (entry->resolution);
  if (entry->duration)
    free (entry->duration);
#ifdef HAVE_DLNA
  if (entry->dlna_profile) {
    free(entry->dlna_profile);
    entry->dlna_profile = NULL;
  }
#endif /* HAVE_DLNA */
  
  //  for (childs = entry->childs; *childs; childs++)
  //    _upnp_entry_free (*childs);
  //  free (entry->childs);
}

void upnp_entry_free (struct ushare_t *ut, struct upnp_entry_t *entry) {
  if (!ut || !entry)
    return;
  
  /* Free all entries (i.e. children) */
  _upnp_entry_free (entry);
  
  free (entry);
}

static xml_convert_t xml_convert[] = {
  {'"' , "&quot;"},
  {'&' , "&amp;"},
  {'\'', "&apos;"},
  {'<' , "&lt;"},
  {'>' , "&gt;"},
  {'\n', "&#xA;"},
  {'\r', "&#xD;"},
  {'\t', "&#x9;"},
  {0, NULL},
};

static char *get_xmlconvert (int c) {
  int j;
  for (j = 0; xml_convert[j].xml; j++) {
    if (c == xml_convert[j].charac)
      return xml_convert[j].xml;
  }
  return NULL;
}

static char *convert_xml (const char *title) {
  char *newtitle, *s, *t, *xml;
  int nbconvert = 0;
  
  /* calculate extra size needed */
  for (t = (char*) title; *t; t++) {
    xml = get_xmlconvert (*t);
    if (xml)
      nbconvert += strlen (xml) - 1;
  }
  if (!nbconvert)
    return NULL;
  
  newtitle = s = (char*) malloc (strlen (title) + nbconvert + 1);
  
  for (t = (char*) title; *t; t++) {
    xml = get_xmlconvert (*t);
    if (xml) {
      strcpy (s, xml);
      s += strlen (xml);
    }
    else
      *s++ = *t;
  }
  *s = '\0';
  
  return newtitle;
}

static struct mime_type_t Container_MIME_Type =
  { NULL, "object.container.storageFolder", NULL};

static struct upnp_entry_t *upnp_entry_new (struct ushare_t *ut, const char *name, const char *fullpath, struct upnp_entry_t *parent, off_t size, int dir)
{
  struct upnp_entry_t *entry = NULL;
  char *title = NULL, *x = NULL;
  char *title_or_name = NULL;
  
  if (!name)
    return NULL;
  
  entry = (struct upnp_entry_t *) malloc (sizeof (struct upnp_entry_t));
  
#ifdef HAVE_DLNA
  entry->dlna_profile = NULL;
  if (ut->dlna_enabled && fullpath && !dir) {
    dlna_profile_t *p = dlna_guess_media_profile (ut->dlna, fullpath);
    if (!p) {
      free (entry);
      log_verbose("Cannot determine file type for  %s\n",fullpath);
      return NULL;
    }
    entry->dlna_profile = p;
    log_info("%s: id=%s, mime=%s, label=%s,class=%d\n",fullpath,p->id,p->mime,p->label,p->class);
    //      log_verbose("%s: id=%s, mime=%s, label=%s,class=%d\n",fullpath,p->id,p->mime,p->label,p->class);
  }
#endif /* HAVE_DLNA */
  
  if (ut->xbox360) {
    if (ut->root_entry)
      entry->id = ut->starting_id + ut->nr_entries++;
    else
      entry->id = 0; /* Creating the root node so don't use the usual IDs */
  } else
    entry->id = ut->starting_id + ut->nr_entries++;
  
  entry->fullpath = fullpath ? strdup (fullpath) : NULL;
  entry->parent = parent;
  entry->child_count =  dir ? 0 : -1;
  entry->title = NULL;
  
  entry->childs = (struct upnp_entry_t **) malloc (sizeof (struct upnp_entry_t *));
  *(entry->childs) = NULL;
  
  if (!dir) {/* item */
    if (ut->dlna_enabled)
      entry->mime_type = NULL;
    else {
      struct mime_type_t *mime = getMimeType (getExtension (name));
      if (!mime) {
	--ut->nr_entries; 
	upnp_entry_free (ut, entry);
	log_error ("Invalid Mime type for %s, entry ignored", name);
	return NULL;
      }
      entry->mime_type = mime;
    }
  } else  /* container */
    entry->mime_type = &Container_MIME_Type;
  
  
  /* Try Iconv'ing the name but if it fails the end device
     may still be able to handle it */
  title = iconv_convert_to_utf8 (name);
  if (title)
    title_or_name = title;
  else {
    if (ut->override_iconv_err) {
      title_or_name = strdup (name);
      log_error ("Entry invalid name id=%d [%s]\n", entry->id, name);
    } else {
      upnp_entry_free (ut, entry);
      log_error ("Freeing entry invalid name id=%d [%s]\n", entry->id, name);
      return NULL;
    }
  }
  
  if (!dir) {
    x = strrchr (title_or_name, '.');
    if (x)  /* avoid displaying file extension */
      *x = '\0';
  }
  x = convert_xml (title_or_name);
  if (x) {
    free (title_or_name);
    title_or_name = x;
  }
  entry->title = title_or_name;
  
  if (!strcmp (title_or_name, "")) { /* DIDL dc:title can't be empty */ 
    free (title_or_name);
    entry->title = strdup (TITLE_UNKNOWN);
  }
  
  entry->size = size;
  
  if (entry->id )
    log_verbose ("Entry->title (%d): %s\n", entry->id, entry->title);
  
  return entry;
}

static void fill_container(struct ushare_t *ut,char * path,int parent_id) {
  struct upnp_entry_t *entry = NULL;
  char *title = NULL;
  int size = 0;
  int newparent;
  
  log_verbose (_("Looking for files in content directory : %s\n"),path);
  
  size = strlen (path);
  if (path[size - 1] == '/')
    path[size - 1] = '\0';
  title = strrchr (path, '/');
  if (title) {
    title++;
  } else {
    /* directly use content directory name if no '/' before basename */
    title = path;
  }
  
  pthread_mutex_lock (&mtd.db_mutex);
  newparent = entry_stored(ut->odbc_ptr,path);
  if (newparent == -1 ) {
    entry = upnp_entry_new (ut, title, path,NULL, -1, true);
    if (entry) 
      newparent = store_entry(ut->odbc_ptr,entry,parent_id);
  }
  pthread_mutex_unlock (&mtd.db_mutex);
  
  struct dirent **namelist;
  int n,i;
  
  n = scandir (path, &namelist, 0, alphasort);
  if (n < 0) {
    perror ("scandir");
    return;
  }
  
  for (i = 0; i < n; i++) {
    struct stat st;
    char *fullpath = NULL;
    
    if (namelist[i]->d_name[0] == '.') {
      free (namelist[i]);
      continue;
    }
    
    fullpath = (char *)malloc (strlen (path) + strlen (namelist[i]->d_name) + 2);
    sprintf (fullpath, "%s/%s", path, namelist[i]->d_name);
    
    log_verbose ("%s\n", fullpath);
    
    if (stat (fullpath, &st) < 0) {
      free (namelist[i]);
      free (fullpath);
      log_info("(%s niet gevonden ondanks scandir\n",fullpath);
      continue;
    }
    
    if (S_ISDIR (st.st_mode)) {
      fill_container(ut,fullpath,newparent);
    } else {
      if (ut->dlna_enabled || is_valid_extension (getExtension (fullpath))) {
	pthread_mutex_lock (&mtd.db_mutex);
	if (entry_stored(ut->odbc_ptr,fullpath) == -1 ) {
	  struct upnp_entry_t *child = NULL;
	  child = upnp_entry_new (ut, namelist[i]->d_name, fullpath, NULL, st.st_size, false);
	  if (child) 
	    store_entry(ut->odbc_ptr,child,newparent);
	}
	pthread_mutex_unlock (&mtd.db_mutex);
      }
    }
    free (namelist[i]);
    free (fullpath);
  }
  free (namelist);
}

static void *newfilesthread(void *a __attribute__ ((unused))) {
  struct ushare_t *ut=mtd.ut;
  int i;
  
  sleep(mtd.initial_wait);
  while (1) {
    log_verbose(_("Starting threadloop\n"));    
    /* process new files */
    
    for (i=0 ; i < ut->contentlist->count ; i++) {
      fill_container(ut,ut->contentlist->content[i],0);
    }
    
    sleep(mtd.loop_wait);
  }
  return NULL;
}

static void *verifythread(void *a __attribute__ ((unused))) {
  long last_id = 0, new_id, size;
  char *filename;
  int odbc_ptr = mtd.ut->odbc_ptr;
  struct stat buf;
  int res;
  
  pthread_mutex_lock (&mtd.db_mutex);
  last_id = get_last_entry(odbc_ptr);
  pthread_mutex_unlock (&mtd.db_mutex);
  
  last_id = rand()*((double)last_id)/((double)RAND_MAX);
  log_info("Verification starting at %ld\n",last_id);
  
  while(1) {
    pthread_mutex_lock (&mtd.db_mutex);
    filename = get_next(odbc_ptr,last_id,&new_id,&size);
    if ((filename != NULL) && ((res=stat(filename,&buf)) == -1)) {
      del_entry(odbc_ptr,filename);
      log_info("removed: last_id=%ld new_id=%ld path=%s\n",last_id,new_id,filename);
    }
    if (filename && !res && size && size != buf.st_size)
      upd_size(odbc_ptr,filename,size);
    pthread_mutex_unlock (&mtd.db_mutex);
    if (filename)
      free(filename);
    last_id = new_id;
    sleep(mtd.verify_wait);
  }
}

struct upnp_entry_t *upnp_get_entry (struct ushare_t *ut, int id) {
  struct upnp_entry_t *entry;
  
  log_verbose ("Looking for entry id %d\n", id);
  pthread_mutex_lock (&mtd.db_mutex);
  entry = fetch_entry(ut->odbc_ptr,id);
  pthread_mutex_unlock (&mtd.db_mutex);
  return entry;
}

void build_metadata_db(struct ushare_t *ut) {
  struct upnp_entry_t *root_entry;
  
  log_info (_("Building Metadata List ...\n"));
  ut->odbc_ptr = init_odbc(ut->dsn);
  if (ut->odbc_ptr == -1) {
    log_error("Cannot access database");
    exit(2);
  }
  if (entry_stored(ut->odbc_ptr,"") != 0) {
    root_entry = upnp_entry_new (ut, "root", "", NULL, -1, true);
    store_entry(ut->odbc_ptr,root_entry,-1);
  }
  
  ut->nr_entries = get_last_entry(ut->odbc_ptr)+1;
  
  ut->init = 1;
  
  mtd.ut = ut;
  mtd.initial_wait=30;
  mtd.loop_wait=12*3600;   /* this must become configurable */
  mtd.verify_wait=1;
  
  pthread_mutex_init (&mtd.db_mutex, NULL);
  
  log_verbose(_("Starting newfilesdata thread...\n"));
  if (pthread_create(&mtd.threadid1,NULL,newfilesthread,NULL))
    log_info(_("New files thread failed to start, no dynamic updates\n"));
  //  if (pthread_create(&mtd.threadid2,NULL,verifythread,NULL))
  //  log_info(_("Metadata thread failed to start, no dynamic updates\n"));
}
