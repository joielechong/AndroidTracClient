#include <typeinfo>
#include <iostream.h>
#include <iomanip.h>
#include <fstream.h>
#ifdef __EMX__
#include <strng.h>
#else
#include <String.h>
#endif
#include <stdexcep>
#include <stl.h>
#include <time.h>

int jaar,maand,dag;

class Regel
{
public:
  Regel(char *s) : str(s) {}
  Regel() : str("") {}
  Regel(String s) : str(s) {} 

  String Fonds();
  double Koers();

private:
  String str;

  friend istream& operator>>(istream& in,Regel& r);
  friend ostream& operator<<(ostream& ou,const Regel& r);
  friend void fill_rc(Regel& r);
};

inline String Regel::Fonds()
{
  if(str[1] != ' ') return str(1,10);
  return str(8,10);
}

inline double Regel::Koers()
{ 
  String s=str(31,4); 
  String t=str(36,2);
  return atof(s)+atof(t)/100.0;
}

ostream& operator<<(ostream& ou,const Regel& r)
{
  ou << r.str;
//  ou << " (" << r.Fonds() << "," << r.Koers() << ")";
  ou << endl; 
  return ou;
}

istream& operator>>(istream& in, Regel& r)
{
  in.unsetf(ios::skipws);
  readline(in,r.str);
  return in;
}

class RekeningConv
{
public:
  RekeningConv() : _TtxNaam(""), _DbNaam(""), type(0), valid(0){}
  RekeningConv(char *t,char *d) : _TtxNaam(t), _DbNaam(d), type(0),valid(0) {}
  RekeningConv(char *t,char *d,double f) : _TtxNaam(t), _DbNaam(d), type(1), valid(0), arg1(f) {}
  int operator==(const RekeningConv& r) const {return _TtxNaam==r._TtxNaam;}
  int operator<(const RekeningConv& r) const  {return _TtxNaam<r._TtxNaam;}
  
  void SetTtxNaam(const String& t) {_TtxNaam=t;}
  void SetDbNaam(const String& d) {_DbNaam=d;}
  void SetKoers(const double k) {_Koers=k;valid=1;jr=jaar;mn=maand;dg=dag;}

  String TtxNaam() const {return _TtxNaam;}
  String DbNaam() const  {return _DbNaam;}
  double Koers() const;

private:
  String _TtxNaam;
  String _DbNaam;
  double _Koers;
  int    type;
  int    valid;
  double arg1;
  int    jr;
  int    mn;
  int    dg;

friend istream& operator>>(istream& in,RekeningConv& r);
friend ostream& operator<<(ostream& out,const RekeningConv& r);
};

ostream& operator<<(ostream& out,const RekeningConv& r)
{
  if (r.valid) {
    out << r.jr*10000L+r.mn*100+r.dg;
    out << setiosflags(ios::fixed) << setw(1) << setprecision(4);
    out << ',' << r.Koers();
    out << ',' << '"' << r.DbNaam() << '"';
    out << endl;
  }
  return out;
}

istream& operator>>(istream& in,RekeningConv& r)
{
  String s;

  readline(in,r._TtxNaam,',');
  readline(in,r._DbNaam,',');
  in >> r.type;
  readline(in,s);
  switch (r.type) {
  case 0:
    break;
  case 1:
//    cout << s << endl;
    r.arg1=0;
    break;
  }
//  cout << r._TtxNaam << '-' << r._DbNaam << '-' << r.type << endl;
  return in;
}

double RekeningConv::Koers() const
{
  switch (type) {
  case 0:
    return _Koers;
  case 1:
    if (_Koers < arg1)
      return 0.0;
    else
      return (_Koers-arg1)/arg1;
  default:
    throw(out_of_range("Invalid type"));
  }
}

vector<RekeningConv> rc;

template <class InputIterator>
void my_for_each(InputIterator first, InputIterator last, Regel& r) {
  String f=r.Fonds();

  while (first != last) {
    String ttx=first->TtxNaam();

 //   if ( ttx > f ) break;
    if ( ttx == f ) first->SetKoers(r.Koers());
    first++;
  }
}

void fill_rc(Regel& r)
{
  if (r.str.contains("EFFECTENBEURS")) {
    String s = r.str(24,2);
    dag      = atoi(s);
    s        = r.str(27,2);
    maand    = atoi(s);
    return;
  }
  if ((!r.str.contains(',')) || r.str.contains("LENING")) return;

  my_for_each(rc.begin(),rc.end(),r);
}

int main(void)
{
  time_t tijd;
  struct tm *t;

  time(&tijd);
  t = localtime(&tijd);
  jaar = t->tm_year + 1900;

  rc.push_back(*(new RekeningConv("ROBECO    ","ROBECO")));
  rc.push_back(*(new RekeningConv("ROLINCO   ","ROLINCO")));
  rc.push_back(*(new RekeningConv("ROLINCO   ","RZROLINCO")));
  rc.push_back(*(new RekeningConv("RODAMCO   ","RODAMCO")));
  rc.push_back(*(new RekeningConv("RG EMERG  ","RZEMF")));
  rc.push_back(*(new RekeningConv("RG AMERIC ","AMERICA")));
  rc.push_back(*(new RekeningConv("RG PACIFI ","PACIFIC")));
  rc.push_back(*(new RekeningConv("RG HOL BEZ","RZHB")));

  rc.push_back(*(new RekeningConv("POSTBK BF ","PBF")));
  rc.push_back(*(new RekeningConv("POSTBK AF ","PAF")));
  rc.push_back(*(new RekeningConv("POSTBK VF ","PVF")));
  rc.push_back(*(new RekeningConv("POSTBK OF ","POF")));

  rc.push_back(*(new RekeningConv("ASN AANDF ","ABF")));

  rc.push_back(*(new RekeningConv("OHRA AAND.","OAF")));
  rc.push_back(*(new RekeningConv("OHRA AAND ","OAF")));
  rc.push_back(*(new RekeningConv("OHRA OBLDF","ODF")));
  rc.push_back(*(new RekeningConv("OHRA TOTAL","OTF")));

  rc.push_back(*(new RekeningConv("AHOLD     ","AHOLD")));
  rc.push_back(*(new RekeningConv("KPN       ","KPN")));
  rc.push_back(*(new RekeningConv("ING       ","ING")));
  rc.push_back(*(new RekeningConv("DORDT PET ","DORDTPET")));
  rc.push_back(*(new RekeningConv("Adam Excha","AEX")));
  rc.push_back(*(new RekeningConv("Adam Excha","FISCALERT",737.46)));

  sort(rc.begin(),rc.end());

  vector<Regel> ttx;
  copy(istream_iterator<Regel>(cin), istream_iterator<Regel>(), inserter(ttx,ttx.end()));
  for_each(ttx.begin(),ttx.end(),fill_rc);

  copy(rc.begin(),rc.end(),ostream_iterator<RekeningConv>(cout));

  return 0;
}
