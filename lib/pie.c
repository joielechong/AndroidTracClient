#include <stdio.h>
#include <alloc.h>
#include <conio.h>
#include <dos.h>
#include <stdlib.h>
#include <string.h>
#include <mem.h>
#include <math.h>
#include <graphics.h>
#include <scale.h>
#include <mfllib.h>
#include "dbf.h"

#define FALSE (0)
#define TRUE (1)

#define TITEL_FONT COMPLEX_FONT
#define titel_font complex_font_far

#define AS_FONT SMALL_FONT
#define as_font small_font_far

int is_graph = FALSE;

struct data
{
  char  x[21];
  float y;
} 
*buf;

void *my_malloc(size_t size)
{
  void *m;

  m =malloc(size);
  if (m == 0)
  {
    if (is_graph)
      closegraph();
    fprintf(stderr,"Te weinig geheugen.\n");
    exit(1);
  }
  return m;
}

float datum(char *buff)
{
  int yr,mon,day;
  struct date dd;
  long dag,beg,end;
  struct time t = {
    0,0,0
  };

  sscanf(buff,"%4d%02d%02d",&yr,&mon,&day);
  dd.da_year = yr;
  dd.da_mon = mon;
  dd.da_day = day;
  dag = dostounix(&dd,&t);
  dd.da_mon = 1;
  dd.da_day = 1;;
  beg = dostounix(&dd,&t);
  dd.da_year++;
  end = dostounix(&dd,&t);
  return (yr+((float)(dag-beg)) / (end-beg));
}

int find_field(char *s,struct DBF *d)
{
  struct FIELD_RECORD *flds;
  int i;

  strupr(s);
  flds = d->fields_ptr;
  for(i=0;i<d->num_fields;i++)
    if (strcmp(s,flds[i].name) == 0)
      break;
  if (i >= d->num_fields)
    return -1;

  return i+1;
}

void iniplot(void)
{
  int detect=DETECT;
  char *tc;
  int mode,gr_err;

  tc = getenv("TURBOC");
  registerfarbgidriver(EGAVGA_driver_far);
  registerfarbgifont(as_font);
  registerfarbgifont(titel_font);

  initgraph(&detect,&mode,(tc == NULL ? "" : tc));
  gr_err = graphresult();
  if (gr_err !=  grOk)
  {
    fprintf(stderr,"Initgraph mislukt : %s\n",grapherrormsg(gr_err));
    exit(1);
  }
  is_graph = TRUE;

  setcolor(1);
  setbkcolor(getmaxcolor());
  settextstyle(TITEL_FONT,HORIZ_DIR,USER_CHAR_SIZE);
  settextjustify(CENTER_TEXT,TOP_TEXT);
  setusercharsize(1,1,1,1);
  outtextxy((getmaxx()+1)/2-1,getmaxy()/100,"PIEPLOT V1.0");
}

void pieplot(struct data *b,int n,char *title)
{
  double vol=0;
  int i;
  int mx,my,rd;
  double start,eind;
  int pat;
  char sbuf[30];
  char my_pat[][8] = {
    0x1f,0x3f,0x7f,0xff,0x01,0x03,0x07,0x0f,
    0x0f,0x07,0x03,0x01,0xff,0x7f,0x3f,0x1f,
    0xc3,0xc3,0x0c,0x0c,0x30,0x30,0xc3,0xc3,
    0xaa,0xaa,0x22,0x22,0xaa,0xaa,0x22,0x22,
    0x01,0x01,0x80,0x80,0x02,0x02,0x04,0x04,
    0xf0,0xf0,0x0f,0x0f,0xf0,0xf0,0x0f,0x0f,
    0x01,0x01,0x80,0x80,0x01,0x01,0x80,0x80,
    0x18,0x3c,0x7e,0xff,0xff,0x7e,0x3c,0x18,
  };
  int th;

  settextstyle(DEFAULT_FONT,HORIZ_DIR,1);
  settextjustify(LEFT_TEXT,BOTTOM_TEXT);

  th = textheight("H");

  for(i=0;i<n;i++)
  {
    vol += b[i].y;
    pat = i;
    if (pat > 11)
    {
      pat = 12;
      setfillpattern((char far *)my_pat[i-12],1);
    }
    setfillstyle(pat,1);
    bar(2,(2*i+0.5)*th,20,(2*i+1.5)*th);
    rectangle(2,(2*i+0.5)*th,20,(2*i+1.5)*th);
    sprintf(sbuf,"%s%9.2f",b[i].x,b[i].y);
    outtextxy(22,(2*i+1.5)*th,sbuf);
  }
  sprintf(sbuf,"%-20s%9.2f","Totaal",vol);
  outtextxy(22,(2*n+1.5)*th,sbuf);

  rd = getmaxy()/3;
  mx = getmaxx()-rd-1;
  my = (getmaxy()+1)/2-1;
  eind = 0;
  for(i=0;i<n;i++)
  {
    start = eind;
    eind = start + 360.0 * b[i].y/vol;
    pat = i;
    if (pat > 11)
    {
      pat = 12;
      setfillpattern((char far *)my_pat[i-12],1);
    }
    setfillstyle(pat,1);
    pieslice(mx,my,(int)start,(int)eind,rd);
  }
}

void make_filename(char *f,char *e)
{
  char *p,*s;

  s = strrchr(f,'\\');
  if (s == NULL)
    s = f;
  else
    s++;
  p = strrchr(s,'.');
  if (p == 0)
  {
    strcat(strcat(f,"."),e);
  }
}

void print_plot_help(void)
{
  fprintf(stderr,"Aanroep: PIEDB [opties] <dbase.file>\n");
}

void main(int argc,char **argv)
{
  int errornum;
  long rec;
  struct DBF *d;
  char *buff;
  size_t i,nrec;
  int veld1,veld2;
  char veld_x[13]="",veld_y[13]="";
  char xtit[12],ytit[12];
  char *title = 0;

  if(argc < 2)
  {
    print_plot_help();
    exit(1);
  }

  d = (struct DBF *)my_malloc(sizeof(struct DBF));		/* allocate space for DBF */
  d->filename[0] = 0;

  for(i=1;i<argc;i++)
  {
    strupr(argv[i]);
    if (argv[i][0] == '-')
    {
      switch (argv[i][1])
      {
      case 'X':
        strcpy(veld_x,argv[++i]);
        break;

      case 'Y':
        strcpy(veld_y,argv[++i]);
        break;

      case 'T':
        title = argv[++i];
        break;

otherwise:
        fprintf(stderr,"Onbekende optie %s\n",argv[i]);
        exit(1);
      }
    }
    else
    {
      if (d->filename[0] == 0)
        strcpy(d->filename,argv[i]);
      else
      {
        fprintf(stderr,"Meer dan 1 filenaam opgegeven : %s\n",argv[i]);
        exit(1);
      }
    }
  }

  make_filename(d->filename,"DBF");

  if((errornum = d_open(d))!=0)										/* open file							 */
  {
    fprintf(stderr,"Fout tijdens openen van file: ");
    switch (errornum)
    {
    case OUT_OF_MEM:
      fprintf(stderr,"Niet genoeg geheugen\n");
      break;
    case NO_FILE:
      fprintf(stderr,"Open van file %s mislukt.\n",d->filename);
      break;
    case BAD_FORMAT:
      fprintf(stderr,"File %s is geen dBASE III file.\n",d->filename);
      break;
    }
    exit(1);
  }
  if (d->records == 0)
  {
    fprintf(stderr,"Database is leeg\n");
    exit(1);
  }

  if (veld_x[0] != 0)
  {
    if ((veld1= find_field(veld_x,d)) == -1)
    {
      fprintf(stderr,"Fout met veld voor %c-as : %s\n",'x',veld_x);
      exit(1);
    }
  }
  else
    veld1 = 1;

  if (veld_y[0] != 0)
  {
    if ((veld2 = find_field(veld_y,d)) == -1)
    {
      fprintf(stderr,"Fout met veld voor %c-as : %s\n",'y',veld_y);
      exit(1);
    }
  }
  else
    veld2 = 2;

  buf = my_malloc((size_t)d->records*sizeof(*buf));

  iniplot();

  settextstyle(AS_FONT,HORIZ_DIR,USER_CHAR_SIZE);
  setusercharsize(2,1,2,1);
  buff = my_malloc(d->record_length+1);
  nieuw_balk("Data wordt ingelezen",d->records);

  nrec = 0;
  for(rec=1;rec<=d->records;rec++)
  {
    struct data r0;

    set_balk(rec);
    d_getrec(d,(long)rec);
    d_getfld(d,veld1,r0.x);
    d_getfld(d,veld2,buff);
    r0.y = atof(buff);
    if (r0.y > 0)
      binsert(&r0,buf,&nrec,sizeof(struct data),strcmp);
  }
  sluit_balk();
  free(buff);

  strcpy(xtit,(d->fields_ptr)[veld1-1].name);
  strcpy(ytit,(d->fields_ptr)[veld2-1].name);
  d_close(d);
  free(d);

  clearviewport();
  pieplot(buf,nrec,title);
  getch();
  closegraph();
}
