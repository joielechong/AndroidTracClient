#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <mfl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <gd.h>
#include <gdfonts.h>
#include <gdfontl.h>
#include "results.h"


char *outfile = "scatchart.gif";
int color[10] = {0,1,2,3,4,5,6,7,8,9};
int kleur[10];


void displayResults(int width,int height,long n)
{
  gdImagePtr im=0;
  FILE *gifout;
  int  black,white,red,blue;
  double xmax,xspace,xfact,xorig,xend;
  double yspace,yfact,yorig,yend,ywid;
  long i;
  int  ndiv,xint,j;

  im = gdImageCreate(width,height);

  white =            gdImageColorAllocate(im,255,255,255);
  kleur[1] = red   = gdImageColorAllocate(im,255,0,0);
  kleur[2] = blue  = gdImageColorAllocate(im,0,0,255);
  kleur[3] =         gdImageColorAllocate(im,0,255,0);
  kleur[4] =         gdImageColorAllocate(im,255,255,0);
  kleur[5] =         gdImageColorAllocate(im,207,207,207);
  kleur[6] = black = gdImageColorAllocate(im,0,0,0);
  kleur[7] =         gdImageColorAllocate(im,207,123,37);
  kleur[8] =         gdImageColorAllocate(im,200,2,150);
  kleur[9] =         gdImageColorAllocate(im,64,111,123);

  gdImageColorTransparent(im,white);

// Determine length of x-axis and space needed left of y-axis

  xspace = 0;
  xmax   = 0;
  for(i=0;i<n;i++) {
    strip_cr(results[i].veld);
    if (strlen(results[i].veld) >xspace)
      xspace = strlen(results[i].veld);
    for (j=0;j<nfields;j++) {
      if (results[i].waarde[j] > xmax)
	xmax = results[i].waarde[j];
    }
  }
  xspace *= gdFontLarge->w;  /* in pixels now */

/* Try to make a better scale, only works for positive numbers */

  ndiv=0;
  xint=(int)(xmax+0.9);

  scale(&xint,&ndiv);
  xmax=xint;

/*
  xspace     :  Space reserved for Y annotations
  xfact      :  factor to convert to pixels in GIF
  xorig      :  X=0
  xend       :  Right end of x-xaxis
*/

  xfact  = (width - xspace -10 - 2*gdFontLarge->h)/xmax;
  xorig  = xspace + 5;
  xend   = xorig + xmax*xfact;

/*
  yfact      :  factor to convert to pixels
  yorig      :  Y=0
  yend       :  Top of Y-axis
  ywid       :  Height of a bar 
*/

  yspace = 3*gdFontLarge->h;
  yorig  = height - yspace;
  yend   = 10;
  yfact  = (yorig - yend) / n;
  ywid   = yfact / (nfields+1);

  for (i=0;i<=ndiv;i++) {
    int y1,y2,x,x1;
    char text[10];

    x = i*xint/ndiv;
    y2 = (int)(yorig);
    y1 = (int)yend+5;

    x1  = (int)(xorig + x*xfact);

    gdImageLine(im,x1,y1,x1,y2,black);
    y2 += 5;
    sprintf(text,"%d",x);
    x1  -= strlen(text)*gdFontLarge->w/2;
    gdImageString(im,gdFontLarge,x1,y2,(unsigned char *)text,black);
  }

  for(i=1;i<=nfields;i++) {
    int x1,x2,y1,y2;

    x1 = (i-1)*width/nfields;
    x2 = x1 + 10;
    y1 = (int)(yorig+gdFontLarge->h*2);
    y2 = (int)(y1 + gdFontLarge->h*0.5);

    gdImageFilledRectangle(im,x1,y1,x2,y2,kleur[color[i]]);
    gdImageString(im,gdFontSmall,x2+2,y1,(unsigned char *)fieldnames[i],black);
  }

  for (i=0;i<n;i++) {
    int x1,x2,y2,y3,y4,ys;
    double y1;

    y1 = yorig - i*yfact - ywid/2;
    y2 = (int)(y1 - yfact*0.5);
    ys = (int)(yorig - (i+0.5)*yfact-gdFontLarge->h/2);

    x1 = (int)xorig;

    gdImageString(im,gdFontLarge,x1-5-gdFontLarge->w*strlen(results[i].veld),ys,(unsigned char *)results[i].veld,black);
    for (j=0;j<nfields;j++) {
      x2 = (int)(x1 + results[i].waarde[j] * xfact);
      y3 = (int)(y1 - j*ywid);
      y4 = (int)(y3 - ywid);
      gdImageFilledRectangle(im,x1,y4,x2,y3,kleur[color[j+1]]);
    }
  }
  
  gdImageRectangle(im,(int)xorig,(int)yorig,(int)xend,(int)yend,black);

  gifout=fopen(outfile,"wb");
  gdImageGif(im,gifout);
  gdImageDestroy(im);
}

void usage(const char *caller)
{
  fprintf(stderr,"Usage: %s <dbase> <query> options\n",caller);
  fprintf(stderr,"Options: -w width              Specify width of image\n");
  fprintf(stderr,"         -h height             Specify height of image\n");
  fprintf(stderr,"         -c<n> colnr           Specify color of trace <n>\n");
  fprintf(stderr,"         -n fields             Specify maximumm number of fields\n");
  fprintf(stderr,"         -o outputfile         Specify outputfile, default 'barchart.gif'\n");
}

int main(int argc,char **argv)
{
  char *dbName;
  char *query;
  int  width;
  int  height;
  long n;
  int  i,j;

  if (argc < 3) {
    usage(argv[0]);
    return 1;
  }


  dbName = strdup(argv[1]);
  query = strdup(argv[2]);

// Set default values

  width   = 650;
  height  = 150;
  nfields = 1;

  for(i=3;i<argc;i++) {
    if (argv[i][0] == '-') {
      switch (argv[i][1]) {
      case 'w':  
	width=atol(argv[++i]);
        break;

      case 'h':
	height=atol(argv[++i]);
	break;

      case 'c':
	j = argv[i][2] - '0';
	if (j<1 || j>9) {
	  fprintf(stderr,"Colorindex must be in range 1-9\n");
	  return 1;
	}
	color[j] = atoi(argv[++i]);
	break;

      case 'n':
	nfields = atoi(argv[++i]);
	break;

      case 'o':
	outfile = strdup(argv[++i]);
	break;

      default:
	fprintf(stderr,"Unknown option '%s'\n",argv[i]);
	usage(argv[0]);
	break;
      }
    }
  }

  n=getResults(dbName,query);
  displayResults(width,height,n);

  return 0;
}
