#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <dbf.h>
#include <typeinfo>
#include <exception>
#include <stdexcep>

#define AEX_REK 37

#define max(a,b) (a>b?a:b)
#define min(a,b) (a<b?a:b)

struct Koers
{
  long   datum;
  double koers;
  short  rek;
} *koers;

struct Aandeel
{
  long   datum;
  double aantal;
  double kosten;
  short  actie;
  short  rek;
  double koers;
  double bedrag;
} *aandeel;

struct Spaar
{
  long   datum;
  short  actie;
  short  rek;
  double bedrag;
} *spaar;

struct Rekening
{
  double koers;
  double aantal;
  double inleg;
  double waarde;
} *rekening;

struct Totaal
{
  long   datum;
  time_t t;
  double waarde;
  double inleg;
  double aex;
} *totaal;

int max_rek = -1;

int comp_spaar(const void *a,const void *b)
{
  long d1 = ((Spaar *)a)->datum;
  long d2 = ((Spaar *)b)->datum;

  if(d1 == d2)
    return 0;
  else if (d1 > d2)
    return 1;
  else
    return -1;
}

int comp_aandeel(const void *a,const void *b)
{
  long d1 = ((Aandeel *)a)->datum;
  long d2 = ((Aandeel *)b)->datum;

  if(d1 == d2)
    return 0;
  else if (d1 > d2)
    return 1;
  else
    return -1;
}

int comp_koers(const void *a,const void *b)
{
  long d1 = ((Koers *)a)->datum;
  long d2 = ((Koers *)b)->datum;

  if(d1 == d2)
    return 0;
  else if (d1 > d2)
    return 1;
  else
    return -1;
}

int lees_spaar(struct DBF *d)
{
  unsigned long record;
  int  i;
  int  datum=-1,rek=-1,actie=-1,bedrag=-1;
  char buffer[128];
  Spaar *s;

  spaar = new Spaar[d->records];

  for(i=0;i<d->num_fields;i++)
  {
    char *fn = d->fields_ptr[i].name;

    if (stricmp(fn,"datum") == 0)
      datum = i+1;
    else if (stricmp(fn,"bedrag") == 0)
      bedrag = i+1;
    else if (stricmp(fn,"rekening") == 0)
      rek    = i+1;
    else if (stricmp(fn,"actie") == 0)
      actie  = i+1;
  }

  if(datum == -1 || bedrag == -1 || rek == -1 || actie == -1)
    throw("Spaar database unvalid");

  for(record=1;record<=d->records;record++)
  {
    d_getrec(d,record);
    s = spaar + record-1;

    d_getfld(d,datum,buffer);
    s->datum = atol(buffer);

    d_getfld(d,bedrag,buffer);
    s->bedrag = atof(buffer);

    d_getfld(d,rek,buffer);
    s->rek = atoi(buffer);
    max_rek = max(max_rek,s->rek);

    d_getfld(d,actie,buffer);
    s->actie = atoi(buffer);
  }
  return 0;
}

int lees_aandeel(struct DBF *d)
{
  unsigned long record;
  int  i;
  int  datum=-1,aantal=-1,kosten=-1,rek=-1,actie=-1,koers=-1;
  char buffer[128];
  Aandeel *a;

  aandeel = new Aandeel[d->records];

  for(i=0;i<d->num_fields;i++)
  {
    char *fn = d->fields_ptr[i].name;

    if (stricmp(fn,"datum") == 0)
      datum = i+1;
    else if (stricmp(fn,"aantal") == 0)
      aantal = i+1;
    else if (stricmp(fn,"kosten") == 0)
      kosten = i+1;
    else if (stricmp(fn,"rekening") == 0)
      rek    = i+1;
    else if (stricmp(fn,"actie") == 0)
      actie  = i+1;
    else if (stricmp(fn,"koers") == 0)
      koers  = i+1;
  }

  if(datum == -1 || aantal == -1 || kosten == -1 || rek == -1 || actie == -1 || koers == -1)
    throw("Database aandeel unvalid");

  for(record=1;record<=d->records;record++)
  {
    d_getrec(d,record);
    a = aandeel + record-1;

    d_getfld(d,datum,buffer);
    a->datum = atol(buffer);

    d_getfld(d,aantal,buffer);
    a->aantal = atof(buffer);

    d_getfld(d,kosten,buffer);
    a->kosten = atof(buffer);

    d_getfld(d,rek,buffer);
    a->rek = atoi(buffer);
    max_rek = max(max_rek,a->rek);

    d_getfld(d,actie,buffer);
    a->actie = atoi(buffer);

    d_getfld(d,koers,buffer);
    a->koers = atof(buffer);

    a->bedrag = a->aantal * a->koers + a->kosten;
/*
    printf("%5ld: ",record);
    printf("%ld %10.4f %8.2f %3d %3d %10.4f %10.2f\n",a->datum,a->aantal,a->kosten,a->rek,a->actie,a->koers,a->bedrag);
*/
  }
  return 0;
}

int lees_koers(struct DBF *d)
{
  unsigned long record;
  int  i;
  int  datum=-1,rek=-1,koerz=-1;
  char buffer[128];
  Koers *k;

  koers = new Koers[d->records];

  for(i=0;i<d->num_fields;i++)
  {
    char *fn = d->fields_ptr[i].name;

    if (stricmp(fn,"datum") == 0)
      datum = i+1;
    else if (stricmp(fn,"rekening") == 0)
      rek    = i+1;
    else if (stricmp(fn,"koers") == 0)
      koerz  = i+1;
  }

  if(datum == -1 || rek == -1 || koerz == -1)
    throw("Database koers unvalid");

  for(record=1;record<=d->records;record++)
  {
    d_getrec(d,record);
    k = koers + record-1;

    d_getfld(d,datum,buffer);
    k->datum = atol(buffer);

    d_getfld(d,rek,buffer);
    k->rek = atoi(buffer);
    max_rek = max(max_rek,k->rek);

    d_getfld(d,koerz,buffer);
    k->koers = atof(buffer);
/*
    printf("%5ld: ",record);
    printf("%ld %3d %10.4f\n",k->datum,k->rek,k->koers);
*/
  }
  return 0;
}

int getdata(void)
{
  struct DBF dk,da,ds;
  long mrec,nrec,ia,ik,ma,mk,is,ms,ir,curdate;
  int i,actie,rek;

  tzset();
  strcpy(da.filename,"c:/dbase/geld/aandeel.dbf");
  if(d_open(&da) != 0)
    throw("Database aandeel does not exist");

  ma = mrec = da.records;
  lees_aandeel(&da);
  d_close(&da);

  strcpy(dk.filename,"c:/dbase/geld/koers.dbf");
  if(d_open(&dk) != 0)
    throw("Database koers does not exist");

  mk = dk.records;
  mrec += mk;
  lees_koers(&dk);
  d_close(&dk);

  strcpy(ds.filename,"c:/dbase/geld/spaar.dbf");
  if(d_open(&ds) != 0)
    throw("Database spaar does not exist");

  ms = ds.records;
  mrec += ms;
  lees_spaar(&ds);
  d_close(&ds);

  rekening = new Rekening[max_rek+1];
  nrec     = 0;
  totaal   = new Totaal[mrec];
  memset(rekening,0,(max_rek+1)*sizeof(Rekening));
  memset(totaal,0,mrec*sizeof(Totaal));

  qsort(aandeel,ma,sizeof(*aandeel),comp_aandeel);
  qsort(koers,mk,sizeof(*koers),comp_koers);
  qsort(spaar,ms,sizeof(*spaar),comp_spaar);

  ia = ik = is = 0;

  while (ia < ma || ik < mk || is < ms)
  {
    curdate = 0;
    if(ia < ma)
      curdate = aandeel[ia].datum;
    if (ik < mk)
    {
      if (curdate == 0)
        curdate = koers[ik].datum;
      else
        curdate = min(curdate,koers[ik].datum);
    }
    if (is < ms)
    {
      if(curdate == 0)
        curdate = spaar[is].datum;
      else
        curdate = min(curdate,spaar[is].datum);
    }

    while(aandeel[ia].datum == curdate && ia < ma)
    {
      actie = aandeel[ia].actie;
      rek   = aandeel[ia].rek;
      if(actie == 1 || actie == 2 || actie == 9 || actie == 14)
        rekening[rek].inleg += aandeel[ia].bedrag;
      rekening[rek].aantal  += aandeel[ia].aantal;
      rekening[rek].koers    = aandeel[ia].koers;
      ia++;
    }

    while(spaar[is].datum == curdate && is < ms)
    {
      actie = spaar[is].actie;
      rek   = spaar[is].rek;
      if(actie == 1 || actie == 2 || actie == 9 || actie == 14)
        rekening[rek].inleg += spaar[is].bedrag;
      rekening[rek].waarde += spaar[is].bedrag;
      is++;
    }

    while(koers[ik].datum == curdate && ik < mk)
    {
      rek = koers[ik].rek;
      rekening[rek].koers = koers[ik].koers;
      if (rek == AEX_REK)
        totaal[nrec].aex = koers[ik].koers;
      ik++;
    }
    totaal[nrec].datum = curdate;
    for(i=0;i<=max_rek;i++)
    {
      totaal[nrec].inleg += rekening[i].inleg;
      if(rekening[i].koers == 0)
        totaal[nrec].waarde += rekening[i].waarde;
      else
        totaal[nrec].waarde += (rekening[i].koers * rekening[i].aantal);
    } 
    ir = nrec;
    nrec++;
  }
  return nrec;
}

void writedata(FILE *fid,int nrec)
{
  int ir;
  time_t t1;
  struct tm t;
  double myindex,factor,oldaex=0;

  myindex=  100.0/4.5568;
  factor  = totaal[0].waarde/myindex;

  for(ir=0;ir<nrec;ir++)
  {
    memset(&t,0,sizeof(t));
    t.tm_isdst = 1;
    t.tm_hour = 2;
    t.tm_mday = totaal[ir].datum %100;
    t.tm_mon  = (totaal[ir].datum / 100) % 100 - 1;
    t.tm_year = totaal[ir].datum /10000L -1900;
    totaal[ir].t = t1 = mktime(&t)/86400L;

    if (totaal[ir].aex == 0 && oldaex != 0)
      totaal[ir].aex = oldaex;

    if (ir > 0) {
      myindex = (totaal[ir].waarde - totaal[ir].inleg + totaal[ir-1].inleg)/factor;
      factor = totaal[ir].waarde/myindex;
    }

    fprintf(fid,"%ld,%10.2f,%10.2f,%8.2f",
         totaal[ir].datum,
         totaal[ir].inleg,
	 totaal[ir].waarde,
	 myindex);
    if (totaal[ir].aex > 0)
      fprintf(fid,",%8.2f",totaal[ir].aex);

    fprintf(fid,"\n");
    oldaex = totaal[ir].aex;
  }
}

int main(void)
{
  int nrec;

  try {
    nrec=getdata();
    writedata(stdout,nrec);
    return 0;
  }
  catch (char *s) {
    fprintf(stderr,"%s\n",s);
    return 1;
  }
  catch (...) {
    fprintf(stderr,"Abnormal termination\n");
    return 1;
  }
}
