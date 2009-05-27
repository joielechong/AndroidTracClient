#include <stdio.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <sys/types.h>
#include <dirent.h>

#define START_DAY 4
#define START_MON 6
#define START_YEAR 1996

time_t today,start;

#define TTSDIR "d:/tts/koersen"

struct {
  char    naam[20];
  double  waarde[4096];
} fonds;

int main(void)
{
  char buffer[80];
  FILE *fid;
  DIR *dirp;
  struct tm t;
  FILE *fid_out;

  time(&today);
  today /= 86400;

  memset(&t,0,sizeof(t));
  t.tm_year = START_YEAR-1900;
  t.tm_mon  = START_MON;
  t.tm_mday = START_DAY;
  start = mktime(&t) / 86400;

  printf("start = %ld today = %ld dagen = %ld\n",start,today,today-start);
  
  fid_out = fopen("/tmp/plat.prn","wt");
  if (fid_out == NULL) {
    perror("plat.prn");
    return 4;
  }

  if ((dirp = opendir(TTSDIR)) != NULL) {  
    for(struct dirent *d=readdir(dirp);d!=NULL;d=readdir(dirp)) {
      sprintf(buffer,"%s/%s",TTSDIR,d->d_name);
      if (d->d_attr & A_DIR) continue;
      memset(&fonds,0,sizeof(fonds));
      fprintf(fid_out,"\"%s\"",d->d_name);

      if ((fid = fopen(buffer,"rt")) != NULL) {
        while (fgets(buffer,sizeof(buffer),fid) != NULL) {
          int d,m,j,u,min;
          double koers;
          time_t ts;
          char *c;
	  
          sscanf(buffer,"%d/%d/%d",&d,&m,&j);
	  
          if ((c = strchr(buffer+15,',')) != NULL)
            *c = '.';
	  
          sscanf(buffer+9,"%d:%d %lf",&u,&min,&koers);
          memset(&t,0,sizeof(t));
          t.tm_year = j;
          t.tm_mon = m;
          t.tm_mday = d;
          ts = mktime(&t) / 86400;
	  fonds.waarde[ts-start] = koers;
        }
        fclose(fid);

	for (long i=0;i<(long)(today-start);i++) {
	  if (fonds.waarde[i] == 0)
	    fprintf(fid_out,", ");
	  else
	    fprintf(fid_out,",%.2f",fonds.waarde[i]);
	}
	fprintf(fid_out,"\n");
      }
    }
  }
  fclose(fid_out);
}
