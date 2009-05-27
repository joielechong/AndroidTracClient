#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dbf.h>
#include <iostream.h>
#include <iomanip.h>
#include <stl.h>
#include "dbfile.h"

#define AEX_REK 37

// myindex == 100 op 1/1/89 (zonder giro)
// #define MYINDEX (100.0/4.5568) 
// myindex=118.73 op 1/1/91 (correctie voor giro erbij
#define MYINDEX (100.0 * 118.73 / 90.67)
#define euroConv(datum) (datum > 19990000 ? 2.20371 : 1.0)

short max_rek = -1;


class RekTempl
{
public:
  RekTempl() : _datum(0), _rek(0), _actie(0) {}

  void setDatum(const long d)  {_datum = d;}
  void setDatum(const char *b) {_datum = atol(b);}
  void setRek(const short r)   {_rek = r; max_rek=max(max_rek,_rek);}
  void setRek(const char *b)   {setRek(atoi(b));}
  void setActie(const short r) {_actie = r;}
  void setActie(const char *b) {_actie = atoi(b);}

  int operator==(const RekTempl& r) const {return _datum == r._datum;}
  int operator<(const RekTempl& r) const  {return _datum <  r._datum;}

  long  datum(void) const {return _datum;}
  short rek(void) const   {return _rek;}
  short actie(void) const {return _actie;}

private:
  long  _datum;
  short _rek;
  short _actie;
};

class Koers : public RekTempl
{
public:
  Koers() : RekTempl(), _koers(0) {}

  void setKoers(const double k) {_koers = k;}
  void setKoers(const char *b)  {_koers = atof(b);}

  double koers(void) const {return _koers;}

  void field_fill(const char *field,const char *value);

private:
  double _koers;
};

void Koers::field_fill(const char *field,const char *value) {
  if (strcasecmp(field,"datum") == 0) 
    setDatum(value);
  else if (strcasecmp(field,"rekening") == 0)
    setRek(value);
  else if (strcasecmp(field,"koers") == 0)
    setKoers(value);
}

inline ostream& operator<<(ostream& o,const Koers& k) {
  o << setiosflags(ios::fixed) << setprecision(4) << k.datum() << ',';
  o << setw(3) << k.rek() << ',';
  o << setw(10)  << k.koers() << endl;
  return o;
}

class Aandeel : public RekTempl
{
public:
  Aandeel() : RekTempl(), _aantal(0), _kosten(0),_koers(0) {}

  void setAantal(const double r) {_aantal = r;}
  void setAantal(const char *b)  {_aantal = atof(b);}
  void setKosten(const double r) {_kosten = r;}
  void setKosten(const char *b)  {_kosten = atof(b);}
  void setKoers(const double k)  {_koers = k;}
  void setKoers(const char *b)   {_koers = atof(b);}

  double aantal(void) const {return _aantal;}
  double kosten(void) const {return _kosten;}
  double bedrag(void) const {return (_aantal*_koers+_kosten)*euroConv(datum());}
  double koers(void) const  {return _koers;}

  void field_fill(const char *field,const char *value);

private:
  double _aantal;
  double _kosten;
  double _koers;


};

void Aandeel::field_fill(const char *field,const char *value) {
  if (strcasecmp(field,"datum") == 0) 
    setDatum(value);
  else if (strcasecmp(field,"rekening") == 0)
    setRek(value);
  else if (strcasecmp(field,"koers") == 0)
    setKoers(value);
  else if (strcasecmp(field,"actie") == 0)
    setActie(value);
  else if (strcasecmp(field,"aantal") == 0)
    setAantal(value);
  else if (strcasecmp(field,"kosten") == 0)
    setKosten(value);
}

class Spaar : public RekTempl
{
public:
  Spaar () : RekTempl(), _bedrag(0) {}

  void setBedrag(const double b)  {_bedrag = b;}
  void setBedrag(const char *b)   {_bedrag = atof(b);}

  double bedrag(void) const {return _bedrag;}

  void field_fill(const char *field,const char *value);

private:
  double _bedrag;
};

void Spaar::field_fill(const char *field,const char *value) {
  if (strcasecmp(field,"datum") == 0) 
    setDatum(value);
  else if (strcasecmp(field,"rekening") == 0)
    setRek(value);
  else if (strcasecmp(field,"bedrag") == 0)
    setBedrag(value);
  else if (strcasecmp(field,"actie") == 0)
    setActie(value);
}

class Giro : public RekTempl
{
public:
  Giro() : RekTempl(), _bedrag(0) {}

  void setBedrag(const double b)  {_bedrag = b;}
  void setBedrag(const char *b)   {_bedrag = atof(b);}

  double bedrag(void) const {return _bedrag;}

  void field_fill(const char *field,const char *value);

private:
  double _bedrag;
};

void Giro::field_fill(const char *field,const char *value) {
  if (strcasecmp(field,"datum") == 0) 
    setDatum(value);
  else if (strcasecmp(field,"bedrag") == 0)
    setBedrag(value);
}

template <class T>
void lees_db(char *naam,vector<T>& arr)
{
  DBFile<T> d(naam);

  for(unsigned long record=1;record<=d.records();record++)
  {
    T *k = new T();
    *k = d[record];
    arr.push_back(*k);
  }
  sort(arr.begin(),arr.end());
}

class Rekening
{
public:
  Rekening () : koers(0), aantal(0), inleg(0), waarde(0) {}

  double koers;
  double aantal;
  double inleg;
  double waarde;
};

class Totaal
{
public:
  Totaal(long d) : datum(d), waarde(0), inleg(0), aex(0), myindex(0) {}

  long   datum;
  double waarde;
  double inleg;
  double aex;
  double myindex;
};

ostream& operator<<(ostream& o,const Totaal& t) {
  o << setiosflags(ios::fixed) << setprecision(2) << t.datum << ',';
  o << setw(10) << t.inleg << ',';
  o << setw(10) << t.waarde << ',';
  o << setw(8)  << t.myindex;
  if (t.aex >0)
    o << ',' << setw(8) << t.aex;
  o << endl;
  return o;
}

void getdata(vector<Totaal>& totaal)
{
  vector<Aandeel> aandeel;
  lees_db("/p233/c/dbase/geld/aandeel.dbf",aandeel);

  vector<Koers> koers;
  lees_db("/p233/c/dbase/geld/koers.dbf",koers);

  vector<Spaar> spaar;
  lees_db("/p233/c/dbase/geld/spaar.dbf",spaar);

  vector<Giro> giro;
  lees_db("/p233/c/dbase/geld/giro.dbf",giro);

//  if (max_rek <1) throw out_of_range("No accounts found");
  if (max_rek <1) throw "No accounts found";

  Rekening rekening[max_rek+1];

  double factor   = 0;
  double oldaex   = 0;
  double oldinleg = 0;
  double girosal  = 0;
  int first       = 1;
  size_t is       = 0;
  size_t ia       = 0;
  size_t ik       = 0;
  size_t ig       = 0;

  while (ia < aandeel.size() || ik < koers.size() || is < spaar.size() || ig < giro.size())
  {
    long curdate;
    int i,actie,rek;

    curdate = 0;

    if(ia < aandeel.size())
      curdate = aandeel[ia].datum();

    if (ik < koers.size())
    {
      if (curdate == 0)
        curdate = koers[ik].datum();
      else
        curdate = min(curdate,koers[ik].datum());
    }

    if (is < spaar.size())
    {
      if(curdate == 0)
        curdate = spaar[is].datum();
      else
        curdate = min(curdate,spaar[is].datum());
    }

    if(ig < giro.size()) {
      if (curdate == 0)
	curdate = giro[ig].datum();
      else
	curdate = min(curdate,giro[ig].datum());
    }

    while(aandeel[ia].datum() == curdate && ia < aandeel.size())
    {
      actie = aandeel[ia].actie();
      rek   = aandeel[ia].rek();
      if(actie == 1 || actie == 2 || actie == 9 || actie == 14)
        rekening[rek].inleg += aandeel[ia].bedrag();
      rekening[rek].aantal  += aandeel[ia].aantal();
      rekening[rek].koers    = aandeel[ia].koers();
      ia++;
    }

    while(spaar[is].datum() == curdate && is < spaar.size())
    {
      actie = spaar[is].actie();
      rek   = spaar[is].rek();
      if(actie == 1 || actie == 2 || actie == 9 || actie == 14)
        rekening[rek].inleg += spaar[is].bedrag();
      rekening[rek].waarde += spaar[is].bedrag();
      is++;
    }

    Totaal *t = new Totaal(curdate);
 
    while(koers[ik].datum() == curdate && ik < koers.size())
    {
      rek = koers[ik].rek();
      rekening[rek].koers = koers[ik].koers();
      if (rek == AEX_REK)
        t->aex = koers[ik].koers();
      ik++;
    }

    for(i=0;i<=max_rek;i++)
    {
      t->inleg += rekening[i].inleg;
      if(rekening[i].koers == 0)
        t->waarde += rekening[i].waarde;
      else
        t->waarde += (rekening[i].koers * rekening[i].aantal * euroConv(t->datum));
    } 

    while(giro[ig].datum() == curdate && ig < giro.size()) {
      girosal += giro[ig].bedrag();
      ig++;
    }

    t->inleg  += girosal;
    t->waarde += girosal;
    
    if (t->aex == 0 && oldaex != 0)
      t->aex = oldaex;

    if (first) {
      first      = 0;
      t->myindex = MYINDEX;
    }
    else
      t->myindex = (t->waarde - t->inleg + oldinleg)/factor;
    factor     = t->waarde/t->myindex;
    oldaex     = t->aex;
    oldinleg   = t->inleg;
    totaal.push_back(*t);
  }
}

int main(void)
{
  try {
    vector<Totaal> totaal;

    getdata(totaal);
    copy(totaal.begin(),totaal.end(),ostream_iterator<Totaal>(cout));

    return 0;
  }
//  catch (logic_error& e) {
////    cerr << "Typeinfo : " << typeid(e).name() << endl;
//    cerr << e.what() << endl;
//    return 1;
//  }
  catch (char *s) {
    cerr << s <<endl;
    return 1;
  }
}
