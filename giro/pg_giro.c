#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <mfl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <libpq-fe.h>
#include "pg_giro.h"

// #define DEBUG

void exit_nicely(PGconn* conn)
{
  PQfinish(conn);
  exit(2);
}

char *get_first_value(PGconn *conn,const char *cmd)
{
  static char buffer[2048];
  PGresult *res;

#ifdef DEBUG
  printf("GFV: Voor PQexec\n");
#endif
  res=PQexec(conn,cmd);
#ifdef DEBUG
  printf("GFV: Na PQexec\n");
#endif
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n",PQerrorMessage(conn));
    exit_nicely(conn);
  }
  else {
    if (PQresultStatus(res) != PGRES_TUPLES_OK) {
      fprintf(stderr,"%s: didn't return tuples properly\n",cmd);
      PQclear(res);
      exit_nicely(conn);
    }
  }

  if (PQntuples(res) == 0) {
    PQclear(res);
    return NULL;
  }

  strcpy(buffer,PQgetvalue(res,0,0));
  
  PQclear(res);
#ifdef DEBUG
  printf("GFV: Voor return\n");
#endif
  return buffer;
}

PGresult *myPQexec(PGconn *conn,const char *cmd)
{
  printf("%s\n",cmd);
  return PQexec(conn,cmd);
}

void SQLcommand(PGconn *conn,const char *cmd)
{
  PGresult *res;

  res=myPQexec(conn,cmd);
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n",PQerrorMessage(conn));
    fprintf(stderr,"   SQL command: %s\n",cmd);
    exit_nicely(conn);
  }
  else {
    if (PQresultStatus(res) != PGRES_COMMAND_OK) {
      fprintf(stderr," command failed\n");
      PQclear(res);
      exit_nicely(conn);
    }
    PQclear(res);
  }
}

