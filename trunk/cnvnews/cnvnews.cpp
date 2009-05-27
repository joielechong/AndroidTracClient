#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <mfl.h>

char buffer[1024];

int main(int argc, char *argv[])
{
  if (argc != 2)
  {
    fprintf(stderr,"Aanroep: %s nr\n",argv[0]);
    return 4;
  }

  long cnt = atol(argv[1]);
  FILE *fid = NULL;

  while (fgets(buffer,sizeof(buffer),stdin) != NULL)
  {
    if (strnicmp(buffer,"From mb_2_soup",14) == 0)
    {
      char filnam[14];

      if (fid != NULL)
        fclose(fid);
      sprintf(filnam,"%ld.nws",cnt++);
      fid = fopen(filnam,"wt");
      if (fid == NULL)
      {
        perror(filnam);
        return 4;
      }
    }
    else if (fid != NULL)
      fputs(buffer,fid);
  }
  if (fid != NULL)
    fclose(fid);
  return 0;
}