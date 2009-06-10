#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <mfl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include "libpq-fe.h"

// #define DEBUG

long counter=0;
char *maanden[] ={"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"};

#define MAX_FONDS 4096

struct _fondsen {
  char *naam;
  long id;
} fondsen[MAX_FONDS];

int nrfonds=0;

void exit_nicely(PGconn* conn)
{
  PQfinish(conn);
  exit(2);
}

void start_transaction(PGconn *conn)
{
  PGresult *res;

  res = PQexec(conn,"BEGIN");
  if (PQresultStatus(res) != PGRES_COMMAND_OK) {
    fprintf(stderr,"BEGIN command failed\n");
    PQclear(res);
    exit_nicely(conn);
  }
  /* should PQclear PGresult whenever it is no longer needed to avoid
     memory leaks */
  PQclear(res);
  counter=0;
}

void end_transaction(PGconn *conn)
{
  PGresult *res;

  res = PQexec(conn,"END");
  if (PQresultStatus(res) != PGRES_COMMAND_OK) {
    fprintf(stderr,"END command failed\n");
    PQclear(res);
    exit_nicely(conn);
  }
  /* should PQclear PGresult whenever it is no longer needed to avoid
     memory leaks */
  PQclear(res);
}

PGresult *myPQexec(PGconn *conn,const char *s)
{
  counter++;
  if ((counter % 100) == 0) {
    printf("saving to disk\n");
    end_transaction(conn);
    start_transaction(conn);
  }
  printf("%s\n",s);
  return PQexec(conn,s);
}

char *get_first_value(PGconn *conn,const char *cmd)
{
  static char buffer[2048];
  PGresult *res;

#ifdef DEBUG
  printf("GFV: Voor PQexec\n");
#endif
  res=myPQexec(conn,cmd);
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

void SQLcommand(PGconn *conn,const char *cmd)
{
  PGresult *res;

  res=myPQexec(conn,cmd);
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n",PQerrorMessage(conn));
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

long zoek_fonds(PGconn *conn,char *fonds)
{
  char *nwfonds,*idstr,*t;
  static char lastfonds[80];
  static long lastid;
  long id=-1;
  int i;

  char cmd[128];
  int n;

  for(n=0;fonds[n]!=0;n++)
    if (fonds[n]=='\'')
      fonds[n] = '-';
  strip_cr(fonds);

  if (strcmp(lastfonds,fonds) == 0) 
    return lastid;

  for(i=0;i<nrfonds;i++)
    if (strcmp(fondsen[i].naam,fonds) == 0)
      return fondsen[i].id;

  sprintf(cmd,"select naam2 from vertaal where naam1='%s';",fonds);
  if ((t=get_first_value(conn,cmd)) == NULL)
    nwfonds=strdup(fonds);
  else
    nwfonds=strdup(t);

  if (nwfonds[0] == '-') 
    id=-1;
  else {
    sprintf(cmd,"select id from rekening where naam='%s';",nwfonds);
    if ((idstr=get_first_value(conn,cmd)) == NULL) {
      end_transaction(conn);
      start_transaction(conn);
      sprintf(cmd,"insert into rekening (naam,soort) values('%s','A');",nwfonds);
      SQLcommand(conn,cmd);
      end_transaction(conn);
      start_transaction(conn);
      sprintf(cmd,"select id from rekening where naam='%s';",nwfonds);
      idstr=get_first_value(conn,cmd);
    }
    id = atol(idstr);
  }

  strcpy(lastfonds,fonds);
  lastid=id;

  if (nrfonds >= MAX_FONDS) {
    fprintf (stderr,"Teveel fondsen\n");
    exit( 1);
  }

  fondsen[nrfonds].naam=strdup(fonds);
  fondsen[nrfonds].id=id;
  nrfonds++;
  if (strcmp(nwfonds,fonds) != 0) {
    fondsen[nrfonds].naam=strdup(nwfonds);
    fondsen[nrfonds].id=id;
    nrfonds++;
  }
  
  return id;
}

void update_koers(PGconn *conn,const long id,char *vol, char *ope, char *hoog, char *laag, char *slot, char *datum)
{
  PGresult *res;
  char cmd[1024];
  int n;
  double new_open,old_open = 0;
  double new_hoog,old_hoog = 0;
  double new_laag,old_laag = 0;
  double new_slot,old_slot = 0;
  long new_vol,  old_vol = 0;

  sscanf(vol,"%ld",&new_vol);
  sscanf(ope,"%lf",&new_open);
  sscanf(hoog,"%lf",&new_hoog);
  sscanf(laag,"%lf",&new_laag);
  sscanf(slot,"%lf",&new_slot);

  if (new_open==0 && new_hoog==0 && new_laag==0 && new_slot == 0)
    return;

  if (new_laag == 0 && new_slot != 0) new_laag = new_slot;
  if (new_laag == 0 && new_open != 0) new_laag = new_open;
  if (new_laag == 0 && new_hoog != 0) new_laag = new_hoog;

  if (new_hoog == 0 && new_slot != 0) new_hoog = new_slot;
  if (new_hoog == 0 && new_open != 0) new_hoog = new_open;
  if (new_hoog == 0 && new_laag != 0) new_hoog = new_laag;

  if (new_slot == 0) new_slot=new_laag;

  if (new_open == 0) new_open=new_hoog;

  if (new_open < new_laag) new_laag = new_open;
  if (new_slot < new_laag) new_laag = new_slot;
  if (new_hoog < new_open) new_hoog = new_open;
  if (new_hoog < new_slot) new_hoog = new_slot;

  sprintf(cmd,"select volume,open,hoog,laag,slot from koersid where id=%ld and datum=\'%s\';",id,datum);

  res=myPQexec(conn,cmd);
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

  /* first, print out the attribute names */
  if (PQnfields(res) != 5) {
    fprintf(stderr,"PQnfields <> 5 is %d\n",PQnfields(res));
    PQclear(res);
    exit_nicely(conn);
  }

  n = PQntuples(res);
  if (n > 1) {
    fprintf(stderr,"PQntuples > 1 is %d\n",n);
    PQclear(res);
    exit_nicely(conn);
  }

  if (n == 1) {
    old_vol = atol(PQgetvalue(res,0,0));
    old_open = atof(PQgetvalue(res,0,1));
    old_hoog = atof(PQgetvalue(res,0,2));
    old_laag = atof(PQgetvalue(res,0,3));
    old_slot = atof(PQgetvalue(res,0,4));
  }
  else {
    old_vol = new_vol;
    old_open = new_open;
    old_hoog = new_hoog;
    old_laag = new_laag;
    old_slot = new_slot;
  }

  PQclear(res);
  
  if (n == 0)
    sprintf(cmd,"insert into koersid (id,volume,open,hoog,laag,slot,datum) values (%ld, %ld, %.2f, %.2f, %.2f, %.2f, '%s');",id,new_vol,new_open,new_hoog,new_laag,new_slot,datum);
  else {
    if (old_open == 0 || new_vol > old_vol) {
      int nocmd = 1;
      
      strcpy(cmd,"update koersid ");
      if (new_vol > old_vol) {
	char temp[80];
	
	sprintf(temp,"%s volume=%ld",(nocmd?"set":","),new_vol);
	strcat(cmd,temp);
	nocmd = 0;
      }
      
      if ((old_open != new_open) && (new_open != 0) &&(new_open != new_hoog &&new_open != new_laag)) {
	char temp[80];
	
	sprintf(temp,"%s open=%.2f",(nocmd?"set":","),new_open);
	strcat(cmd,temp);
	nocmd = 0;
      }
      
      if ((old_hoog < new_hoog) && (new_hoog != 0)) {
	char temp[80];
	
	sprintf(temp,"%s hoog=%.2f",(nocmd?"set":","),new_hoog);
	strcat(cmd,temp);
	nocmd = 0;
      }
      
      if ((old_laag > new_laag) && (new_laag != 0)) {
	char temp[80];
	
	sprintf(temp,"%s laag=%.2f",(nocmd?"set":","),new_laag);
	strcat(cmd,temp);
	nocmd = 0;
    }
      
      if ((old_slot != new_slot) && (new_slot != 0)) {
	char temp[80];
	
	sprintf(temp,"%s slot=%.2f",(nocmd?"set":","),new_slot);
	strcat(cmd,temp);
	nocmd = 0;
      }
    
      if (nocmd)
	cmd[0] = 0;
      else {
	char temp[80];
	
	sprintf(temp," where id=%ld and datum='%s';",id,datum);
	strcat(cmd,temp);
      }
    }
    else
      cmd[0] = 0;
  }

  if (cmd[0] != 0) 
    SQLcommand(conn,cmd);

}

int process_bsn(PGconn *conn,char *filarg)
{
  char filnam[512];
  char buffer[512];
  char datum[10];
  FILE *fbsn;
  long file_size;
  struct stat stbuf;
  int perc;
  
  filnam[0] = 0;
  
  if (strchr(filarg,'/') == NULL) {
    if (getenv("BSN_SOURCE") != NULL) {
      strcat(filnam,getenv("BSN_SOURCE"));
      strcat(filnam,"/");
    }
  }

  strcat(filnam,filarg);
  if ((strstr(filarg,".bsn") == NULL) && (strstr(filarg,".vsn") == NULL))
    strcat(filnam,".bsn");

  if ((fbsn = fopen(filnam,"rt")) == NULL) {
    perror(filnam);
    return 1;
  }

  if (fstat(fileno(fbsn),&stbuf) != 0) {
    perror(filnam);
    fclose(fbsn);
    return 1;
  }
  file_size = stbuf.st_size;
  if (file_size == 0) {
    fclose(fbsn);
    return 0;
  }

  datum[0] = 0;
  perc = (100L *ftell(fbsn))/file_size;

  start_transaction(conn);
  while (fgets(buffer,sizeof(buffer),fbsn) != NULL) {
    int new_perc = (100L *ftell(fbsn))/file_size;

    if (new_perc > perc)
      printf("%ld%% done on %s\n",(100L *ftell(fbsn))/file_size,filnam);
    perc=new_perc;

    strip_cr(buffer);
    if (buffer[0] == '0') {
      strncpy(datum,buffer+4,9);
      datum[9] = 0;
    }
    else if (buffer[0] == '9') {
      char *fonds,*vol,*ope,*hoog,*laag,*slot;
      long id;
      
      fonds = buffer+5;
      if (buffer[2] == '3') {
	vol = strchr(fonds,'|');
	*vol++ = 0;
	
	ope = strchr(vol,'|');
	*ope++ = 0;
	
	hoog = strchr(ope,'|');
	*hoog++ = 0;
	
	laag = strchr(hoog,'|');
	*laag++ = 0;
	
	slot = strchr(laag, '|');
	*slot++ = 0;
      } else if (buffer[2] == '4') {
	ope = strchr(fonds,'|');
	*ope++ = 0;
	
	hoog = strchr(ope,'|');
	*hoog++ = 0;
	
	laag = strchr(hoog,'|');
	*laag++ = 0;
	
	slot = strchr(laag, '|');
	*slot++ = 0;

	vol = strchr(slot,'|');
	*vol++ = 0;
      } else {
	fprintf(stderr,"Kan deze regel in file %s niet verwerken\n%s\n",filarg,buffer);
	return 1;
      }
      id = zoek_fonds(conn,fonds);
      if (id > 0)
	update_koers(conn,id,vol,ope,hoog,laag,slot,datum);
    }
  }
  end_transaction(conn);
  
  fclose(fbsn);
  return 0;
}

int main(int argc, char **argv)
{
  char *pghost, *pgport, *pgoptions, *pgtty;
  char* dbName;
  int i;
  
  PGconn* conn;

  if (argc < 2) {
    fprintf(stderr,"Aanroep: %s <bsn-file>,....\n\n",argv[0]);
    return 1;
  }
  
  /* begin, by setting the parameters for a backend connection
     if the parameters are null, then the system will try to use
     reasonable defaults by looking up environment variables
     or, failing that, using hardwired constants */
  pghost = NULL;	 /* host name of the backend server */
  pgport = NULL;	 /* port of the backend server */
  pgoptions = NULL; /* special options to start up the backend server */
  pgtty = NULL;	   /* debugging tty for the backend server */
  dbName = "koersdata";
  
  /* make a connection to the database */
  conn = PQsetdb(pghost, pgport, pgoptions, pgtty, dbName);
  
  /* check to see that the backend connection was successfully made */
  if (PQstatus(conn) == CONNECTION_BAD) {
    fprintf(stderr,"Connection to database '%s' failed.\n", dbName);
    fprintf(stderr,"%s",PQerrorMessage(conn));
    exit_nicely(conn);
  }
  
  for (i=1;i<argc;i++) {
    printf("BSN file = %s\n",argv[i]);
    if (process_bsn(conn,argv[i]) != 0)
      break;
  }

  /* close the connection to the database and cleanup */
  PQfinish(conn);
  
  /*   fclose(debug); */

  return 0;
}
