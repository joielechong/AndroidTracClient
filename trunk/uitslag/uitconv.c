#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <ctype.h>

#define index strchr

char *strip_cr(char *b) {
  size_t len = strlen(b);
  
  while (len > 0 && index("\n\r \t",b[len-1]) != NULL)
    b[--len] = 0; 
  return b;
}

void uitconv(char *name) {
  FILE *fin,*fout=stdout;
  char buffer[512];
  char *c,*d;
  int state = 0,error = 0,nafst,i,ndeeln;
  char *datum=NULL,*wedstrijd=NULL,*uitfile=NULL;
  
  if (name == NULL)
    return;
  if((fin = fopen(name,"rt")) == NULL) {
    perror(name);
    return;
  }
  
  uitfile = strdup(name);
  c = strstr(uitfile,".txt");
  strncpy(c,".uit",4);
  
  fout = fopen(uitfile,"wt");
  
  while(fgets(buffer,sizeof(buffer),fin) != NULL && !error) {
    strip_cr(buffer);
    switch (state) {
    case 0:
      if(buffer[0] != 0x1b) {
        fprintf(stderr,"File %s begint niet met een ESCAPE code\n",name);
        error = 1;
	break;
      }
      /* falls through !! */
    case 1:
    case 2:
    case 4:
    case 5:
    case 9:
      state++;
      break;
      
// Verwerk 4e regel
    case 3:
      if(buffer[0] != 0x1b) {
        fprintf(stderr,"File %s: ESCAPE code verwacht op de 4e regel\n",name);
        error = 1;
	break;
      }
      else {
	if (buffer[1] == 0xf)
	  c = buffer+1;
	else
	  c = strchr(buffer,'H');
	
	if(c == NULL) {
	  fprintf(stderr,"File %s: Onverwachte ESCAPE code op regel 4, ik verwachtte een 'H'\n",name);
	  fprintf(stderr,"         Het was een %c (0x%2X)\n",buffer[1],buffer[1]);
	  error =1 ;
	  break;
	}
	d = strchr(c++,' ');
	*d++ = 0;
	datum = strdup(c);
	while(*d++==' ');
	wedstrijd = strdup(d-1);
	fprintf(fout,"%s %s\n",datum,wedstrijd);
	state++;
      }
      break;
      
// Verwerk 1e kopregel voor afstanden combinatie (wordt ook in 12 gedaan)
    case 6:
      if (strstr(buffer,"tijd") != NULL)
	state++;
      break;
      
// Verwerk 2e kopregel voor afstanden combinatie
    case 7:
      c = strstr(buffer,"Nr   Deelnemer");
      if (c == NULL) {
	fprintf(stderr,"File %s: Kopje verwacht\n",name);
	error = 1;
      }
      else {
	nafst = 0;
	while(!isdigit(*c++));
	--c;
	d = strstr(c,"punten");
	if(d!= NULL) {
	  *d = 0;
	  strip_cr(c);
	}
	d = strstr(c,"achter");
	if(d!= NULL) {
	  *d = 0;
	  strip_cr(c);
	}
	
	fprintf(fout,"\t");
	while ((d = strchr(c,'m')) != NULL) {
	  d++;
	  *d++ = 0;
	  fprintf(fout,"\t%s",c);
	  nafst++;
	  c = d;
	  while(!isdigit(*c++));
	  --c;
	}
	fprintf(fout,"\tpunten\n");
	ndeeln = 0;
	state++;
      }
      break;
      
    case 10:
      state = 8;
      break;
      
// Verwerk categorie kopje
    case 12:
      fprintf(fout,"\n");
      if(buffer[0] == 0x1b) {
	if (buffer[1] == 0x2d)
	  c = buffer + 3;
	else
	  c = buffer + 5;
	d = strchr(c,0x1b);
	if (d != NULL)
	  *d = 0;
	fprintf(fout,"%s\n",c);
	ndeeln = 0;
	state = 8;
      }
// blijkt toch geen categorie kop te zijn dan state 6 herhalen of volhgende regel in 6
      else if (strstr(buffer,"--") != NULL)
	state=13;
      else if (strstr(buffer,"tijd") != NULL)
	state = 7;
      else if (strlen(buffer) == 0)
	state = 6;
      else {
	error = 1;
	fprintf(stderr,"File %s: Ik had een kopje of subkopje verwacht\n",name);
      }
      break;

    case 13:
      if (strlen(buffer) == 0)
	state=8;
      break;

    case 14:
      state=8;
      break;
      
// Verwerk een gewone regel (naam + tijden en punten)
    case 8:
// eerst wat eindmarkers checken
      if(buffer[0] == '\f') {
	state++;
	break;
      }
      if(strlen(buffer) == 0) {
	state = 12;
	break;
      }

// 't is een gewone regel, formatteer voor uitvoer      
      if(strlen(buffer) >=10 && buffer[9] == '.') {
	c = buffer + 10;
	buffer[36] = 0;
	strip_cr(c);
	fprintf(fout,"%s",c);
	c = buffer + 37;
	buffer[40] = 0;
	fprintf(fout,"\t%s",c);
	c = buffer+49;
	for(i=0;i<=nafst;i++) {
	  while(*c++ == ' ');
	  --c;
	  d = strstr(c,"  ");
	  if(d!=NULL)
	    *d = 0;
	  fprintf(fout,"\t%s",c);
	  fflush(fout);
	  c = d+1;
	}
	fprintf(fout,"\n");
      }
      else {
	fprintf(fout,"\n");
	state = 6;
      }
      break;
    }
  }

// Klaar, eventueel als gevolg van fout, dan niet uit state 6

  if (state != 6)
    fprintf(stderr,"Het ging niet goed met file %s. State = %d\n",name,state);
  else 
    fprintf(stderr,"File %s verwerkt\n",name);
  
  fclose(fin);
  
  if(fout != stdout) {
    fclose(fout);
//    if (error)
  //    unlink(uitfile);
  }
  
  if (wedstrijd!=NULL) free(wedstrijd);
  if (datum != NULL) free(datum);
  if (uitfile != NULL) free(uitfile);
}

int main(int argc,char *argv[]) {
  DIR *dirp;
  struct dirent *de;
  
  dirp = opendir(".");
  while((de=readdir(dirp)) != NULL) {
    if(strstr(de->d_name,".txt") != NULL)
      uitconv(de->d_name);
  }
  closedir(dirp);
  return 0;
}
