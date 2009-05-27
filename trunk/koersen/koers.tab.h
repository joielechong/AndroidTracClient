typedef union {
  int i;
  long l;
  char *s;
  double f;
} YYSTYPE;
#define	OPENBLOCK	258
#define	CLOSEBLOCK	259
#define	STRING	260
#define	FLOAT	261
#define	NORMAAL	262
#define	OPTIE_FL	263
#define	OPTIE_REL_FL	264
#define	OPTIE_EUR	265
#define	OPTIE_REL_EUR	266
#define	LAATSTE	267


extern YYSTYPE yylval;
