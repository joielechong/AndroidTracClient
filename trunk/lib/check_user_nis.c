#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <pwd.h>
#ifndef OS2
#include <crypt.h>
#include <rpcsvc/ypclnt.h>
#include <rpcsvc/yp_prot.h>
#endif

static struct passwd pwd={
  NULL,NULL,0,0,0,0,NULL,NULL,NULL
};

#ifdef OS2
void *crypt(char *s,char *p);

static int yp_get_default_domain(char **d)
{
  *d = "dds.nl";
  return 0;
}

static int yp_match(char *domain,char *inmap,char *inkey,int keylen,char**out,int *outlen)
{
  char *buffer;

  buffer = malloc(256);
  sprintf(buffer,"%s:hcsskj:%d:%d:De Naam:/directory:/shell",inkey,inkey[0],inkey[1]);
  *out = buffer;
  *outlen = strlen(buffer);
  return 0;
}
#endif

struct passwd *check_user_nis(char *user,char *sent_pw)
{
  char *domain, *p, *password,*s_uid,*s_gid,*t,*u;
  int l, yp_err;

  if(pwd.pw_name != NULL)
  {
    free(pwd.pw_name);
    pwd.pw_name = NULL;
    free(pwd.pw_passwd);
    pwd.pw_passwd = NULL;
    free(pwd.pw_gecos);
    pwd.pw_gecos = NULL;
    free(pwd.pw_dir);
    pwd.pw_dir = NULL;
    free(pwd.pw_shell);
    pwd.pw_shell = NULL;
  }

  if ((yp_err = yp_get_default_domain(&domain)) != 0)
    return NULL;

  if ((yp_err = yp_match(domain,"passwd.byname",user,strlen(user),&p,&l)) != 0)
    return NULL;

  password = strchr(p,':')+1;
  s_uid = strchr(password,':');
  *s_uid++ = 0;
  pwd.pw_name = strdup(p);
  s_gid = strchr(s_uid,':');
  *s_gid++ = 0;
  pwd.pw_passwd = strdup(password);
  pwd.pw_uid = atol(s_uid);
  pwd.pw_gid = atol(s_gid);
  t = strchr(s_gid,':');
  *t++ = 0;
  u = strchr(t,':');
  *u++ = 0;
  pwd.pw_gecos = strdup(t);
  t = strchr(u,':');
  *t++ = 0;
  pwd.pw_dir = strdup(u);
  pwd.pw_shell = strdup(t);

  free(p);

  if (strcmp(password,(char *)crypt(sent_pw,password)) != 0)
    return NULL;

  return &pwd;
}
