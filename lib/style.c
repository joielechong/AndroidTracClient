#include <www.h>
#include <mfl.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "config.h"

#define STYLENAME ".stijlblad.html"
#define COOKIE    "<!--#-->"
#define TITLE     "<title></title>"
#define HEADER1   "<h1></h1>"

static char **stylelines = NULL;
static int    nlines     = 0;
static int    maxlines   = 0;
static int    cookie     = -1;
static int    title      = -1;
static int    header1    = -1;

void print_opdr(FILE *);

int load_stylepage(void)
{
  struct stat stbuf;
  FILE *fid;
  char buffer[2048];

  if(stat(STYLENAME,&stbuf) == -1)
    return -1;

  if((fid = fopen(STYLENAME,"rt")) == NULL)
    return -2;

  while(fgets(buffer,sizeof(buffer),fid) != NULL)
  {
    if(nlines == maxlines)
    {
      maxlines += 100;
      if ((stylelines = realloc(stylelines,maxlines*sizeof(stylelines[0]))) == NULL)
      {
        fclose(fid);
        return -4;
      }
    }
    strip_cr(buffer);
    if(strcmp(buffer,COOKIE)   == 0 && cookie  == -1)
      cookie  = nlines;
    if(STRCASECMP(buffer,TITLE)   == 0 && title   == -1)
      title   = nlines;
    if(STRCASECMP(buffer,HEADER1) == 0 && header1 == -1)
      header1 = nlines;
    stylelines[nlines++] = strdup(buffer);
  }

  fclose(fid);
  if(cookie < 0)
    return -5;
  else if(nlines == 0)
    return -3;

  return 0;
}

void style_header(FILE *fid,char *titel)
{
  int i;

  for(i=0;i<cookie;i++)
  {
    if(i == title)
    {
      if(titel != NULL)
        fprintf(fid,"<title>\n%s\n</title>\n",(titel[0]=='-' ? titel+1 : titel));
      print_opdr(fid);
    }
    else if(i == header1 && titel != NULL)
    {
      if(titel[0] != '-')
        fprintf(fid,"<h1>\n%s\n</h1>\n",titel);
    }
    else
      fprintf(fid,"%s\n",stylelines[i]);
  }
  if(title == -1)
    print_opdr(fid);
}

void style_footer(FILE *fid)
{
  int i;

  for(i=cookie+1;i<nlines;i++)
    fprintf(fid,"%s\n",stylelines[i]);
}

int is_style(void)
{
  if(nlines > 0)
    return 1;
  else
    return (load_stylepage() == 0);
}
