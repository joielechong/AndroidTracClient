#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char buffer[8192];

int main(void)
{
  int first    = 1;

  while (gets(buffer) != NULL) {
    if (strlen(buffer) == 0) {
      break;
    }
    if ((buffer[0] == ' ' || buffer[0] == 0x9) && !first)
      continue;
    char *c = strchr(buffer,':');
    if (c == NULL) {
      puts(buffer);
      break;
    }
    first = 0;
  }
  while (gets(buffer) != NULL)
    puts(buffer);
}
