#include <stdio.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <sys/types.h>
#include <dirent.h>

#include <typeinfo>
#include <stdexcep>
#include <iostream.h>
#include <fstream.h>
#include <stl.h>

#define TTSDIR "d:/tts/koersen"
#define DRMP   0.25


class Datapoint
{
  public:
    Datapoint(time_t xx=0,double yy=0) : x(xx), y(yy){}
    ~Datapoint() {}

    int operator==(const Datapoint &t) const {return x==t.x && y==t.y;}
    int operator<(const Datapoint &t) const;

    time_t xv() const { return x;}
    int   xvd() const { return x/86400L -9680;}
    double yv() const { return y;}


  private:
    time_t x;
    double y;
};

int Datapoint::operator<(const Datapoint& ts) const
{
  if (x < ts.x) return 1;
  if (x > ts.x) return 0;
  return y < ts.y;
}

istream& operator>>(istream& is,Datapoint &t)
{
  return is;
}


int main(void)
{
  char buffer[80];
  FILE *fid;
  DIR *dirp;

  if ((dirp = opendir(TTSDIR)) != NULL)
  {  
    for(struct dirent *d=readdir(dirp);d!=NULL;d=readdir(dirp))
    {
      vector<Datapoint> Points;
      if (d->d_attr & A_DIR) continue;
      sprintf(buffer,"%s/%s",TTSDIR,d->d_name);

      ifstream is(buffer);

      exit(0);
#if 0
      if ((fid = fopen(buffer,"rt")) != NULL)
      {
        while (fgets(buffer,sizeof(buffer),fid) != NULL)
        {
          int d,m,j,u,min;
          double koers;
          struct tm t;
          time_t ts;
          char *c;
          Datapoint *dp;

          sscanf(buffer,"%d/%d/%d",&d,&m,&j);

          if ((c = strchr(buffer+15,',')) != NULL)
            *c = '.';

          sscanf(buffer+9,"%d:%d %lf",&u,&min,&koers);
          memset(&t,0,sizeof(t));
          t.tm_min = min;
          t.tm_hour = u;
          t.tm_year = j;
          t.tm_mon = m;
          t.tm_mday = d;
          ts = mktime(&t);
          dp = new Datapoint(ts,koers);
          Points.add(*dp);    
        }
        fclose(fid);
      }

      double sumx = 0;
      double sumx2= 0;
      double sumy = 0;
      double sumy2= 0;
      double sumxy= 0;
      long cnt    = 0;
      int d0      = 32767;
      int d1      = 0;

      for (ArrayIterator j(Points);j.current() != NOOBJECT;j++)
      {
        Datapoint &dp = (Datapoint &)(j.current());

//        printf("%7.2f  %7.2f\n",dp.xv()/86400.0,dp.yv());
        if ((dp.xv() % 86400L) > 60000L)
        {
          cnt++;
          sumx += dp.xvd();
          sumx2 += dp.xvd() * dp.xvd();
          sumy += dp.yv();
          sumy2 += dp.yv() * dp.yv();
          sumxy += dp.xvd() * dp.yv();
          if (d0 > dp.xvd()) d0 = dp.xvd();
          if (d1 < dp.xvd()) d1 = dp.xvd();
        }
      }

      double b1 = (cnt * sumxy - sumx*sumy)/(cnt*sumx2-sumx*sumx);

      sumx = 0;
      sumx2= 0;
      sumy = 0;
      sumy2= 0;
      sumxy= 0;
      cnt = 0;
      for (ArrayIterator j(Points);j.current() != NOOBJECT;j++)
      {
        Datapoint &dp = (Datapoint &)(j.current());

        if ((dp.xv() % 86400L) > 60000L && dp.xvd() >= (d1-7))
        {
          cnt++;
          sumx += dp.xvd();
          sumx2 += dp.xvd() * dp.xvd();
          sumy += dp.yv();
          sumy2 += dp.yv() * dp.yv();
          sumxy += dp.xvd() * dp.yv();
        }
      }

      double b7 = (cnt * sumxy - sumx*sumy)/(cnt*sumx2-sumx*sumx);

      sumx = 0;
      sumx2= 0;
      sumy = 0;
      sumy2= 0;
      sumxy= 0;
      cnt = 0;
      for (ArrayIterator j(Points);j.current() != NOOBJECT;j++)
      {
        Datapoint &dp = (Datapoint &)(j.current());

        if ((dp.xv() % 86400L) > 60000L && dp.xvd() >= (d1-30))
        {
          cnt++;
          sumx += dp.xvd();
          sumx2 += dp.xvd() * dp.xvd();
          sumy += dp.yv();
          sumy2 += dp.yv() * dp.yv();
          sumxy += dp.xvd() * dp.yv();
        }
      }

      double b30= (cnt * sumxy - sumx*sumy)/(cnt*sumx2-sumx*sumx);

      sumx = 0;
      sumx2= 0;
      sumy = 0;
      sumy2= 0;
      sumxy= 0;
      cnt = 0;
      for (ArrayIterator j(Points);j.current() != NOOBJECT;j++)
      {
        Datapoint &dp = (Datapoint &)(j.current());

        if ((dp.xv() % 86400L) > 60000L && dp.xvd() >= (d1-91))
        {
          cnt++;
          sumx += dp.xvd();
          sumx2 += dp.xvd() * dp.xvd();
          sumy += dp.yv();
          sumy2 += dp.yv() * dp.yv();
          sumxy += dp.xvd() * dp.yv();
        }
      }

      double b91 = (cnt * sumxy - sumx*sumy)/(cnt*sumx2-sumx*sumx);

      sumx = 0;
      sumx2= 0;
      sumy = 0;
      sumy2= 0;
      sumxy= 0;
      cnt = 0;
      for (ArrayIterator j(Points);j.current() != NOOBJECT;j++)
      {
        Datapoint &dp = (Datapoint &)(j.current());

        if ((dp.xv() % 86400L) > 60000L && dp.xvd() >= (d1-365))
        {
          cnt++;
          sumx += dp.xvd();
          sumx2 += dp.xvd() * dp.xvd();
          sumy += dp.yv();
          sumy2 += dp.yv() * dp.yv();
          sumxy += dp.xvd() * dp.yv();
        }
      }

      double bj = (cnt * sumxy - sumx*sumy)/(cnt*sumx2-sumx*sumx);

      printf("\"%-20s\",",d->d_name);
      printf("%7.2f,",sumy/cnt);
      double fact=100*cnt/sumy;

      printf("%8.4f,%8.4f,%8.4f,%8.4f,%8.4f\n",fact*b1,fact*bj,fact*b91,fact*b30,fact*b7);
#endif
    }
  }
  return 0;
}
