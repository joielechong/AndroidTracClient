#include <string.h>
#define index strchr


char *strip_cr(char *b)
{
  size_t len = strlen(b);

  while (len > 0 && index("\n\r \t",b[len-1]) != NULL)
    b[--len] = 0; 
  return b;
}

