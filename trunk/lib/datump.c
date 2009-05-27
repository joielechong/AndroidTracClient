#include <time.h>
#include <mfl.h>

int LeapYear(const int jaar)
{
  if ((jaar % 4) != 0)
    return 0;
  else if ((jaar % 400) != 0)
    return 0;
  else
    return 1;
}

int DayNumber(struct tm *d)
{
  int m;
  int jaar=d->tm_year;
  int maand=d->tm_mon + 1;
  int dag=d->tm_mday;

  if (maand <= 2)
  {
    m = maand-1;
    m *= 63;
    m /= 2;
  }
  else
  {
    m = maand + 1;
    m *= 30.6;
    if (LeapYear(jaar))
      m -= 62;
    else
      m -= 63;
  }
  return m + dag;
}

long JulianDate(struct tm *d)
{
  int j = d->tm_year;
  int maand = d->tm_mon+1;
  int a,b,dd,m=maand;
  long c;

  if (maand <= 2)
  {
    j--;
    m += 12;
  }

  a = j/100;
  b = 2 - a + (a/4);
  c = (j<0 ? 365.25*j-0.75 : 365.25*j);
  dd = 30.6001 * (m+1);
  return b+c+dd+d->tm_mday+1720994.5;
}
