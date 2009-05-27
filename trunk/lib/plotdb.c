#include <conio.h>
#include <stdlib.h>
#include <dos.h>
#include <string.h>
#include <graphics.h>
#include <mfllib.h>

#include "dbf.h"
#include "plotdb.h"

void plot(struct data *b,int n,char *xtit,char *ytit,int zero,char *title);

struct data *buf;

void *my_malloc(size_t size)
{
  void *m;

  m =malloc(size);
  if (m == 0)
  {
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
  return (yr + ((float)(dag-beg)) / (end-beg));
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
  atexit(closegraph);

  setcolor(1);
  setbkcolor(getmaxcolor());
  settextstyle(TITEL_FONT,HORIZ_DIR,USER_CHAR_SIZE);
  settextjustify(CENTER_TEXT,TOP_TEXT);
  setusercharsize(1,1,1,1);
  outtextxy((getmaxx()+1)/2-1,getmaxy()/100,"PLOTDB V1.0");
}

int compar(struct data *d1,struct data *d2)
{
  if (d1->x < d2->x)
    return -1;
  else if (d1->x == d2->x)
    return 0;
  else
    return 1;
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
    strcat(strcat(f,"."),e);
}

void print_plot_help(void)
{
  extern char far plot_help();
  char far *s,*t;

  t = (char far *)plot_help;
  s =  strstr(t,"<eof>");
  if (s == 0)
    fprintf(stderr,"Aanroep: PLOTDB [opties] <dbase.file>\n");
  else
  {
    *s = 0;
    fputs(t,stderr);
  }
}

float waarde(struct DBF *d,int fld)
{
  static char *buff=0;

  if (buff == 0)
    buff = my_malloc(d->record_length+1);

  return (d_getfld(d,fld,buff) == 'D' ? datum(buff) : atof(buff));
}

void main(int argc,char **argv)
{
  int errornum;
  long rec;
  struct DBF *d;
  size_t i,nrec;
  int veld1,veld2,som=FALSE,zero=FALSE;
  char veld_x[13]="",veld_y[13]="";
  char xtit[12],ytit[12];
  char *title = 0;

  if(argc < 2)
  {
    print_plot_help();
    exit(1);
  }

  d = (struct DBF *)my_malloc(sizeof(struct DBF));    /* allocate space for DBF */
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

      case 'S':
        som = TRUE;
        break;

      case 'Z':
        zero = TRUE;
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

  if((errornum = d_open(d))!=0)                    /* open file               */
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
  nieuw_balk("Data wordt ingelezen",d->records);

  nrec = 0;
  for(rec=1;rec<=d->records;rec++)
  {
    struct data *r,r0;

    set_balk(rec);
    d_getrec(d,(long)rec);
    r0.x = waarde(d,veld1);
    r0.y = 0;
    r = binsert(&r0,buf,&nrec,sizeof(struct data),compar);
    r->y += waarde(d,veld2);
  }
  sluit_balk();

  strcpy(xtit,(d->fields_ptr)[veld1-1].name);
  strcpy(ytit,(d->fields_ptr)[veld2-1].name);
  d_close(d);
  free(d);

  if (nrec < 2)
  {
    fprintf(stderr,"Onvoldoende datapunten\n");
    exit(1);
  }

  if (som)
  {
    for (i=1;i<nrec;i++)
      buf[i].y += buf[i-1].y;
  }

  clearviewport();
  plot(buf,nrec,xtit,ytit,zero,title);
  getch();
}
