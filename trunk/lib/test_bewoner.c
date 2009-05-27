#include <string.h>
#include <cgi.h>
#include <www.h>
#include "config.h"

int is_bewoner(cgi_info *ci)
{
  if(ci == NULL)
    return 1;

  if (ci->remote_user == NULL)
    return 0;

  if (STRCASECMP(ci->remote_user,"gast") == 0)
    return 0;
  return 1;
}

