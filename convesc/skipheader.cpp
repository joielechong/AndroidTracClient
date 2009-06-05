#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define BUFSIZE (8192)
char buffer[BUFSIZE];

int main(void)
{
  int first    = 1;

  while (fgets(buffer,BUFSIZE,stdin) != NULL) {
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
  while (fgets(buffer,BUFSIZE,stdin) != NULL)
    puts(buffer);
}
