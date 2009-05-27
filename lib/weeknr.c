#include <time.h>
#include <stdio.h>
#include <stdlib.h>

static int month_tot[]={
  0,0,31,59,90,120,151,181,212,243,273,304,334,365
};
int week_start=2;

static int leap_yr( int yr )
{
  return ( yr%4 == 0 ) - ( yr%100 == 0 ) + ( yr%400 == 0 );
}

static int weeking( int i )
{
  int ow = week_start;

  week_start = i%7;
  return ow;
}

/* This year has FULLY passed!**/
static int year2days( int yr )
{
  int s = 0;

  s=yr*365;        /* Add extras because of yr 0   */
  s+=(yr+3)/4;     /* Leap year every 4 years      */
  s-=(yr+99)/100;  /* No leap year every 100 years */
  s+=(yr+399)/400; /* Leap year every 400 years    */
  return s;
}

/* convert ymd to days since 1-1-0**/
static int ymd2days( int yr, int mon, int day )
{
  int month_days, is_leap;

  month_days = month_tot[ mon+1 ] - month_tot[ mon ];
  is_leap = leap_yr( yr );
  if ( mon == 2 ) month_days += is_leap;
  if ( mon < 1 || mon > 12 || day < 1 || day > month_days ) return -1;
  if ( mon <= 2 ) is_leap = 0;
  return year2days( yr ) + month_tot[mon] + day + is_leap - 1;
}

/* Delivers the weekday 0=sat 1=sun 2=mon ... */
static int weekday( int s )
{
  return (s-2)%7;
}


void td_now( int *yr, int *mon, int *day)
{
  time_t t;
  struct tm *t1;

  time(&t);
  t1 = localtime(&t);

  *yr  = t1->tm_year + 1900;
  *mon = t1->tm_mon + 1;
  *day = t1->tm_mday;
}

void weeknr(int yr,int mon,int day,int *yruit,int *wkuit)
{
  int s,s1,wd,wd1,ds,wk;

  if ((s = ymd2days(yr,mon,day)) == -1)
  {
    *wkuit = *yruit = -1;
    return;
  }
  weeking(2);
  wd = weekday(s);
  s1 = ymd2days(yr,1,1);
  wd1 = weekday(s1);
  if (wd1<3)
    s1 -= wd1;
  else
    s1 += (7-wd1);
  ds = s-s1;
  if (ds < 0) 
  {
    yr--;
    s1 = ymd2days(yr,1,1);
    wd1 = weekday(s1);
    if (wd1<3)
      s1 -= wd1;
    else
      s1 += (7-wd1);
    ds = s-s1;
  }
  wk = 1+(ds/7);
  if (wk == 53)
  {
    wd = weekday(ymd2days(yr,12,31));
    if (wd < 2)
    {
      yr++;
      wk = 1;
    }
  }
  *yruit = yr;
  *wkuit = wk;
}

