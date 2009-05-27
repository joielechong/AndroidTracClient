#include <ctype.h>
#include <mfl.h>

char *strupr(char *string)
{
  char *c;

  for (c=string;*c!=0;c++)
    *c=toupper(*c);
  return string;
}

char *strlwr(char *string)
{
  char *c;

  for (c=string;*c!=0;c++)
    *c=tolower(*c);
  return string;
}

