#include <string.h>
#ifdef DEBUG
#include <stdio.h>
#endif
#include <stdlib.h>
#ifdef DEBUG_LOG
#include <stdarg.h>
#endif
#include <unistd.h>

/*
check_browser : check browser for capabilities.

int check_browser(const char *name,int *table,int *push,int *text);

Arguments:

Input:
  char *    name        Identification  of the browser (from HTTP_USER_AGENT)

Output:    (1 is capable, 0 is not capable)
  int *     table       Can process <TABLE>
  int *     push        Can process serverpush
  int *     text        Text_only browser

Returns:
  0        Browser not recognized (all outputs set to 0)
  1        Browser recognized

*/

static struct browser
{
  char *name;
  int   has_table;
  int   has_push;
  int   text_only;
} browsers[] = 
/* 
   The following list is processed top down. so special cases must be 
   put before more general cases.
   The name provided as the first argument is checked to start with the 
   value in the table.
*/
/*    N                                T P T
      a                                A U E
      a                                B S X
      m                                L H T
*/
  {
    {"Mozilla/0."                     ,0,0,0},
    {"Mozilla/1.0"                    ,0,0,0},
    {"Mozilla/1.22 (compatible;"      ,1,0,0},
    {"Mozilla/2.0 (compatible;"       ,1,0,0},
    {"Mozilla/1."                     ,1,1,0},
    {"Mozilla/"                       ,1,1,0},
    {"IBM WebExplorer DLL /v1.03"     ,1,0,0},
    {"IBM-WebExplorer-DLL/v1.1"       ,1,0,0},
    {"IBM WebExplorer DLL /v96"       ,1,0,0},
    {"Lynx"                           ,0,0,1},
    {"Harvest"                        ,0,0,1},
    {"Microsoft Internet Explorer"    ,1,0,0},
    {"NetManage Chameleon WebSurfer"  ,1,0,0},
    {"",0,0,0}  /* always at end */
  };


int check_browser(const char *name,int *table,int *push,int *text)
{
  struct browser *b;
  int retval = 0;
#ifdef DEBUG
  FILE *fid;
#endif

  *table = 0;
  *push  = 0;
  *text  = 0;

#ifdef DEBUG
  fid = fopen("browser.chk","at");
#endif

  for(b=browsers;b->name[0] != 0;b++)
  {
    if (strncmp(name,b->name,strlen(b->name)) == 0)
    {
      *table = b->has_table;
      *push  = b->has_push;
      *text  = b->text_only;
      retval =1;
      break;
    }
  }
  
#ifdef DEBUG
  fprintf(fid,"%s: %d %d %d %d\n",name,*table,*push,*text,retval);
  fclose(fid);
#endif
  return retval;
}
