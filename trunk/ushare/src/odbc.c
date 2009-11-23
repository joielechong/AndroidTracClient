#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "metadata.h"
#include <sql.h>
#include <sqlext.h>
#include "odbc.h"
#include "mime.h"
#include "trace.h"

static void extract_error (char *caller,char *fn, SQLHANDLE handle, SQLSMALLINT type) {
  SQLINTEGER i=0;
  SQLINTEGER native;
  SQLCHAR state[7];
  SQLCHAR text[256];
  SQLSMALLINT len;
  SQLRETURN ret;
  
  fprintf(stderr,"\nThe driver reported the following diagnostics whilst running %s - %s\n\n",caller,fn);
  do {
    ret = SQLGetDiagRec(type,handle,++i,state,&native,text,sizeof(text),&len);
    if (SQL_SUCCEEDED(ret))
      printf("%s:%ld:%ld:%s\n",state,i,native,text);
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
    
    if (ret == SQL_SUCCESS_WITH_INFO) {
      printf("Driver reported the following diagnostics\n");
      extract_error("init_odbc","SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.es_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.es_stmt,(SQLCHAR *)"SELECT id FROM ms.mediacontent WHERE fullpath=?",SQL_NTS))) {
      uo.es_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.store_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.store_stmt,(SQLCHAR *)"INSERT INTO ms.mediacontent (id,fullpath,parent_id,dlna_mime,dlna_id,title,url,size) VALUES ((select max(id)+1 from ms.mediacontent),?,?,?,?,?,?,?)",SQL_NTS))) {
      uo.store_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.fetch_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.fetch_stmt,(SQLCHAR *)"SELECT fullpath,dlna_mime,dlna_id,title,url,size FROM ms.mediacontent where id=?",SQL_NTS))) {
      uo.fetch_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.child_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.child_stmt,(SQLCHAR *)"SELECT id,fullpath,dlna_mime,dlna_id,title,url,size FROM ms.mediacontent where parent_id=?",SQL_NTS))) {
      uo.child_stmt = NULL;
    }
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.count_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.count_stmt,(SQLCHAR *)"SELECT count(*) FROM ms.mediacontent where parent_id=?",SQL_NTS))) {
      uo.count_stmt = NULL;
    }
    return 1;
  } else {
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

long entry_stored(int odbc_ptr,char *path)
{
  SQLRETURN ret;
  SQLINTEGER rows;
  long retval = -1;
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
	if (rows != 1) return -1;
        lastcall = "SQLFetch";
        if (SQL_SUCCEEDED(ret=SQLFetch(uo.es_stmt))) {
          if (indicator == SQL_NULL_DATA) return -1;
	  return retval;
	}
      }
    }
  }
  
  printf("Driver reported the following diagnostics\n");
  extract_error("entry_stored",lastcall,uo.dbc,SQL_HANDLE_DBC);
  return -1;
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
  
  printf("Driver reported the following diagnostics\n");
  extract_error("get_child_count",lastcall,uo.dbc,SQL_HANDLE_DBC);
  return 0;
}

static struct mime_type_t Container_MIME_Type =
  { NULL, "object.container.storageFolder", NULL};

struct upnp_entry_t *fetch_entry(int odbc_ptr,int id) {
  SQLRETURN ret;
  SQLINTEGER indicator[8];
  struct upnp_entry_t *entry;
  char fullpath[512];
  char dlna_mime[512];
  char dlna_id[512];
  char title[512];
  char url[512];
  long size;
  
  if (odbc_ptr < 0)
    return NULL;
  
  entry = (struct upnp_entry_t *) malloc (sizeof (struct upnp_entry_t));
  memset(entry,0,sizeof(*entry));
  SQLFreeStmt(uo.fetch_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.fetch_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, &id, sizeof(id), NULL);
  ret =  SQLBindCol( uo.fetch_stmt, 1, SQL_C_CHAR, &fullpath,sizeof(fullpath),&indicator[1]);
  ret =  SQLBindCol( uo.fetch_stmt, 2, SQL_C_CHAR, &dlna_mime,sizeof(dlna_mime),&indicator[2]);
  ret =  SQLBindCol( uo.fetch_stmt, 3, SQL_C_CHAR, &dlna_id,sizeof(dlna_id),&indicator[3]);
  ret =  SQLBindCol( uo.fetch_stmt, 4, SQL_C_CHAR, &title,sizeof(title),&indicator[4]);
  ret =  SQLBindCol( uo.fetch_stmt, 5, SQL_C_CHAR, &url,sizeof(url),&indicator[5]);
  ret =  SQLBindCol( uo.fetch_stmt, 6, SQL_C_ULONG, &size,sizeof(size),&indicator[6]);
  ret = SQLExecute(uo.fetch_stmt);
  ret = SQLFetch(uo.fetch_stmt);
  
  entry->dlna_profile=malloc(sizeof(dlna_profile_t));
  memset(entry->dlna_profile,0,sizeof(entry->dlna_profile));
  
  entry->id=id;
  if (indicator[1] == SQL_NULL_DATA)
    entry->fullpath = NULL;
  else
    entry->fullpath=strdup(fullpath);
  
  if (indicator[2] == SQL_NULL_DATA) {
    entry->dlna_profile->mime = NULL;
    entry->child_count=get_child_count(odbc_ptr,id);
    entry->mime_type = &Container_MIME_Type;
  } else {
    entry->dlna_profile->mime=strdup(dlna_mime);
    entry->child_count = -1;
    entry->mime_type = NULL;
  }
  
  if (indicator[3] == SQL_NULL_DATA)
    entry->dlna_profile->id = NULL;
  else
    entry->dlna_profile->id=strdup(dlna_id);
  
  if (indicator[4] == SQL_NULL_DATA)
    entry->title = NULL;
  else
    entry->title=strdup(title);
  
  if (indicator[5] == SQL_NULL_DATA) {
    entry->url = NULL;
  } else {
    entry->url=strdup(url);
  }
  
  if (indicator[6] == SQL_NULL_DATA)
    entry->size = 0;
  else
    entry->size=size;
  
  entry->childs = NULL;
  
  return entry;
}

struct upnp_entry_t **fetch_children(int odbc_ptr,struct upnp_entry_t *parent)
{
  SQLRETURN ret;
  SQLINTEGER indicator[8];
  struct upnp_entry_t *entry;
  struct upnp_entry_t **childs;
  char fullpath[512];
  char dlna_mime[512];
  char dlna_id[512];
  char title[512];
  SQLINTEGER i,rows;
  char url[512];
  long size,id;
  
  if (odbc_ptr < 0)
    return NULL;
  
  SQLFreeStmt(uo.child_stmt,SQL_CLOSE);
  ret = SQLBindParameter(uo.child_stmt, 1, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER, sizeof(long), 0, &parent->id, sizeof(parent->id), NULL);
  ret =  SQLBindCol( uo.child_stmt, 1, SQL_C_ULONG, &id,sizeof(id),&indicator[1]);
  ret =  SQLBindCol( uo.child_stmt, 2, SQL_C_CHAR, &fullpath,sizeof(fullpath),&indicator[2]);
  ret =  SQLBindCol( uo.child_stmt, 3, SQL_C_CHAR, &dlna_mime,sizeof(dlna_mime),&indicator[3]);
  ret =  SQLBindCol( uo.child_stmt, 4, SQL_C_CHAR, &dlna_id,sizeof(dlna_id),&indicator[4]);
  ret =  SQLBindCol( uo.child_stmt, 5, SQL_C_CHAR, &title,sizeof(title),&indicator[5]);
  ret =  SQLBindCol( uo.child_stmt, 6, SQL_C_CHAR, &url,sizeof(url),&indicator[6]);
  ret =  SQLBindCol( uo.child_stmt, 7, SQL_C_ULONG, &size,sizeof(size),&indicator[7]);
  ret = SQLExecute(uo.child_stmt);
  
  SQLRowCount(uo.child_stmt,&rows);
  
  childs = (struct upnp_entry_t **) calloc (sizeof (struct upnp_entry_t *),rows);
  
  for (i=1;i<=rows;i++) {
    ret = SQLFetch(uo.child_stmt);
    entry = (struct upnp_entry_t *) malloc (sizeof (struct upnp_entry_t));
    memset(entry,0,sizeof(*entry));
    entry->dlna_profile=malloc(sizeof(dlna_profile_t));
    memset(entry->dlna_profile,0,sizeof(entry->dlna_profile));
    
    entry->id=id;
    if (indicator[2] == SQL_NULL_DATA)
      entry->fullpath = NULL;
    else
      entry->fullpath=strdup(fullpath);
    
    if (indicator[3] == SQL_NULL_DATA)
      entry->dlna_profile->mime = NULL;
    else
      entry->dlna_profile->mime=strdup(dlna_mime);
    
    if (indicator[4] == SQL_NULL_DATA)
      entry->dlna_profile->id = NULL;
    else
      entry->dlna_profile->id=strdup(dlna_id);
    
    if (indicator[5] == SQL_NULL_DATA)
      entry->title = NULL;
    else
      entry->title=strdup(title);
    
    if (indicator[6] == SQL_NULL_DATA) {
      entry->url = NULL;
      entry->child_count=get_child_count(odbc_ptr,id);
      entry->mime_type = &Container_MIME_Type;
    } else {
      entry->url=strdup(url);
      entry->child_count = -1;
      entry->mime_type = NULL;
    }
    
    if (indicator[7] == SQL_NULL_DATA)
      entry->size = 0;
    else
      entry->size=size;
    
    entry->childs = NULL;
    childs[i-1]=entry;
  }
  
  return childs;
}

int store_entry(int odbc_ptr,struct upnp_entry_t *entry,int parent_id)
{
  SQLRETURN ret;
  SQLINTEGER null = SQL_NULL_DATA;
  SQLHSTMT stmt;
 
  if (odbc_ptr < 0)
    return 0;

  if (entry->id == 0) {
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&stmt);
    ret=SQLPrepare(stmt,(SQLCHAR *)"INSERT INTO ms.mediacontent (id,fullpath,parent_id,dlna_mime,dlna_id,title,url,size) VALUES (0,?,?,?,?,?,?,?)",SQL_NTS);
  } else {
    stmt = uo.store_stmt;
  }
  
  SQLFreeStmt(stmt,SQL_CLOSE);
  SQLBindParameter(stmt, 1, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, entry->fullpath, strlen(entry->fullpath), NULL);
  SQLBindParameter(stmt, 2, SQL_PARAM_INPUT, SQL_C_LONG, SQL_INTEGER,sizeof(parent_id), 0, &parent_id, sizeof(parent_id), NULL);
  if (entry->dlna_profile != NULL) {
    SQLBindParameter(stmt, 3, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->mime, strlen(entry->dlna_profile->mime), NULL);
    SQLBindParameter(stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->id, strlen(entry->dlna_profile->id), NULL);
  } else {
    SQLBindParameter(stmt, 3, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
    SQLBindParameter(stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
  }
  SQLBindParameter(stmt, 5, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->title, strlen(entry->title), NULL);
  if (entry->url != NULL) {
    SQLBindParameter(stmt, 6, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->url, strlen(entry->url), NULL);
  } else {
    SQLBindParameter(stmt, 6, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
  }
  if (entry->size >= 0) {
    SQLBindParameter(stmt, 7, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, &entry->size, sizeof(entry->size), NULL);
  } else {
    SQLBindParameter(stmt, 7, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, NULL, 0, &null);
  }
  if (!SQL_SUCCEEDED(ret = SQLExecute(stmt))) {
    printf("Driver reported the following diagnostics\n");
    extract_error("store_entry","SQLExecute",uo.dbc,SQL_HANDLE_DBC);
  }
  entry->id = entry_stored(odbc_ptr,entry->fullpath);
  return entry->id;
}
