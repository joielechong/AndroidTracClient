#include <graphics.h>
#include <stdlib.h>
#include <math.h>
#include <stdio.h>

#include "plotdb.h"

static int xleft,ybot;
static int xmax,ymax;
static float xlow,xhi;
static float ylow,yhi;

void axes(float *,float *,float *);

static int x_pos(float x)
{
  return (xleft + (xmax-xleft) * (x-xlow) / (xhi-xlow));
}

static int y_pos(float y)
{
  return (ymax - (ymax-ybot) * (y-ylow) / (yhi-ylow));
}

static void calc_axis(float *xlow,float *xhi,float *deltx,int *fmt)
{
  float d1;

  axes(xlow,xhi,deltx);
  *fmt=0;
  d1=*deltx;
  while (d1 < 1)
  {
    (*fmt)++;
    d1 *= 10;
  }
}

static void put_title(char *title)
{
  if (title != 0)
  {
    settextstyle(TITEL_FONT,HORIZ_DIR,USER_CHAR_SIZE);
    setusercharsize(1,1,1,1);
    ybot += 1.1*textheight(title);
    settextjustify(CENTER_TEXT,TOP_TEXT);
    outtextxy(xmax/2,1,title);
  }
}

void plot(struct data *b,int n,char *xtit,char *ytit,int zero,char *title)
{
  float delty,deltx;
  double x1,y1;
  int i;
  char buffer[80];
  char fmxbuf[10],fmybuf[10];
  struct textsettingstype ts;
  int fmt;

  xlow = b->x;
  xhi  = b[n-1].x;

  calc_axis(&xlow,&xhi,&deltx,&fmt);
  sprintf(fmxbuf,"%%1.%1df",fmt);

  yhi = ylow = (zero ? 0 : b->y);
  for (i=0;i<n;i++)
  {
    ylow = min(ylow,b[i].y);
    yhi  = max(yhi ,b[i].y);
  }

  if (ylow == yhi)
  {
    if (ylow == 0)
    {
      ylow = -1;
      yhi = 1;
    }
    else
    {
      ylow *= 0.9;
      yhi  *= 1.1;
    }
  }

  calc_axis(&ylow,&yhi,&delty,&fmt);
  sprintf(fmybuf," %%1.%1df ",fmt);

  xmax = getmaxx();
  ymax = getmaxy();
  ybot = xleft = 0;

  put_title(title);

  settextstyle(AS_FONT,HORIZ_DIR,USER_CHAR_SIZE);
  setusercharsize(1,1,1,1);
  sprintf(buffer,fmybuf,yhi);
  xleft = textwidth(buffer);
  sprintf(buffer,fmybuf,ylow);
  xleft = max(xleft,textwidth(buffer));

  sprintf(buffer,fmxbuf,xhi);
  ybot += textheight(buffer);
  ymax -= 3*textheight(buffer);

  xmax -= 1+textwidth(buffer)/2;
  xleft += textheight(buffer)+2;

  setlinestyle(DOTTED_LINE,0,1);
  settextjustify(RIGHT_TEXT,CENTER_TEXT);

  for(y1=ylow;y1<=yhi+0.1*delty;y1+=delty)
  {
    moveto(x_pos(xhi),y_pos(y1));
    lineto(x_pos(xlow)-5,y_pos(y1));
    if (y1 == floor(y1))
      sprintf(buffer,"%1.0f ",y1);
    else
      sprintf(buffer,fmybuf+1,y1);
    outtext(buffer);
  }
  moveto(0,ymax-(ymax-ybot)/2);
  gettextsettings(&ts);
  settextstyle(AS_FONT,VERT_DIR,USER_CHAR_SIZE);
  settextjustify(LEFT_TEXT,CENTER_TEXT);
  outtext(ytit);
  settextstyle(ts.font,ts.direction,ts.charsize);

  settextjustify(CENTER_TEXT,TOP_TEXT);

  for(x1=xlow;x1<=xhi+deltx*.1;x1+=deltx)
  {
    moveto(x_pos(x1),y_pos(yhi));
    lineto(x_pos(x1),y_pos(ylow)+5);
    if (x1 == floor(x1))
      sprintf(buffer,"%1.0f",x1);
    else
      sprintf(buffer,fmxbuf,x1);
    outtext(buffer);
  }
  moveto(xleft + (xmax-xleft)/2,ymax+textheight(buffer)+7);
  outtext(xtit);
  setlinestyle(SOLID_LINE,0,1);

  moveto(x_pos(xlow),y_pos(0));
  lineto(x_pos(xhi),y_pos(0));

  moveto(x_pos(b->x),y_pos(b->y));
  for (i=1;i<n;i++)
  {
    lineto(x_pos(b[i].x),y_pos(b[i-1].y));
    lineto(x_pos(b[i].x),y_pos(b[i].y));
  }
}
