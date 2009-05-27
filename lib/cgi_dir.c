#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <www.h>
#include "config.h"

void cgi_directory(cgi_info *ci)
{
  if (ci == NULL)
    return;

  if(ci->path_translated == NULL)
    return;

  if(STRNCASECMP(ci->script_name,"/cgi-bin",8) != 0)
    return;

  chdir(ci->path_translated);
}


