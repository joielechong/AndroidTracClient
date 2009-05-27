#include <cgi.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <www.h>
#include "config.h"

void stat_err(int d,char *s)
{
  printf("Status: %d %s\n\n",d,s);
  exit(0);
}

void require_post(cgi_info *ci)
{
  if (ci == NULL || ci->request_method == NULL)
    stat_err(500,"Internal server error");
  if (STRCASECMP(ci->request_method,"POST") != 0)
    stat_err(403,"Illegal call to script");
}
