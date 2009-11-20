#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "metadata.h"
#include <sql.h>
#include <sqlext.h>
#include "odbc.h"

static void extract_error (char *fn, SQLHANDLE handle, SQLSMALLINT type) {
  SQLINTEGER i=0;
  SQLINTEGER native;
  SQLCHAR state[7];
  SQLCHAR text[256];
  SQLSMALLINT len;
  SQLRETURN ret;
  
  fprintf(stderr,"\nThe driver reported the following diagnostics whilst running %s\n\n",fn);
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
} ushare_odbc;

static ushare_odbc uo;	

int init_odbc(const char *dsn) {
  SQLRETURN ret;
  
  SQLAllocHandle(SQL_HANDLE_ENV,SQL_NULL_HANDLE, &uo.env);
  SQLSetEnvAttr(uo.env,SQL_ATTR_ODBC_VERSION, (void *)SQL_OV_ODBC3,0);
  
  SQLAllocHandle(SQL_HANDLE_DBC,uo.env,&uo.dbc);
  ret=SQLDriverConnect(uo.dbc,NULL,(SQLCHAR *)dsn, SQL_NTS, uo.outstr,sizeof(uo.outstr),&uo.outstrlen,SQL_DRIVER_COMPLETE);
  if (SQL_SUCCEEDED(ret)) {
    printf("Connected\n");
    printf("Returned connection string was:\n\t%s\n", uo.outstr);
    if (ret == SQL_SUCCESS_WITH_INFO) {
      printf("Driver reported the following diagnostics\n");
      extract_error("SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
    }
	SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.es_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.es_stmt,(SQLCHAR *)"SELECT id FROM ms.mediacontent WHERE fullpath=?",SQL_NTS))) {
	  uo.es_stmt = NULL;
	}
	SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&uo.store_stmt);
    if (!SQL_SUCCEEDED(ret=SQLPrepare(uo.store_stmt,(SQLCHAR *)"INSERT INTO ms.mediacontent (id,fullpath,container,dlna_mime,dlna_id,title,url,size) VALUES (?,?,?,?,?,?,?,?)",SQL_NTS))) {
	  uo.store_stmt = NULL;
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
    return 0;

  SQLFreeStmt(uo.es_stmt,SQL_CLOSE);
  lastcall = "SQLBindParameter";
  if (SQL_SUCCEEDED(ret = SQLBindParameter(uo.es_stmt, 1, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, path, strlen(path), NULL))) {
    lastcall = "SQLBindCol";
    if (SQL_SUCCEEDED(ret = SQLBindCol( uo.es_stmt, 1, SQL_C_LONG, &retval,sizeof(retval),&indicator))) {
      lastcall = "SQLExecute";
      if (SQL_SUCCEEDED(ret = SQLExecute(uo.es_stmt))) {
        SQLRowCount(uo.es_stmt,&rows);
	    if (rows != 1) return 0;
        lastcall = "SQLFetch";
        if (SQL_SUCCEEDED(ret=SQLFetch(uo.es_stmt))) {
          if (indicator == SQL_NULL_DATA) return -1;
	      return retval;
	    }
	  }
	}
  }
 
  printf("Driver reported the following diagnostics\n");
  extract_error(lastcall,uo.dbc,SQL_HANDLE_DBC);
  return -1;
}

int store_entry(int odbc_ptr,struct upnp_entry_t *entry)
{
  char *container,*c;
  SQLRETURN ret;
  SQLINTEGER null = SQL_NULL_DATA;

	
  if (odbc_ptr < 0)
    return 0;

  container = strdup(entry->fullpath);
  if (container[strlen(container)-1] == '/')
    container[strlen(container)-1] = 0;
  c = strrchr(container,'/');
  if (c == NULL) {
    free(container);
    container = "Ongedefinieerd";
  } else {
    *c = 0;
  }

  SQLFreeStmt(uo.store_stmt,SQL_CLOSE);
  SQLBindParameter(uo.store_stmt,1,SQL_PARAM_INPUT,SQL_C_LONG, SQL_INTEGER,sizeof(long),0,&(entry->id),sizeof(entry->id),NULL);
  SQLBindParameter(uo.store_stmt, 2, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, entry->fullpath, strlen(entry->fullpath), NULL);
  SQLBindParameter(uo.store_stmt, 3, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, container, strlen(container), NULL);
  if (entry->dlna_profile != NULL) {
    SQLBindParameter(uo.store_stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->mime, strlen(entry->dlna_profile->mime), NULL);
    SQLBindParameter(uo.store_stmt, 5, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->dlna_profile->id, strlen(entry->dlna_profile->id), NULL);
  } else {
    SQLBindParameter(uo.store_stmt, 4, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
    SQLBindParameter(uo.store_stmt, 5, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
  }
  SQLBindParameter(uo.store_stmt, 6, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->title, strlen(entry->title), NULL);
  if (entry->url != NULL) {
    SQLBindParameter(uo.store_stmt, 7, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, (char *)entry->url, strlen(entry->url), NULL);
  } else {
    SQLBindParameter(uo.store_stmt, 7, SQL_PARAM_INPUT, SQL_C_CHAR, SQL_VARCHAR, 255, 0, NULL, 0, &null);
  }
  if (entry->size >= 0) {
    SQLBindParameter(uo.store_stmt, 8, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, &entry->size, sizeof(entry->size), NULL);
  } else {
    SQLBindParameter(uo.store_stmt, 8, SQL_PARAM_INPUT, SQL_C_UBIGINT, SQL_BIGINT, sizeof(long long), 0, NULL, 0, &null);
  }
  if (!SQL_SUCCEEDED(ret = SQLExecute(uo.store_stmt))) {
    printf("Driver reported the following diagnostics\n");
    extract_error("SQLExecute",uo.dbc,SQL_HANDLE_DBC);
  }
  return 0;
}

#ifdef ODBC_DEBUG


int main (int argc, char **argv) {
  SQLHSTMT stmt;
  SQLRETURN ret;
  int row=0;
  SQLSMALLINT columns;
  SQLINTEGER rows;
  int odbc_ptr;

  
  odbc_ptr=init_odbc("DSN=mediaserver;");
  if (odbc_ptr>=0) {
    SQLAllocHandle(SQL_HANDLE_STMT,uo.dbc,&stmt);
    if (SQL_SUCCEEDED(ret = SQLPrepare(stmt,"SELECT id,fullpath,container,dlna_flags,dlna_mime,dlna_id,title,url,size FROM ms.mediacontent",SQL_NTS))) {
      if (SQL_SUCCEEDED(ret = SQLExecute(stmt))) {
	SQLNumResultCols(stmt,&columns);
	SQLRowCount(stmt,&rows);
	printf("Columns = %d, rows = %d\n",columns,rows);


	while (SQL_SUCCEEDED(ret=SQLFetch(stmt))) {
	  SQLUSMALLINT i;
	  printf("Row %d\n",row++);
	  for (i=1;i<=columns;i++) {
	    SQLINTEGER indicator;
	    char buf[512];
	    
	    ret=SQLGetData(stmt,i,SQL_C_CHAR,buf,sizeof(buf),&indicator);
	    if (SQL_SUCCEEDED(ret)) {
	      if (indicator == SQL_NULL_DATA) strcpy(buf,"*NULL*");
	      printf(" Column %u: %s\n",i,buf);
	    }
	  }
	}
      } else {
	fprintf(stderr, "Failed to execute\n");
	extract_error("SQLExecute",uo.dbc,SQL_HANDLE_DBC);
      }
    }  else {
      fprintf(stderr, "Failed to prepare\n");
      extract_error("SQLPrepare",uo.dbc,SQL_HANDLE_DBC);
    }
  } else {
    fprintf(stderr, "Failed to connect\n");
    extract_error("SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
  }
  odbc_finish(odbc_ptr);
  return 0;
}

#endif
