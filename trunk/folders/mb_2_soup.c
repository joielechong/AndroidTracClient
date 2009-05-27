/*
  This program is written by Michiel van Loon
*/
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

struct message
{
  int bufsiz;
  char *buffer;
};

int main(int argc,char *argv[])
{
  FILE *fid;
  int nmsg = 0,i;
  int bufsiz;
  char filename[80];
  struct message *m = NULL;

  if(argc != 2)
  {
    printf("Call: mb_2_soup <folder>\n");
    printf("\nProgram to convert a YARN folder to a SOUP file.\n");
    printf("Program written by Michiel van Loon (C) 1996\n");
    return 1;
  }
 
  if((fid=fopen(argv[1],"rb")) == NULL)
  {
    printf("Folder %s does not exist\n",argv[1]);
    return 1;
  }

  while(!feof(fid))
  {
    if (fread(&bufsiz,sizeof(bufsiz),1,fid) < 1)
      break;
    if((m = realloc(m,(nmsg+1)*sizeof(*m))) == NULL)
    {
      printf("Increasing of message-array failed, message nr %d\n",nmsg+1);
      return 1;
    }
    if((m[nmsg].buffer = calloc(bufsiz+1,1)) == NULL)
    {
      printf("Creation of buffer failed, message nr %d\n",nmsg+1);
      return 1;
    }
    m[nmsg].bufsiz = bufsiz;
    fread(m[nmsg].buffer,1,bufsiz,fid);
    nmsg++;
  }
  fclose(fid);
  printf("All messages read\n");
  sprintf(filename,"%s.old",argv[1]);
  rename(argv[1],filename);

  fid=fopen(argv[1],"wb");
  for(i=0;i<nmsg;i++)
  {
    time_t now;
    char buffer[80];

    time(&now);

    bufsiz = m[i].bufsiz;
    sprintf(buffer,"From mfvl@xs4all.nl %s",ctime(&now));
    fwrite(buffer,strlen(buffer),1,fid);
    fwrite(m[i].buffer,1,bufsiz,fid);
  }
  fclose(fid);

  sprintf(filename,"%s.msg",argv[1]);
  rename(argv[1],filename);

  printf("Messages written to file %s.msg\nold folder is renamed to %s.old\n",argv[1],argv[1]);

  return 0;
}
