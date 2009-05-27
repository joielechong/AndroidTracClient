#include <time.h>
#include <mfl.h>
#include <define.h>

int paasdag(int jaar)
{
  int b,c,d,e,f,g,h,i,j,k,l,m,n;
  struct tm dat;

  b = jaar % 19;
  c = jaar / 100;
  d = jaar % 100;
  e = c / 4;
  f = c % 4;
  g = (c + 8) / 25;
  h = (c - g + 1) / 3;
  i = (19 * b + c - e - h + 15) % 30;
  j = d / 4;
  k = d % 4;
  l = (32 + 2 * f + 2 * j - i - k) % 7;
  m = (b + 11 * i + 22 * l) /451;
  n = i + l - 7 * m + 114;
  dat.tm_mon = (n / 31) - 1;
  dat.tm_mday = (n % 31)+1;
  dat.tm_year = jaar;
  return DayNumber(&dat);
}

int feestdag(struct tm *d)
{
  int pasen,dag_num;

  if (d->tm_mon==0 && d->tm_mday==1)
    return TRUE;                  /* nieuwjaarsdag */
  else if (d->tm_mon==3 && d->tm_mday == 30)
    return TRUE;                  /* koninginnedag */
  else if (d->tm_mon==11 && (d->tm_mday==25 || d->tm_mday==26))
    return TRUE;                  /* kerstmis */
  else if ((d->tm_year % 5) == 0 && d->tm_mon == 4 && d->tm_mday == 5)
    return TRUE;									/* 5 mei eens in de 5 jaar */
  else
  {
    dag_num = DayNumber(d);
    pasen   = paasdag(d->tm_year);
    switch (dag_num-pasen)
    {
    case 0:
    case 1:
    case 39:
    case 50:
      return TRUE;
    }
  }
  return FALSE;
}
