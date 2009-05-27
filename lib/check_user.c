#include <pwd.h>
#include <string.h>
#include <unistd.h>
#include <crypt.h>

int check_user(const char *name,const char *password)
{
  struct passwd* p;

  if (strcmp(name,"gast") == 0)
    return 0;

  if ((p = getpwnam(name)) == NULL)
    return 0;

  if (crypt(password,p->pw_passwd) == NULL)
    return 0;

  return 1;
}


