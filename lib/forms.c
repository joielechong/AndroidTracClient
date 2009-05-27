#include <stdio.h>
#include <string.h>
#include <mfl.h>

void start_form(FILE *fid,char *cginame)
{
  char *s;

  s = strdup(cginame);
  strlwr(s);
  if(strstr(s,".cgi") == NULL)
#ifdef OS2
    fprintf(fid,"<form method=\"post\" action=\"%s.exe\">\n",cginame);
#else
    fprintf(fid,"<form method=\"post\" action=\"%s.cgi\">\n",cginame);
#endif
  else
    fprintf(fid,"<form method=\"post\" action=\"%s\">\n",cginame);
}

void start_form_user(FILE *fid,char *cginame,char *arg)
{
  char *s;

  s = strdup(cginame);
  strlwr(s);
  if(strstr(s,".cgi") == NULL)
#ifdef OS2
    fprintf(fid,"<form method=\"post\" action=\"%s.exe%s\">\n",cginame,arg);
#else
    fprintf(fid,"<form method=\"post\" action=\"%s.cgi%s\">\n",cginame,arg);
#endif
  else
    fprintf(fid,"<form method=\"post\" action=\"%s%s\">\n",cginame,arg);
}

void button_submit(FILE *fid,char *value)
{
  fprintf(fid,"<input type=submit value = \"%s\">",value);
}

void button_reset(FILE *fid,char *value)
{
  fprintf(fid,"<input type=reset value = \"%s\">",value);
}

void end_form(FILE *fid)
{
  fprintf(fid,"</form>\n");
}

void sluit_form(FILE *fid)
{
  fprintf(fid,"<center>\n");
  button_submit(fid,"Verzenden");
  button_reset(fid,"Wissen");
  fprintf(fid,"</center>\n");
  end_form(fid);
}

void sluit_form_gb(FILE *fid)
{
  fprintf(fid,"<center>\n");
  button_submit(fid,"Send");
  button_reset(fid,"Erase");
  fprintf(fid,"</center>\n");
  end_form(fid);
}
