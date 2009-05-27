#include <ctype.h>

#include "config.h"

int mfl_version(void)
{
  char *c = VERSION;
  int versie = 0;

  for(;*c != 0; c++)
    if (isdigit(*c)) versie=10*versie+(*c-'0');

  return versie;
}
