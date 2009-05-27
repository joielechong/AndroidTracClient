#include <stdio.h>
#include <stdarg.h>
#include <www.h>

void start_ref(FILE *fid,char *ref,...)
{
  va_list argp;

  va_start(argp,ref);
  fprintf(fid,"<a href=\"");
  vfprintf(fid,ref,argp);
  fprintf(fid,"\">\n");
  va_end(argp);
}

void sluit_ref(FILE *fid)
{
  fprintf(fid,"</a>\n");
}
