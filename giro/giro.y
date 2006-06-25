%token HEADER
%token DATUM
%token FLOAT
%token STRING
%token INTEGER
%token EMPTY

%union {
  char *s;
  double f;
  int i;
}

%{

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <libpq-fe.h>
#include "pg_giro.h"

static PGconn* conn;

int yylex(void);
 extern FILE *yyin;
 
 void yyerror (char *s) {
   fprintf (stderr, "%s\n", s);
 }
 
 void process(int format,long girorek,long datum,char *type,int nr, long reknr, char *naam, double bedrag, char *ab, char *omschr) {
   char *o,*p;
   double b;
   char cmd[1024];
   //   fprintf(stderr,"%d %ld %ld %s %d %ld %s %9.2lf %s %s\n",format,girorek,datum,type,nr,reknr,naam,bedrag,ab,omschr);
   
   if (*ab != 0) {
      
     if (format==2) {
       int iii=strncmp(naam,omschr,strlen(naam));
       if (iii==0) 
	 o=omschr+strlen(naam);
       else
	 o=omschr;
       //       fprintf(stderr,"iii = %d, o=%s\n",iii,o);
     } else 
       o=omschr;

     while (*o==' ')
	o++;
     
     while ((p=strchr(naam,0xAC)) != NULL) {
       *p='-';
     }
     while ((p=strchr(naam,'\'')) != NULL) {
       *p='-';
      }
     while ((p=strchr(o,0xAC)) != NULL) {
       *p='-';
     }
     while ((p=strchr(o,'\'')) != NULL) {
       *p='-';
      }
     
     if (ab[0]=='A') 
       b=-bedrag;
     else if (ab[0]=='B') 
       b = bedrag;
      else
	b=-99999.99;
     
     sprintf(cmd,"insert into girotmp1 values (%ld,%ld,'%s',%d,%ld,'%s',%.2lf,'%s');",girorek,datum,type,nr,reknr,naam,b,o); 
     SQLcommand(conn,cmd);
   }    
 }
 
 %}

%%

giro               :  HEADER format2
                   |  format1
;

format1            :  line1 
                   |  format1 line1
                   ;

format2            : line2
                   | format2 line2
                   ;

line1              : girorek ',' datum ',' type ',' nr1 ',' rek ',' naam ',' dummy ',' bedrag ',' ab1 ',' dummy ',' omschr ',' dummy {process(1,$<i>1,$<i>3,$<s>5,$<i>7,$<i>9,$<s>11,$<f>15,$<s>17,$<s>21);} ;

line2              : datum ',' naam ',' girorek ',' rek ',' type ',' ab2 ',' bedrag ',' dummy ',' omschr {process(2,$<i>5,$<i>1,$<s>9,0,$<i>7,$<s>3,$<f>13,$<s>11,$<s>17);} ;

girorek            : INTEGER;
rek                : INTEGER|EMPTY {$<i>$=0;};
datum              : INTEGER;
type               : STRING|EMPTY {$<s>$="";};
dummy              : STRING|EMPTY;
nr1                : INTEGER;
naam               : STRING|INTEGER|EMPTY {$<s>$=""};
bedrag             : FLOAT;
ab1                : STRING;
omschr             : STRING|EMPTY {$<s>$=""};
ab2                : STRING;

%%

#define MAXREK 16

int main(int argc,char **argv)
{
  int k,l;
  char filename[512];
  char *pghost=NULL, *pgport=NULL, *pgoptions=NULL, *pgtty=NULL;
  char *dbname="httpd";
  char cmd[512];
  PGresult *res;
  int girorek[MAXREK];
  int maxdat[MAXREK];
  int mindat[MAXREK];
  int nrek;

  if (argc <= 1) {
    fprintf(stderr,"Usage: %s download_file[s]\n",argv[0]);
    return(4);
  }

  conn = PQsetdb(pghost,pgport,pgoptions,pgtty,dbname);
  if (PQstatus(conn) == CONNECTION_BAD) {
    fprintf(stderr,"Connection to database '%s' failed\n",dbname);
    fprintf(stderr,"%s",PQerrorMessage(conn));
    exit_nicely(conn);
  }

  strcpy(cmd,"delete from girotmp1;");
  SQLcommand(conn,cmd);

  yydebug=0;

  for (k=1;k<argc;k++) {
    if (strcmp(argv[k],"-d")==0) {
      yydebug=1;
    } else {
      strcpy(filename,argv[k]);
      yyin=fopen(filename,"r");
      if (yyin != NULL) {
        if (yyparse()) {
          printf("Klaar met fouten in %s\n",filename);
	  exit_nicely(conn);
        }
        fclose(yyin);
      } else {
        fprintf(stderr,"Kan %s niet openen\n",filename);
      }
    }
  }

  strcpy(cmd,"select girorekening,max(datum) from giro where girorekening in (select girorekening from girotmp1) group by girorekening;");
  res = myPQexec(conn,cmd);
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n  Cmd=%s\n",PQerrorMessage(conn),cmd);
    exit_nicely(conn);
  }
  if (PQresultStatus(res) != PGRES_TUPLES_OK) {
    fprintf(stderr,"Geen waarden uit database gekregen\n");
    exit_nicely(conn);
  }

  nrek=PQntuples(res);
  if (nrek > MAXREK) {
    fprintf(stderr,"Teveel rekeningen in download files\n");
    exit_nicely(conn);
  }

  for (k=0;k<nrek;k++) {
    girorek[k]=atol(PQgetvalue(res,k,0));
    maxdat[k]=atol(PQgetvalue(res,k,1));
  }
  PQclear(res);


  strcpy(cmd,"select girorekening,min(datum) from girotmp1 group by girorekening;");
  res = myPQexec(conn,cmd);
  if (res == NULL) {
    fprintf(stderr,"PQexec failed: %s\n  Cmd=%s\n",PQerrorMessage(conn),cmd);
    exit_nicely(conn);
  }
  if (PQresultStatus(res) != PGRES_TUPLES_OK) {
    fprintf(stderr,"Geen waarden uit database gekregen\n");
    exit_nicely(conn);
  }

  nrek=PQntuples(res);
  if (nrek > MAXREK) {
    fprintf(stderr,"Teveel rekeningen in download files\n");
    exit_nicely(conn);
  }

  for (k=0;k<nrek;k++) {
    for (l=0;l<PQntuples(res);l++) {
      if (girorek[k]==atol(PQgetvalue(res,l,0))) {
        mindat[k]=atol(PQgetvalue(res,l,1));
      }
    }
  }
  PQclear(res);

  SQLcommand(conn,"BEGIN");
  for(k=0;k<nrek;k++) {
    if (mindat[k]<=maxdat[k]) {
      sprintf(cmd,"delete from giro where girorekening=%d and datum >=%d;",girorek[k],maxdat[k]);
      SQLcommand(conn,cmd);
      sprintf(cmd,"delete from girotmp1 where girorekening=%d and datum <%d;",girorek[k],maxdat[k]);
      SQLcommand(conn,cmd);
    }
  }
  sprintf(cmd,"insert into giro select girorekening,datum,type,volgnr,rekening,naam,bedrag,omschrijving from girotmp1;");
  SQLcommand(conn,cmd);
  
  SQLcommand(conn,"END");

  PQfinish(conn);
  return 0;
}
