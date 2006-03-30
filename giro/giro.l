
%option noyywrap 
D                [0-9]
S                [-+]?
I                {S}{D}+
G1               {S}{D}*\,{D}+([eE]{I})?
G2               {I}\,{D}*([eE]{I})?
F1               {S}{D}*\.{D}+([eE]{I})?
F2               {I}\.{D}*([eE]{I})?
F3               {I}[eE]{I}
F                {F1}|{F2}|{F3}
G                {G1}|{G2}|{F3}
Q                "\""

%{
#include <stdio.h>
#include <string.h>
#include "giro.tab.h"

static char temp[512];

static char *strip (char *s) {
  char *c;

  strcpy(temp,s);
  c=temp+strlen(temp)-1;
  *c--=0;   /* end quote */
  while (c >= temp && *c==' ')
    *c-- = 0;

  c=temp;
  while (*c==' ') 
    c++;

  return c;
}

%}

%%

{Q}Datum{Q},{Q}.+                    {return HEADER;}

{Q}{D}{D}-{D}{D}-{D}{D}{D}{D}{Q}     {yylval.i=(atol(yytext+7)*10000+atol(yytext+4)*100+atol(yytext+1));return INTEGER;}

{Q}{Q}                   {return EMPTY;}

{F}                      {yylval.f=atof(yytext);return FLOAT;}
{Q}{G}{Q}                {char *c; strcpy(temp,yytext+1); c=strchr(temp,',');if (c) {*c='.';}; yylval.f=atof(temp);return FLOAT;}
{I}                      {yylval.i=atol(yytext);return INTEGER;}
{Q}{I}{Q}                {yylval.i=atol(yytext+1);return INTEGER;}     

","                      {return ',';}

{Q}[^\"}]+{Q}            {yylval.s=strdup(strip(yytext+1)); return STRING;}

[ \t\r]                    
\n                       
