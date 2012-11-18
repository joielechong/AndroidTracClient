#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "metadata.h"
#include <sql.h>
#include <sqlext.h>
#include "odbc.h"
#include "mime.h"
#include "trace.h"
#include "util_iconv.h"

static void extract_error (char *caller,char *fn, SQLHANDLE handle, SQLSMALLINT type) {
  SQLINTEGER i=0;
  SQLINTEGER native;
  SQLCHAR state[7];
  SQLCHAR text[256];
  SQLSMALLINT len;
  SQLRETURN ret;
  
  log_info("\nThe driver reported the following diagnostics whilst running %s - %s\n\n",caller,fn);
  do {
    ret = SQLGetDiagRec(type,handle,++i,state,&native,text,sizeof(text),&len);
    if (SQL_SUCCEEDED(ret))
      log_info("%s:%ld:%ld:%s\n",state,i,native,text);
  }
  while (ret == SQL_SUCCESS);
}

typedef struct ushare_odbc_t {
  SQLHENV env;
  SQLHDBC dbc;
  SQLCHAR outstr[1024];
  SQLSMALLINT outstrlen;
  SQLHSTMT es_stmt;
  SQLHSTMT store_stmt;
  SQLHSTMT fetch_stmt;
  SQLHSTMT child_stmt;
  SQLHSTMT count_stmt;
  SQLHSTMT del_stmt;
  SQLHSTMT loop_stmt;
  SQLHSTMT size_stmt;
  pthread_mutex_t db_mutex;
} ushare_odbc;

static ushare_odbc uo;	

int init_odbc(const char *dsn) {
  SQLRETURN ret;
  
  SQLAllocHandle(SQL_HANDLE_ENV,SQL_NULL_HANDLE, &uo.env);
  SQLSetEnvAttr(uo.env,SQL_ATTR_ODBC_VERSION, (void *)SQL_OV_ODBC3,0);
  
  SQLAllocHandle(SQL_HANDLE_DBC,uo.env,&uo.dbc);
  ret=SQLDriverConnect(uo.dbc,NULL,(SQLCHAR *)dsn, SQL_NTS, uo.outstr,sizeof(uo.outstr),&uo.outstrlen,SQL_DRIVER_COMPLETE);
  if (SQL_SUCCEEDED(ret)) {
    log_verbose("Connected\n");
    log_verbose("Returned connection string was:\n\t%s\n", uo.outstr);    
    if (ret == SQL_SUCCESS_WITH_INFO)
      extract_error("init_odbc","SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
    
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.es_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.es_stmt,(SQLCHAR *)"SELECT id FROM ms.mediacontent WHERE fullpath=?",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare es",uo.dbc,SQL_HANDLE_DBC);
      uo.es_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.store_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.store_stmt,(SQLCHAR *)"INSERT INTO ms.mediacontent (id,fullpath,parent_id,dlna_mime,dlna_id,title,size,dlna_class) VALUES ((select max(id)+1 from ms.mediacontent),?,?,?,?,?,?,?)",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare store",uo.dbc,SQL_HANDLE_DBC);
      uo.store_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.fetch_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.fetch_stmt,(SQLCHAR *)"SELECT fullpath,dlna_mime,dlna_id,title,size,dlna_class,date,artist,album,resolution,duration,genre FROM ms.mediacontent where id=?",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare fetch",uo.dbc,SQL_HANDLE_DBC);
      uo.fetch_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.child_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.child_stmt,(SQLCHAR *)"SELECT id,fullpath,dlna_mime,dlna_id,title,size,dlna_class,date,artist,album,resolution,duration,genre FROM ms.mediacontent where parent_id=? ORDER BY upper(title)",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare child",uo.dbc,SQL_HANDLE_DBC);
      uo.child_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.count_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.count_stmt,(SQLCHAR *)"SELECT count(*) FROM ms.mediacontent where parent_id=?",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare count",uo.dbc,SQL_HANDLE_DBC);
      uo.count_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.del_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.del_stmt,(SQLCHAR *)"DELETE FROM ms.mediacontent where fullpath=?",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare del",uo.dbc,SQL_HANDLE_DBC);
      uo.del_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.size_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.size_stmt,(SQLCHAR *)"UPDATE ms.mediacontent set size=? where fullpath=?",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare size",uo.dbc,SQL_HANDLE_DBC);
      uo.size_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.loop_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.loop_stmt,(SQLCHAR *)"SELECT fullpath,id,size FROM ms.mediacontent where (NOT (fullpath like 'http://%') OR (fullpath like 'https://%') OR (fullpath like 'ftp://%')) AND id = (SELECT min(id) FROM ms.mediacontent WHERE id>?)",SQL_NTS))) {
      extract_error("init_odbc","SQLPrepare loop",uo.dbc,SQL_HANDLE_DBC);
      uo.loop_stmt = NULL;
    }
    return 1;
  } else {
    extract_error("init_odbc","SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
    SQLFreeHandle(SQL_HANDLE_DBC,uo.dbc);
    SQLFreeHandle(SQL_HANDLE_ENV,uo.env);
    return -1;
  }
}

void odbc_finish(int odbc_ptr) {
  if (odbc_ptr >=0) {
    SQLDisconnect(uo.dbc);
    SQLFreeHandle(SQL_HANDLE_DBC,uo.dbc);
    SQLFreeHandle(SQL_HANDLE_ENV,uo.env);
  }
}
#if 0
char *get_next(int odbc_ptr,long from_id,long *new_id, long *size) {
  SQLRETURN ret;
  SQLINTEGER indicator[4];
  char filename[255];
  SQLINTEGER rows;

  if (odbc_ptr < 0)
    return NULL;
  
  SQLFreeStmt(uo.loop_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.loop_stmt, 1, SQL_PARAM_INPUT, SQL_C_ULONG, SQL_INTEGER, sizeof(long), 0, &from_id, sizeof(from_id), NULL);
  ret = SQLBindCol( uo.loop_stmt, 1, SQL_C_CHAR, filename,255,&indicator[1]);
  ret = SQLBindCol( uo.loop_stmt, 2, SQL_C_LONG, new_id,sizeof(*new_id),&indicator[2]);
  ret = SQLBindCol( uo.loop_stmt, 3, SQL_C_LONG, size,sizeof(*size),&indicator[3]);
  ret = SQLExecute(uo.loop_stmt);
  SQLRowCount(uo.loop_stmt,&rows);
  if (rows == 0) {
    *new_id = 0;
    *size = 0;
    return NULL;
  }
  ret = SQLFetch(uo.loop_stmt);
  if (indicator[2] == SQL_NULL_DATA)
    *new_id = 0;
  if (indicator[3] == SQL_NULL_DATA)
    *size = 0;
  return indicator[1] == SQL_NULL_DATA ? NULL : strdup(filename);
}

void del_entry_u8(int odbc_ptr,char *filename) {
  SQLRETURN ret;
  
  if (odbc_ptr < 0)
    return;
  
  SQLFreeStmt(uo.del_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.del_stmt, 1, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, filename, strlen(filename), NULL);
  ret = SQLExecute(uo.del_stmt);
  return;
}

void del_entry(int odbc_ptr,char *filename) {
  char *filu8;

  filu8=iconv_convert_to_utf8(filename);
  del_entry_u8(odbc_ptr,filu8);
  free(filu8);
}
#endif

void upd_size_u8(int odbc_ptr,char *filename,long size) {
  SQLRETURN ret;
  
  if (odbc_ptr < 0)
    return;
  
  SQLFreeStmt(uo.size_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.size_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(size), 0, &size, sizeof(size), NULL);
  ret = SQLBindParameter(uo.size_stmt, 2, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, filename, strlen(filename), NULL);
  ret = SQLExecute(uo.size_stmt);
  return;
}

void upd_size(int odbc_ptr,char *filename,long size) {
  char *filu8;

  filu8=iconv_convert_to_utf8(filename);
  upd_size_u8(odbc_ptr,filu8,size);
  free(filu8);
}

long entry_stored_u8(int odbc_ptr,char *path)
{
  SQLRETURN ret;
  SQLINTEGER rows;
  long retval;
  SQLINTEGER indicator;
  char *lastcall = NULL;
  
  if (odbc_ptr < 0)
    return -1;
  
  SQLFreeStmt(uo.es_stmt,SQL_CLOSE);
  lastcall = "SQLBindParameter";
  if (SQL_SUCCEEDED(ret = SQLBindParameter(uo.es_stmt, 1, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, path, strlen(path), NULL))) {
    lastcall = "SQLBindCol";
    if (SQL_SUCCEEDED(ret = SQLBindCol( uo.es_stmt, 1, SQL_C_LONG, &retval,sizeof(retval),&indicator))) {
      lastcall = "SQLExecute";
      if (SQL_SUCCEEDED(ret = SQLExecute(uo.es_stmt))) {
        SQLRowCount(uo.es_stmt,&rows);
	if (rows == 1) {
	  lastcall = "SQLFetch";
	  if (SQL_SUCCEEDED(ret=SQLFetch(uo.es_stmt))) {
	    if (indicator != SQL_NULL_DATA) 
              lastcall=NULL;
	  }
	} else {
	  lastcall = NULL;
          retval = -1;
	}
      }
    }
  }
  if (lastcall) {
    extract_error("entry_stored",lastcall,uo.dbc,SQL_HANDLE_DBC);
    log_info("   path = %s\n",path);
  }
  return retval;
}

long entry_stored(int odbc_ptr,char *path)
{
  char *filu8;
  long retval;

  filu8=iconv_convert_to_utf8(path);
  retval = entry_stored_u8(odbc_ptr,filu8);
  free(filu8);
  return retval;
}

static long get_child_count(int odbc_ptr,long id) {
  SQLRETURN ret;
  long retval;
  SQLINTEGER indicator;
  char *lastcall = NULL;
  
  if (odbc_ptr < 0)
    return -1;
  

  SQLFreeStmt(uo.count_stmt,SQL_CLOSE);
  lastcall = "SQLBindParameter";
  if (SQL_SUCCEEDED(ret = SQLBindParameter(uo.count_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(id), 0, &id, sizeof(id), NULL))) {
    lastcall = "SQLBindCol";
    if (SQL_SUCCEEDED(ret = SQLBindCol( uo.count_stmt, 1, SQL_C_LONG, &retval,sizeof(retval),&indicator))) {
      lastcall = "SQLExecute";
      if (SQL_SUCCEEDED(ret = SQLExecute(uo.count_stmt))) {
        lastcall = "SQLFetch";
        if (SQL_SUCCEEDED(ret=SQLFetch(uo.count_stmt))) {
          if (indicator == SQL_NULL_DATA) return 0;
	  return retval;
	}
      }
    }
  }
  extract_error("get_child_count",lastcall,uo.dbc,SQL_HANDLE_DBC);
  return 0;
}

long get_last_entry(int odbc_ptr) {
  SQLRETURN ret;
  long retval;
  SQLINTEGER indicator;
  SQLHSTMT hstmt;
  
  if (odbc_ptr < 0)
    return -1;
  
  SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&hstmt);
  ret=SQLPrepare(hstmt,(SQLCHAR *)"SELECT max(id) FROM ms.mediacontent",SQL_NTS);
  ret = SQLBindCol(hstmt, 1, SQL_C_LONG, &retval,sizeof(retval),&indicator);
  SQLExecute(hstmt);
  SQLFetch(hstmt);
  if (indicator == SQL_NULL_DATA) return 0;
  return retval;
}

static char *make_time(long duration) {
  long hours, mins, secs;
  char result[20];
  
  secs=duration;
  hours = secs / 3600;
  secs = secs % 3600;
  mins = secs / 60;
  secs = secs % 60;
  
  sprintf(result,"%ld:%2.2ld:%2.2ld",hours,mins,secs);
  return strdup(result);
}

static struct mime_type_t Container_MIME_Type =
  { NULL, "object.container.storageFolder", NULL};

struct upnp_entry_t *fetch_entry(int odbc_ptr,int id) {
  SQLRETURN ret;
  SQLINTEGER indicator[13];
  struct upnp_entry_t *entry;
  char fullpath[512];
  char dlna_mime[512];
  char dlna_id[512];
  long dlna_class;
  char title[512];
  long size;
  char date[20];
  char artist[64];
  char album[255];
  char resolution[255];
  long duration;
  char genre[255];
  
  if (odbc_ptr < 0)
    return NULL;
  
  SQLFreeStmt(uo.fetch_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.fetch_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, &id, sizeof(id), NULL);
  ret =  SQLBindCol( uo.fetch_stmt, 1, SQL_C_CHAR, &fullpath,sizeof(fullpath),&indicator[1]);
  ret =  SQLBindCol( uo.fetch_stmt, 2, SQL_C_CHAR, &dlna_mime,sizeof(dlna_mime),&indicator[2]);
  ret =  SQLBindCol( uo.fetch_stmt, 3, SQL_C_CHAR, &dlna_id,sizeof(dlna_id),&indicator[3]);
  ret =  SQLBindCol( uo.fetch_stmt, 4, SQL_C_CHAR, &title,sizeof(title),&indicator[4]);
  ret =  SQLBindCol( uo.fetch_stmt, 5, SQL_C_ULONG, &size,sizeof(size),&indicator[5]);
  ret =  SQLBindCol( uo.fetch_stmt, 6, SQL_C_ULONG, &dlna_class,sizeof(dlna_class),&indicator[6]);
  ret =  SQLBindCol( uo.fetch_stmt, 7, SQL_C_CHAR, &date,sizeof(date),&indicator[7]);
  ret =  SQLBindCol( uo.fetch_stmt, 8, SQL_C_CHAR, &artist,sizeof(artist),&indicator[8]);
  ret =  SQLBindCol( uo.fetch_stmt, 9, SQL_C_CHAR, &album,sizeof(album),&indicator[9]);
  ret =  SQLBindCol( uo.fetch_stmt, 10, SQL_C_CHAR, &resolution,sizeof(resolution),&indicator[10]);
  ret =  SQLBindCol( uo.fetch_stmt, 11, SQL_C_ULONG, &duration,sizeof(duration),&indicator[11]);
  ret =  SQLBindCol( uo.fetch_stmt, 12, SQL_C_CHAR, &genre,sizeof(genre),&indicator[12]);
  ret = SQLExecute(uo.fetch_stmt);
  ret = SQLFetch(uo.fetch_stmt);
  
  entry = (struct upnp_entry_t *) malloc (sizeof (struct upnp_entry_t));
  memset(entry,0,sizeof(*entry));
  entry->dlna_profile=malloc(sizeof(dlna_profile_t));
  memset(entry->dlna_profile,0,sizeof(entry->dlna_profile));
  
  entry->id=id;
  entry->fullpath = indicator[1] == SQL_NULL_DATA ? NULL : strdup(fullpath);
  if (indicator[2] == SQL_NULL_DATA) {
    entry->dlna_profile->mime = NULL;
    entry->child_count=get_child_count(odbc_ptr,id);
    entry->mime_type = &Container_MIME_Type;
  } else {
    entry->dlna_profile->mime=strdup(dlna_mime);
    entry->child_count = -1;
    entry->mime_type = NULL;
  }
  entry->dlna_profile->id = indicator[3] == SQL_NULL_DATA ? NULL : strdup(dlna_id);
  entry->title = indicator[4] == SQL_NULL_DATA ? NULL : strdup(title);
  entry->size = indicator[5] == SQL_NULL_DATA ? 0 : size;
  entry->dlna_profile->class = indicator[6] == SQL_NULL_DATA ? 0 : dlna_class;
  entry->date = indicator[7] == SQL_NULL_DATA? NULL : strdup(date);
  entry->artist = indicator[8] == SQL_NULL_DATA? NULL : strdup(artist);
  entry->album = indicator[9] == SQL_NULL_DATA? NULL : strdup(album);
  entry->resolution = indicator[10] == SQL_NULL_DATA ? NULL : strdup(resolution);
  entry->duration = indicator[11] == SQL_NULL_DATA ? NULL : make_time(duration);
  entry->genre = indicator[12] == SQL_NULL_DATA ? NULL : strdup(genre);

  entry->childs = NULL;
  
  return entry;
}

struct upnp_entry_t **fetch_children(int odbc_ptr,struct upnp_entry_t *parent)
{
  SQLRETURN ret;
  SQLINTEGER indicator[14];
  struct upnp_entry_t *entry;
  struct upnp_entry_t **childs;
  char fullpath[512];
  char dlna_mime[512];
  char dlna_id[512];
  long dlna_class;
  char title[512];
  SQLINTEGER i,rows;
  long size,id;
  char date[20];
  char artist[64];
  char album[255];
  char resolution[255];
  long duration;
  char genre[255];
  
  if (odbc_ptr < 0)
    return NULL;
  
  SQLFreeStmt(uo.child_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.child_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, &parent->id, sizeof(parent->id), NULL);
  ret =  SQLBindCol( uo.child_stmt, 1, SQL_C_ULONG, &id,sizeof(id),&indicator[1]);
  ret =  SQLBindCol( uo.child_stmt, 2, SQL_C_CHAR, &fullpath,sizeof(fullpath),&indicator[2]);
  ret =  SQLBindCol( uo.child_stmt, 3, SQL_C_CHAR, &dlna_mime,sizeof(dlna_mime),&indicator[3]);
  ret =  SQLBindCol( uo.child_stmt, 4, SQL_C_CHAR, &dlna_id,sizeof(dlna_id),&indicator[4]);
  ret =  SQLBindCol( uo.child_stmt, 5, SQL_C_CHAR, &title,sizeof(title),&indicator[5]);
  ret =  SQLBindCol( uo.child_stmt, 6, SQL_C_ULONG, &size,sizeof(size),&indicator[6]);
  ret =  SQLBindCol( uo.child_stmt, 7, SQL_C_ULONG, &dlna_class,sizeof(dlna_class),&indicator[7]);
  ret =  SQLBindCol( uo.child_stmt, 8, SQL_C_CHAR, &date,sizeof(date),&indicator[8]);
  ret =  SQLBindCol( uo.child_stmt, 9, SQL_C_CHAR, &artist,sizeof(artist),&indicator[9]);
  ret =  SQLBindCol( uo.child_stmt, 10, SQL_C_CHAR, &album,sizeof(album),&indicator[10]);
  ret =  SQLBindCol( uo.child_stmt, 11, SQL_C_CHAR, &resolution,sizeof(resolution),&indicator[11]);
  ret =  SQLBindCol( uo.child_stmt, 12, SQL_C_ULONG, &duration,sizeof(duration),&indicator[12]);
  ret =  SQLBindCol( uo.child_stmt, 13, SQL_C_CHAR, &genre,sizeof(genre),&indicator[13]);
  ret = SQLExecute(uo.child_stmt);
  
  SQLRowCount(uo.child_stmt,&rows);
  
  childs = (struct upnp_entry_t **) calloc (sizeof (struct upnp_entry_t *),rows+1);
  
  for (i=1;i<=rows;i++) {
    ret = SQLFetch(uo.child_stmt);
    entry = (struct upnp_entry_t *) malloc (sizeof (struct upnp_entry_t));
    memset(entry,0,sizeof(*entry));
    entry->dlna_profile=malloc(sizeof(dlna_profile_t));
    memset(entry->dlna_profile,0,sizeof(entry->dlna_profile));
    
    entry->id=id;
  entry->id=id;
  entry->fullpath = indicator[2] == SQL_NULL_DATA ? NULL : strdup(fullpath);
  if (indicator[3] == SQL_NULL_DATA) {
    entry->dlna_profile->mime = NULL;
    entry->child_count=get_child_count(odbc_ptr,id);
    entry->mime_type = &Container_MIME_Type;
  } else {
    entry->dlna_profile->mime=strdup(dlna_mime);
    entry->child_count = -1;
    entry->mime_type = NULL;
  }
  entry->dlna_profile->id = indicator[4] == SQL_NULL_DATA ? NULL : strdup(dlna_id);
  entry->title = indicator[5] == SQL_NULL_DATA ? NULL : strdup(title);
  entry->size = indicator[6] == SQL_NULL_DATA ? 0 : size;
  entry->dlna_profile->class = indicator[7] == SQL_NULL_DATA ? 0 : dlna_class;
  entry->date = indicator[8] == SQL_NULL_DATA? NULL : strdup(date);
  entry->artist = indicator[9] == SQL_NULL_DATA? NULL : strdup(artist);
  entry->album = indicator[10] == SQL_NULL_DATA? NULL : strdup(album);
  entry->resolution = indicator[11] == SQL_NULL_DATA ? NULL : strdup(resolution);
  entry->duration = indicator[12] == SQL_NULL_DATA ? NULL : make_time(duration);
  entry->genre = indicator[13] == SQL_NULL_DATA ? NULL : strdup(genre);
    
    entry->childs = NULL;
    childs[i-1]=entry;
  }
  childs[rows] = NULL;
  return childs;
}

int store_entry(int odbc_ptr,struct upnp_entry_t *entry,int parent_id)
{
  SQLRETURN ret;
  SQLINTEGER null = SQL_NULL_DATA;
  SQLHSTMT stmt;
 
  log_info("\nstore_entry, id=%d, path=%s\n",entry->id,entry->fullpath)
  if (odbc_ptr < 0)
    return 0;

  if (entry->id == 0) {
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&stmt);
    ret=SQLPrepare(stmt,(SQLCHAR *)"INSERT INTO ms.mediacontent (id,fullpath,parent_id,dlna_mime,dlna_id,title,size,dlna_class) VALUES (0,?,?,?,?,?,?,?)",SQL_NTS);
  } else {
    stmt = uo.store_stmt;
  }
  
  SQLFreeStmt(stmt,SQL_CLOSE);
  SQLBindParameter(stmt, 1, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, entry->fullpath, strlen(entry->fullpath), NULL);
  SQLBindParameter(stmt, 2, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER,sizeof(parent_id), 0, &parent_id, sizeof(parent_id), NULL);
  if (entry->dlna_profile != NULL) {
    SQLBindParameter(stmt, 3, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->mime, strlen(entry->dlna_profile->mime), NULL);
    SQLBindParameter(stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->id, strlen(entry->dlna_profile->id), NULL);
    SQLBindParameter(stmt, 7, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, &entry->dlna_profile->class, sizeof(entry->dlna_profile->class), NULL);
  } else {
    SQLBindParameter(stmt, 3, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
    SQLBindParameter(stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
    SQLBindParameter(stmt, 7, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, NULL, 0, &null);
  }
  SQLBindParameter(stmt, 5, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->title, strlen(entry->title), NULL);
  if (entry->size >= 0) {
    SQLBindParameter(stmt, 6, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, &entry->size, sizeof(entry->size), NULL);
  } else {
    SQLBindParameter(stmt, 6, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, NULL, 0, &null);
  }
  if (!SQL_SUCCEEDED(ret = SQLExecute(stmt))) {
    fprintf(stderr,"Driver reported the following diagnostics\n");
    extract_error("store_entry","SQLExecute",uo.dbc,SQL_HANDLE_DBC);
  }
  entry->id = entry_stored(odbc_ptr,entry->fullpath);
  return entry->id;
}
