%token TABLE
%token TABLEEND
%token ROW
%token ROWDESC
%token ROWHEAD
%token <s> STRING
%token <f> FLOAT

%type <s> strings
%type <f> val
%type <f> val1

%union {
  char *s;
  double f;
}

%{

#include <stdlib.h>
#include <stdio.h>

int yylex(void);

void yyerror (char *s) {
  fprintf (stderr, "%s\n", s);
}

%}

%%

program     : TABLE rowheader rows TABLEEND
            ;

rows        : row
            | row rows
            ;

row         : ROW ROWHEAD STRING {/* fonds */         printf("%-20s ",$3); }
              ROWDESC val1
              ROWDESC val 
              ROWDESC tijd
              ROWDESC val 
              ROWDESC val 
              ROWDESC val1 
              ROWDESC strings     {/* ignore rest */ printf("%10.2f %6.2f\n",$6-$8,100*$8/($6-$8)); }
            ;

val1        : val
            | val strings {$$=$1;}
            ;

strings     : STRING;
            | STRING strings
            ;

val         : STRING { printf("%10s ",$1); $$=atof($1); }
            | FLOAT  { printf("%10.2f ",$1); $$=$1; }
            ;

tijd        : STRING     {/* tijd */     printf("%6s ",$1); }
            | { printf("%6s "," "); }
            ;

rowheader   : ROW rowheads { printf("%-20s %10s %10s %6s %10s %10s %10s %10s %6s\n",
                "Fonds","Huidig","Verschil","Tijd","Hoog","Laag","Open","Vorig","Perc."); }
            ;

rowheads    : rowhead 
            | rowheads rowhead
            ;

rowhead     : ROWHEAD strings 
            ;

strings     : STRING
            | STRING strings 
            ;

%%
extern int yydebug;

int main(int argc) {
  yydebug=(argc >1?1:0);
  if (yyparse() != 0) {
    return(1);
  }
  return 0;
}
