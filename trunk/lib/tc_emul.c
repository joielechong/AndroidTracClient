#define INCL_KBD
#ifdef OS2
#include <os2.h>
#endif
#include <mfl.h>

int getdisk(void)
{
  ULONG disknum,logical;

  DosQueryCurrentDisk(&disknum,&logical);
  return (int) disknum;
}

int setdisk(int disknum)
{
  DosSetDefaultDisk((ULONG)disknum);
  return 0;
}

int getcurdir(int drive,char *path)
{
  char buffer[2048];
  ULONG lengte = 2048;
  int iret;

  iret = DosQueryCurrentDir((ULONG)drive,(PBYTE)&buffer,&lengte);
  strcpy(path,buffer);
  return iret;
}

int kbhit (void)
{
   KBDKEYINFO kbci;
   APIRET rc;
   rc = KbdCharIn (&kbci, IO_NOWAIT, (HKBD)0);
   return ((kbci.fbStatus & 0x40) ? (int)kbci.chChar : 0);
}
