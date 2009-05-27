#include <www.h>

static void def_footer(FILE *fid)
{
  fprintf(fid,"\n</body></html>\n");
}

void footer(FILE *fid)
{
  if (!is_style())
    def_footer(fid);
  else 
    style_footer(fid);
}

void print_opdr(FILE *fid)
{
  fprintf(fid,"\n<!--\n  CGI software geschreven door Michiel van Loon (michiel@van-loon.xs4all.nl)\n-->\n\n");
}

static void def_header(FILE *fid,char *titel,char *b,char *c)
{
  fprintf(fid,"<html><head><title>\n");
  fprintf(fid,"%s\n",(titel[0] == '-' ? titel+1 : titel));
  fprintf(fid,"</title>\n");
  print_opdr(fid);
  fprintf(fid,"<link rev=made href=\"mailto:michiel@van-loon.xs4all.nl\">\n</head><body>\n");
  if(titel[0] != '-')
    fprintf(fid,"<center><h1>%s</h1></center>\n",titel);
}

void header(FILE *fid,char *titel,char *b, char *c)
{
  if (!is_style())
    def_header(fid,titel,b,c);
  else
    style_header(fid,titel);
}
