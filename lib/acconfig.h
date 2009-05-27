/* Define to the name of the distribution.  */
#undef PACKAGE

/* Define to 1 if ANSI function prototypes are usable.  */
#undef PROTOTYPES

/* Path to directory containing system wide message catalog sources.  */
#undef STD_INC_PATH

/* Define to the version of the distribution.  */
#undef VERSION

@BOTTOM@

#ifdef HAVE_STRICMP
#define STRCASECMP stricmp
#define STRNCASECMP strnicmp
#else
#define STRCASECMP strcasecmp
#define STRNCASECMP strncasecmp
#endif
