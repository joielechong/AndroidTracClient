#include <stdio.h>
#include <string.h>
#include <stdlib.h>

struct tabel
{
  char *code;
  char *html;
} tab[] = { {"=46","F"},
            {"=C0","&Aacute;"},
            {"=C1","&Agrave;"},
            {"=C2","&Acirc;"},
            {"=C3","&Atilde;"},
            {"=C4","&Aring;"},
            {"=C5","&Auml;"},
            {"=C6","&AElig"},
            {"=C7","&&Ccedil;"},
            {"=C8","&Eacute;"},
            {"=C9","&Egrave;"},
            {"=CA","&Ecirc;"},
            {"=CB","&Euml;"},
            {"=CC","&Iacute;"},
            {"=CD","&Igrave;"},
            {"=CE","&Icirc;"},
            {"=CF","&Iuml;"},
            {"=D0","&ETH;"},
            {"=D1","&Ntilde;"},
            {"=D2","&Oacute;"},
            {"=D3","&Ograve;"},
            {"=D4","&Ocirc;"},
            {"=D5","&Otilde;"},
            {"=D6","&Ouml;"},
            {"=D7","*"},
            {"=D8","&Oslash;"},
            {"=D9","&Uacute;"},
            {"=DA","&Ugrave;"},
            {"=DB","&Ucirc;"},
            {"=DC","&Uuml;"},
            {"=DD","&Yacute;"},
            {"=DE","&THORN;"},
            {"=DF","&szlig;"},
            {"=E0","&aacute;"},
            {"=E1","&agrave;"},
            {"=E2","&acirc;"},
            {"=E3","&atilde;"},
            {"=E4","&aring;"},
            {"=E5","&auml;"},
            {"=E6","&aelig;"},
            {"=E7","&ccedil;"},
            {"=E8","&eacute;"},
            {"=E9","&egrave;"},
            {"=EA","&ecirc;"},
            {"=EB","&euml;"},
            {"=EC","&iacute;"},
            {"=ED","&igrave;"},
            {"=EE","&icirc;"},
            {"=EF","&iuml;"},
            {"=F0","&eth;"},
            {"=F1","&ntilde;"},
            {"=F2","&oacute;"},
            {"=F3","&ograve;"},
            {"=F4","&ocirc;"},
            {"=F5","&otilde;"},
            {"=F6","&ouml;"},
            {"=F7","/"},
            {"=F8","&oslash;"},
            {"=F9","&uacute;"},
            {"=FA","&ugrave;"},
            {"=FB","&ucirc;"},
            {"=FC","&uuml;"},
            {"=FD","&yacute;"},
            {"=FE","&thorn;"},
            {"=FF","&yuml;"},
            {"=\n",""},
            {",,","<cite>"},
            {"''","</cite>"},
            {"TVM","<a href=\"ploeg.nl.html#tvm\">TVM</a>"},
            {"rabo","<a href=\"ploeg.nl.html#rabo\">RABO</a>"},
            {"ONCE","<a href=\"ploeg.e.html#once\">ONCE</a>"},
            {NULL,NULL}};

int main(void)
{
  char buffer[1024],*c;


  while (fgets(buffer,sizeof(buffer),stdin) != NULL)
  {
    c = buffer;
    while (*c)
    {
      int i;

      for(i=0;tab[i].code != NULL;i++)
      {
        if(strnicmp(tab[i].code,c,strlen(tab[i].code)) == 0)
        {
          fputs(tab[i].html,stdout);
          c += strlen(tab[i].code)-1;
          break;
        }
      }
      if (tab[i].code == NULL)
        fputc(*c,stdout);
      c++;
    }
  }

  return 0;
}