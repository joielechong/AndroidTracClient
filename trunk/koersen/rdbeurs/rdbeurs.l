D      [0-9]

%start TABEL CAPTION

%{

#include <string.h>
#include <stdlib.h>
#include "rdbeurs.tab.h"

static char stringetje[128];

char *copystr(char *s) {
  memset(stringetje,0,sizeof(stringetje));
  strncpy(stringetje,yytext,yyleng);
  return stringetje;
}

double convflt(char *s)
{
  char *d;

  copystr(s);
  d=stringetje;
  if (*d=='-') d++;
  for(;*d!=0;d++) 
    if (*d ==',') strcpy(d,d+1);

  return(atof(stringetje));
}

%}

%%

"<table cellspacing"[^>]*">"                  { BEGIN TABEL; return(TABLE); }

<TABEL>"<A "[^>]*">"               /* ignore */ ;
<TABEL>"</A>"                      /* ignore */ ;
<TABEL>"</table>"                  { BEGIN 0; return(TABLEEND); }
<TABEL>"<tr"[^>]*">"               return(ROW);
<TABEL>"<th"[^>]*">"               return(ROWHEAD);
<TABEL>"<td"[^>]*">"               return(ROWDESC);
<TABEL>"<IMG"[^>]*">"              /* ignore */ ;
<TABEL>"<caption>"                 BEGIN CAPTION;
<CAPTION>"</caption>"              BEGIN TABEL;
<CAPTION>.                         /* ignore */ ;
<CAPTION>"\n"                      /* ignore */ ;
<TABEL>"<br>"                      {yylval.s="";return STRING; }
<TABEL>"<"[^>]+">"                 /* ignore */ ; 

<TABEL>[0-9]+":"[0-9]+             { yylval.s=copystr(yytext);return STRING; }
<TABEL>[-+]?[0-9,.]+[ ]*[-+ABX]?[ ]*"y"?[ ]*  { yylval.f=convflt(yytext); return FLOAT; }
<TABEL>"&nbsp;"                    { yylval.s=" ";return STRING; }
<TABEL>"\r"                        /* ignore */;
<TABEL>"\n"                        /* ignore */;
<TABEL>[^<\r\n]+                   { yylval.s=copystr(yytext);return STRING; }


"\n"                               /* ignore */ ;
.                                  /* ignore */ ;
%%
