#include <stdio.h>

int main(void) {
  int level=0,c,i;

  while ((c=fgetc(stdin)) != EOF) {
    fputc(c,stdout);
    if (c=='\n') {
      for(i=0;i<(level*4);i++) {
	fputc(' ',stdout);
      }
    } else if (c=='(') {
      level++;
    } else if (c==')') {
      level--;
    }
  }
  fputc('\n',stdout);
  return 0;
}


