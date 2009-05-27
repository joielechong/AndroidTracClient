#include <stdio.h>
#include <www.h>

void radio_button(FILE *fid,char *name,char *value)
{
  fprintf(fid,"<input type=radio name=\"%s\" value=\"%s\">\n",name,value);
}

void radio_button_checked(FILE *fid,char *name,char *value)
{
  fprintf(fid,"<input type=radio name=\"%s\" value=\"%s\" checked>\n",name,value);
}

void check_box(FILE *fid,char *name,int on)
{
  fprintf(fid,"<input type=checkbox name=\"%s\"",name);
  if(on)
    fprintf(fid,"value= \"on\"");
  fprintf(fid,">\n");
}

void check_box_checked(FILE *fid,char *name,int on)
{
  fprintf(fid,"<input type=checkbox name=\"%s\"",name);
  if(on)
    fprintf(fid,"value= \"on\"");
  fprintf(fid," checked>\n");
}
