#include <stdio.h>
#include <string.h>
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
} ushare_odbc;

static ushare_odbc uo;	

int init_odbc(char *dsn) {
  SQLRETURN ret;
  
  SQLAllocHandle(SQL_HANDLE_ENV,SQL_NULL_HANDLE, &uo.env);
  SQLSetEnvAttr(uo.env,SQL_ATTR_ODBC_VERSION, (void *)SQL_OV_ODBC3,0);
  
  SQLAllocHandle(SQL_HANDLE_DBC,uo.env,&uo.dbc);
  ret=SQLDriverConnect(uo.dbc,NULL,dsn, SQL_NTS, uo.outstr,sizeof(uo.outstr),&uo.outstrlen,SQL_DRIVER_COMPLETE);
  if (SQL_SUCCEEDED(ret)) {
    printf("Connected\n");
    printf("Returned connection string was:\n\t%s\n", uo.outstr);
    if (ret == SQL_SUCCESS_WITH_INFO) {
      printf("Driver reported the following diagnostics\n");
      extract_error("SQLDriverConnect",uo.dbc,SQL_HANDLE_DBC);
    }
    return 1;
  } else {
    return -1;
  }
}

void odbc_finish(int odbc_ptr) {
  if (odbc_ptr >=0) SQLDisconnect(uo.dbc);
  SQLFreeHandle(SQL_HANDLE_DBC,uo.dbc);
  SQLFreeHandle(SQL_HANDLE_ENV,uo.env);
}

int entry_stored(int odbc_ptr,char *path)
{
  return 0;
}

int store_entry(int odbc_entry,strict upnp_entry_t *entry)
{
  return 1;
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
    if (SQL_SUCCEEDED(ret = SQLPrepare(stmt,"SELECT id,fullpath,container,dlna_type,dlna_speed,dlna_conversion,dlna_operation,dlna_flags,dlna_mime,dlna_id,title,url,size,mime_type FROM ms.mediacontent",SQL_NTS))) {
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
