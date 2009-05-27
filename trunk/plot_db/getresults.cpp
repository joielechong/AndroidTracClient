#include <string.h>
#include <stdlib.h>
#include <libpq-fe.h>
#include "results.h"

int nfields;

char *fieldnames[10];

struct result *results;

static void exit_nicely(PGconn* conn)
{
  PQfinish(conn);
  exit(2);
}

long getResults(const char *dbName, const char *query)
{
  char *pghost, *pgport, *pgoptions, *pgtty;
  long i,n;
  PGresult *res;
  PGconn* conn;

  pghost = NULL;
  pgport = NULL;
  pgoptions = NULL;
  pgtty = NULL;

  /* make a connection to the database */
  conn = PQsetdb(pghost, pgport, pgoptions, pgtty, dbName);
  
  /* check to see that the backend connection was successfully made */
  if (PQstatus(conn) == CONNECTION_BAD) {
    fprintf(stderr,"Connection to database '%s' failed.\n", dbName);
    fprintf(stderr,"%s",PQerrorMessage(conn));
    exit_nicely(conn);
  }

  res=PQexec(conn,query);
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n",PQerrorMessage(conn));
    exit_nicely(conn);
  }
  else {
    if (PQresultStatus(res) != PGRES_TUPLES_OK) {
      fprintf(stderr,"SELECT command didn't return tuples properly\n");
      PQclear(res);
      exit_nicely(conn);
    }
  }

  if (PQnfields(res) <= nfields) {
    fprintf(stderr,"Not enough fields in query\n");
    fprintf(stderr,"  Request : %2d\n",nfields);
    fprintf(stderr,"  Query   : %2d + 1\n",PQnfields(res)-1);
    PQclear(res);
    exit_nicely(conn);
  }

  n = PQntuples(res);
  if (n < 1) {
    fprintf(stderr,"PQntuples < 1 is %ld\n",n);
    PQclear(res);
    exit_nicely(conn);
  }

  for(i=0;i<=nfields;i++) {
    char *s=PQfname(res,i);

    if (s)
      fieldnames[i] = strdup(PQfname(res,i));
    else
      fieldnames[i]="Unkown";
  }

  results = (struct result *)calloc(n,sizeof(struct result));
  if (results == NULL) {
    PQclear(res);
    perror("results");
    exit_nicely(conn);
  }

  for(i=0;i<n;i++) {
    int j;

    results[i].veld = strdup(PQgetvalue(res,i,0));
    for (j=1;j<=nfields;j++)
      results[i].waarde[j-1] = atof(PQgetvalue(res,i,j));
  }

  PQclear(res);
  
  /* close the connection to the database and cleanup */
  PQfinish(conn);
  
  /*   fclose(debug); */
  return n;
}
