#ifndef BISON_KOERSSYN_H
# define BISON_KOERSSYN_H

#ifndef YYSTYPE
typedef union {
  int i;
  long l;
  char *s;
  double f;
} yystype;
# define YYSTYPE yystype
# define YYSTYPE_IS_TRIVIAL 1
#endif
# define	OPENBLOCK	257
# define	CLOSEBLOCK	258
# define	STRING	259
# define	FLOAT	260
# define	NORMAAL	261
# define	OPTIE_FL	262
# define	OPTIE_REL_FL	263
# define	OPTIE_EUR	264
# define	OPTIE_REL_EUR	265
# define	LAATSTE	266


extern YYSTYPE yylval;

#endif /* not BISON_KOERSSYN_H */
