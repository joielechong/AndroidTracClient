#include <stdio.h>
#include <sql.h>
#include <sqlext.h>

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
			   


int main (int argc, char **argv) {
  SQLHENV env;
  SQLHDBC dbc;
  SQLHSTMT stmt;
  SQLRETURN ret;
  SQLCHAR outstr[1024];
  SQLSMALLINT outstrlen;

  SQLAllocHandle(SQL_HANDLE_ENV,SQL_NULL_HANDLE, &env);
  SQLSetEnvAttr(env,SQL_ATTR_ODBC_VERSION, (void *)SQL_OV_ODBC3,0);

  SQLAllocHandle(SQL_HANDLE_DBC,env,&dbc);
  ret=SQLDriverConnect(dbc,NULL,"DSN=contacts;", SQL_NTS, outstr,sizeof(outstr),&outstrlen,SQL_DRIVER_COMPLETE);
  if (SQL_SUCCEEDED(ret)) {
    printf("Connected\n");
    printf("Returned connection string was:\n\t%s\n", outstr);
    if (ret == SQL_SUCCESS_WITH_INFO) {
      printf("Driver reported the following diagnostics\n");
      extract_error("SQLDriverConnect",dbc,SQL_HANDLE_DBC);
    }
    SQLDisconnect(dbc);
  } else {
    fprintf(stderr, "Failed to connect\n");
    extract_error("SQLDriverConnect",dbc,SQL_HANDLE_DBC);
  }
  SQLFreeHandle(SQL_HANDLE_DBC,dbc);
  SQLFreeHandle(SQL_HANDLE_ENV,env);
  return 0;
}
