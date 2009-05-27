#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>
#include "koersen.h"

#define EURO2FL (2.20371)

int jaar,maand,dag = 0;
FILE *rend;

extern int yydebug;
void yyrestart(FILE *);
int yyparse(void);

int loadtabel(const char *naam) {
  FILE *fin;

  fin = fopen(naam,"rt");
  if (fin == NULL) {
    perror(naam);
    return(1);
  }
  yyrestart(fin);
  yydebug=0;
  if (yyparse() != 0) {
    fclose(fin);
    return(1);
  }
  fclose(fin);
  tabel[tabcnt].tt_nam="          ";
  tabel[tabcnt].db_nam="";
  tabel[tabcnt].koers=0;
  tabel[tabcnt].type=laatste;
  tabel[tabcnt].conv_koers=0;
  tabcnt++;

#ifdef DEBUG
  int i;
  for(i=0;i<tabcnt;i++) {
    struct tabel *t = tabel+i;

    printf("%s:%s:%7.2f:%1d:%7.2f\n",t->tt_nam,t->db_nam,t->koers,t->type,t->conv_koers);
  }
#endif

  return(0);
}

void skip_page(FILE *fid)
{
  char buffer[1024];

  for(;;)
  {
    if ((fgets(buffer,sizeof(buffer),fid)) == NULL)
      return;
    if (strncmp(buffer,"NOS-TT",6) == 0)
      return;
  }
}

void pagina(char *filename)
{
  char buffer[1024];
  int i;
  FILE *fid;
  float koers;

  if (strcmp(filename,"-") == 0)
    fid = stdin;
  else
    if ((fid = fopen(filename,"rt")) == NULL)
    {
      perror(filename);
      return;
    }
  for(;;)
  {
    if ((fgets(buffer,sizeof(buffer),fid)) == NULL)
      break;
    if (strstr(buffer,"TOP-30") != 0)
      skip_page(fid);
    else if (strstr(buffer,"STIJGERS") != 0)
      skip_page(fid);
    else if (strstr(buffer,"STAATSLENINGEN") != 0)
      skip_page(fid);
    else if (strstr(buffer,"CONVERTIBLES") != 0)
      skip_page(fid);
    else if (strstr(buffer,"OBLIGATIES") != 0)
      skip_page(fid);
    else if (strstr(buffer,"INDICES") != 0)
      skip_page(fid);
    else if (buffer[26]=='/' && dag == 0)
      sscanf(buffer+24,"%d/%d",&dag,&maand);
    else
    {
      if (buffer[26] == ',') {
        double oud,nieuw;

	for(i=0;tabel[i].type!=laatste ;i++)
	  {
	    if (strstr(buffer,tabel[i].tt_nam) != NULL)
	      {
		buffer[26] = '.';
		if (sscanf(buffer+21,"%f",&koers) == 1) {
		  switch (tabel[i].type) {
		  case laatste:
		    break;  // to satisfy the compiler
		  case normaal:
		    tabel[i].koers = koers;
		    break;
		  case optie_fl:
		    if (jaar > 1998)  {
		      tabel[i].koers = (tabel[i].conv_koers < (koers*EURO2FL) ? 
					((koers*EURO2FL)-tabel[i].conv_koers)/EURO2FL :
					0);
		    } else {
		      tabel[i].koers = (tabel[i].conv_koers < koers ? 
					koers-tabel[i].conv_koers :
					0);
		    }
		    break;
		  case optie_rel_fl:
		    if (jaar >1998)
		      tabel[i].koers = (tabel[i].conv_koers < (koers*EURO2FL) ? 
					((koers*EURO2FL)-tabel[i].conv_koers)/(tabel[i].conv_koers*EURO2FL) :
					0);
		    else
		      tabel[i].koers = (tabel[i].conv_koers < koers ? 
					(koers-tabel[i].conv_koers)/tabel[i].conv_koers :
					0);
		    break;
		  case optie_eur:
		    tabel[i].koers = (tabel[i].conv_koers < koers ? 
				      koers-tabel[i].conv_koers :
				      0);
		    break;
		  case optie_rel_eur:
		    tabel[i].koers = (tabel[i].conv_koers < koers ? 
				      (koers-tabel[i].conv_koers)/tabel[i].conv_koers :
				      0);
		    break;
		  }
		}
	      }
	  }

        buffer[26] = '.';
        buffer[16] = '.';
        buffer[11] = 0;
        oud   = atof(buffer+12);
        nieuw = atof(buffer+21);
        fprintf(rend,"%s %8.2f %8.2f ",buffer+1,oud,nieuw);
        if (oud > 0)
          fprintf(rend,"%7.2f%%",100.0*(nieuw-oud)/oud);
        else
          fprintf(rend,"*******");
        fprintf(rend,"\n");
      }
    }
  }
  if (strcmp(filename,"-") != 0) fclose(fid);
}

int main(int argc, char *argv[])
{
  time_t tijd;
  struct tm *t;
#ifdef __EMX__
  char format[] = "%4.4d%2.2d%2.2d,%6.4lf,\"%s\"\n";
#else
  char format[] = "%4.4d%2.2d%2.2d,%6.4lf,\"%s\"\r\n";
#endif
  int i;
  FILE *fid_uit;

#ifdef __EMX__
  char filprint[] = "c:\\temp\\koers_up.prn";
  char filrend[]  = "c:\\temp\\rend.txt";
#else
  char filprint[] = "/home/mfvl/etc/koers_up.prn";
  char filrend[]  = "/home/mfvl/etc/rend.txt";
#endif

  char *koersinf;

  if ((koersinf=getenv("MIJN_KOERSEN")) == NULL)
    koersinf="mijn_koersen";

  if (loadtabel(koersinf) != 0)
    return 1;

  rend = fopen(filrend,"wt");

  time(&tijd);
  t = localtime(&tijd);
  jaar = t->tm_year + 1900;

  for(i=1;i<argc;i++)
    pagina(argv[i]);

#ifdef __EMX__
  fid_uit = fopen(filprint,"at");
#else
  fid_uit = fopen(filprint,"ab");
#endif

  if (fid_uit == NULL)
  {
     perror(filprint);
     return 1;
  }

  for(i=0;tabel[i].type!=laatste;i++)
  {
    if(tabel[i].koers >= 0)
    {
      fprintf(fid_uit,format,jaar,maand,dag,tabel[i].koers,tabel[i].db_nam);
      printf(format,jaar,maand,dag,tabel[i].koers,tabel[i].db_nam);
    }
  }

  fflush(stdout);
  fclose(fid_uit);
  fclose(rend);

  return 0;
}

