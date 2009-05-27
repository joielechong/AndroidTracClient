#include <string.h>
#include <stdlib.h>
#include <pwd.h>
#include <www.h>

#define INGROUP 100

static int make_num(char *s)
{
  int a = 0;

  if(s == NULL)
    return 0;

  while (*s != 0)
    a += *s++;

  return a;
}

static int make_uid(char *s)
{
  int a = make_num(s);

  if(s == NULL)
    return 0;

  return(a%INGROUP);
}

static int make_gid(char *s)
{
  int a = make_num(s);

  if(s == NULL)
    return 0;

  return(a / INGROUP);
}

int eval_nis(struct passwd *pwd)
{
  char *p,*s_uid,*s_gid,*u,*t,*password;

  p = getenv("SERVER_SOFTWARE");
  if (p != NULL && strstr(p,"MFVL") != NULL)
  {
    pwd->pw_name = strdup(getenv("REMOTE_USER"));
    pwd->pw_passwd = "geheim";
    pwd->pw_uid = make_uid(pwd->pw_name);
    pwd->pw_gid = make_gid(pwd->pw_name);
    pwd->pw_gecos = "Michiel van Loon";
    return 0;
  }

  p = getenv("NIS_ENTRY");
  if(p == NULL)
    return -1;

  password = strchr(p,':');
  *password++=0;
  s_uid = strchr(password,':');
  *s_uid++ = 0;
  pwd->pw_name = strdup(p);
  s_gid = strchr(s_uid,':');
  *s_gid++ = 0;
  pwd->pw_passwd = strdup(password);
  pwd->pw_uid = atol(s_uid);
  pwd->pw_gid = atol(s_gid);
  t = strchr(s_gid,':');
  *t++ = 0;
  u = strchr(t,':');
  *u++ = 0;
  pwd->pw_gecos = strdup(t);
  t = strchr(u,':');
  *t++ = 0;
#ifndef OS2
  pwd->pw_dir = strdup(u);
  pwd->pw_shell = strdup(t);
#endif
  return 0;
}
