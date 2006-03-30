#ifndef _PG_GIRO_H
#define _PH_GIRO_H 1.0

#include <libpq-fe.h>

void SQLcommand (PGconn *,const char *);
PGresult *myPQexec(PGconn *conn,const char *cmd);
void exit_nicely(PGconn *);

#endif
