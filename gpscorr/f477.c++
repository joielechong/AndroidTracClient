#include <iostream>
#include <fstream>
#include <vector>
#include <locale>
#include <limits>
#include <cmath>

using namespace std;

namespace
{
       const char DFormatExponentChar( 'D' );
       const char CppFormatExponentChar( 'E' );
}
// Helper function to display error message if
// passed in input stream is in a bad state
void CheckIStrm( std::istream const & in)
{
       if ( in.fail() )
       {
               if ( in.bad() )
               {
                       std::cerr << "Cin in fatal error state\n";
               }
               else
               {
                       std::cerr << "Cin operation failed\n";
               }
       }
}

// Sub-class template of the std::num_get facet class template
// used to parse numeric input character sequences
template < class charT, class InputIterator=std::istreambuf_iterator<charT> >
class DFormatNumGet : public std::num_get< charT, InputIterator >
{
public:
// Ensure we have base types available
       typedef typename std::num_get< charT, InputIterator >   base_type;
       using base_type::iter_type;
       using base_type::char_type;
       

// Replicate the base constructor.
// Pass on most of the work to the base constructor
// Get and save reference to current global num_get facet
       DFormatNumGet( size_t refs = 0 )
       : base_type( refs )
       , iPrevNumGet( std::use_facet<base_type>(std::locale()) )
       {
       }

protected:
// Override the base class virtual member function
// that handles parsing doubles type character sequences
       virtual typename DFormatNumGet<charT,InputIterator>::iter_type do_get( 
                                 typename DFormatNumGet<charT,InputIterator>::iter_type in
                               , typename DFormatNumGet<charT,InputIterator>::iter_type end
                               , std::ios_base & ib
                               , std::ios_base::iostate & err
                               , double & v
                               ) const;

private:
// Reference to global num_get facet in effect when
// instance created
       base_type const &     iPrevNumGet;
};

// Implementation of overriden double value parsing function
template < class charT, class InputIterator >
typename DFormatNumGet<charT,InputIterator>::iter_type
DFormatNumGet<charT,InputIterator>::do_get
( typename DFormatNumGet<charT,InputIterator>::iter_type in
, typename DFormatNumGet<charT,InputIterator>::iter_type end
, std::ios_base & ib
, std::ios_base::iostate & err
, double & v
) const
{
// Initially parse sequence using previous num_get facet
       typename DFormatNumGet<charT,InputIterator>::iter_type next( iPrevNumGet.get(in, end, ib, err, v) );

// Continue if there was no error
       if ( 0==err || std::ios::goodbit==err )
       {
       // If we are not at the end of the sequence and the
       // current character is the D format exponent character
       // then we have some work to do...
               if ( next != end && *next==DFormatExponentChar )
               {
                       ++next; // skip exponent character

               // Get the exponent value:
               // This is a integer value so we use the
               // stashed num_get facet reference to parse
               // the integer value as a long:
                       err = std::ios::goodbit;// may need to set to 0
                       long exponent;
                       next=iPrevNumGet.get(in,end,ib,err, exponent);

               // may need to compare against 0
                       if ( std::ios::goodbit==err )
                       {
                       // If all well check exponent value is in range
                       // Note: this is not a water tight error
                       //       checking strategy
                               if (  std::numeric_limits<double>::
                                           min_exponent10 <= exponent
                                  && std::numeric_limits<double>::
                                          max_exponent10 >= exponent
                                  )
                               {
                               // If the exponent value is in
                               // range apply it to the double value
                               // we already obtained:
                                       v *= pow(10.0, exponent);
                               }
                               else // exponent out of range
                               {    // set err to failbit
                                       err = std::ios::failbit;
                               }
                       }
               }
       }

       return next; // next character position in sequence
}

typedef vector<double> array;

#define MAXN (360)
#define S (MAXN+1)
#define L (((MAXN+1)*(MAXN+2))/2)

void hundu (double &undu,const long nmax,const array &p,const array &hc,const array &hs,
      const array &sinml,const array &cosml,const double &gr,const double &re,const double ang, 
      const array &cc,const array &cs,double &haco) {
  
  double gm = 0.3986004418e15;
  double ae = 6378137.0;
  
  double ar = ae/re;
  double arn = ar;
  double ac  = 0.0;
  double a   = 0.0;

  long k = 3;
  for (long n=2;n<=nmax;n++) {
    arn *= ar;
    k++;
    double sum = p[k] * hc[k];
    double sumc= p[k] * cc[k];
    for(long m=1;m<=n;m++) {
      k++;
      double tempc = cc[k]*cosml[m]+cs[k]*sinml[m];
      double temp  = hc[k]*cosml[m]+hs[k]*sinml[m];
      sumc += p[k]*tempc;
      sum += p[k]*temp;
    }
    ac += sumc;
    a  += sum*arn;
  }
  ac += cc[1]+p[2]*cc[2]+p[3]*(cc[3]*cosml[1]+cs[3]*sinml[1]);
  haco = ac / 100.0;
  undu = a *gm / (gr*re);
  undu += haco -0.53;
}

void dhcsin(ifstream &egm96,const long nmax,double &j2,double &j4,double &j6,array &hc,array &hs) {
  double j8,j10;

  j2=0.108262982131e-2;
  j4=-.237091120053e-5;
  j6=0.608346498882e-8;
  j8=-0.142681087920e-10;
  j10=0.121439275882e-13;
  
  long m = ((nmax+1)*(nmax+2))/2;

  while (egm96.good()) {
    long n;
    double c,s,ec,es;
    egm96 >> n >> m >> c  >> s >>ec >>es;
    if (n <= nmax) {
      n=(n*(n+1))/2+m+1;
      hc[n] = c;
      hs[n] = s;
    }
  }
  hc[4]  += j2/sqrt(5.0);
  hc[11] += j4/3.0;
  hc[22] += j6/sqrt(13.0);
  hc[37] += j8/sqrt(17.0);
  hc[56] += j10/sqrt(21.0);
}

void dscml(const double rlon,const long nmax,array &sinml,array &cosml){
  double a = sin(rlon);
  double b = cos(rlon);
  sinml[1] = a;
  cosml[1] = b;
  sinml[2] = 2.0*b*a;
  cosml[2] = 2.0*b*b-1.0;
  for(long m=3;m<=nmax;m++) {
    sinml[m] = 2.0*b*sinml[m-1]-sinml[m-2];
    cosml[m] = 2.0*b*cosml[m-1]-cosml[m-2];
  }
}

void radgra(const double flat, const double flon, const double ht,double &rlat,double &gr,double &re){
  double ae   = 6378137.0;
  double e2   = 0.00669437999013;
  double rad  = 57.29577951308232;
  double geqt = 9.7803253359;
  double k    = .00193185265246;

  double flatr = flat / rad;
  double flonr = flon / rad;
  double t1 = pow(sin(flatr),2);
  double n = ae/sqrt(1.0-e2*t1);
  double t2=(n+ht)*cos(flatr);
  double x = t2*cos(flonr);
  double y = t2*sin(flonr);
  double z=(n*(1.-e2)+ht)*sin(flatr);
  n = ae/sqrt(1.-e2*t1);
  re = sqrt(pow(x,2)+pow(y,2)+pow(z,2));
  rlat = atan(z/sqrt(pow(x,2)+pow(y,2)));
  gr = geqt*(1.0+k*t1)/sqrt(1.-e2*t1);
}

array drts(1301,0),dirt(1301,0);

void legfdn(const long m,const double theta, array &rleg, array &dleg,const long nmx, int &ir,array &rlnn,const long iflag) {
  
  long nmx1 = nmx+1;
  long nmx2p = 2*nmx+1;
  long m1 = m+1;
  long m2 = m+2;
  long m3 = m+3;
  if (ir != 1) {
    ir = 1;
    for (long n=1;n<=nmx2p;n++) {
      drts[n] = sqrt((double)n);
      dirt[n] = 1.0 / drts[n];
    }
  }
  double cothet = cos(theta);
  double sithet = sin(theta);
  double sithi;
  
  if (iflag != 1 && theta != 0.0) 
    sithi = 1.0/sithet;
  rlnn[1] = 1;
  rlnn[2] = sithet*drts[3];
  for (long n1=3;n1 <=m1;n1++) {
    long n = n1-1;
    long n2 = 2*n;
    rlnn[n1] = drts[n2+1]*dirt[n2]*sithet*rlnn[n1-1];
  }
  if (m <= 1) {
    if (m != 0) {
      rleg[2] = rlnn[2];
      rleg[3] = drts[5]*cothet*rleg[2];
    } else {
      rleg[1] = 1;
      rleg[2] = cothet*drts[3];
    }
  }
  rleg[m1] = rlnn[m1];
  if (m2 <= nmx1) {
    rleg[m2] = drts[m1*2+1]*cothet*rleg[m1];
    if (m3 <= nmx1) {
      for (long n1=m3;n1<=nmx1;n1++) {
        long n=n1-1;
        if (!(m==0 && n < 2  || m == 1  && n < 3)) {
          long n2 = 2*n;
          rleg[n1] = drts[n2+1]*dirt[n+m]*dirt[n-m]*(drts[n2-1]*cothet*rleg[n1-1]-drts[n+m-1]*drts[n-m-1]*dirt[n2-3]*rleg[n1-2]);
        }
      }
    }
  }
  if (iflag != 1) {
    if (sithet == 0.0) {
      cout << "*** legfdn does not compute derivatives at the poles ***************" << endl;
    } else {
      rlnn[1] = 0;
      double rln = rlnn[2];
      rlnn[2] = drts[3] * cothet;
      for (long n1=3; n1 <= m1; n1++) {
        long n=n1-1;
        long n2=2*n;
        double rln1 = rlnn[n1];
        rlnn[n1] = drts[n2+1]*dirt[n2]*(sithet*rlnn[n]+cothet*rln);
        rln=rln1;
      }
      dleg[m1] = rlnn[m1];
      if (m2 <= nmx1) {
        for ( long n1=m2;n1<= nmx1; n1++) {
          long n=n1-1;
          long n2 = 2*n;
          dleg[n1] = sithi*(n*rleg[n1]*cothet-drts[n-m]*drts[n+m]*drts[n2+1]*dirt[n2-1]*rleg[n]);
        }
      }
    }
  }
}

int main(int argc,char **argv) {
  array rleg(S+1,0),dleg(S+1,0),rlnn(S+1,0),sinml(S+1,0),cosml(S+1,0);
  array p(L+1,0),hc(L+1,0),hs(L+1,0),cc(L+1,0),cs(L+1,0);
  double rad=57.29577951308232;
  double ht=0;

  long m,n;
  
  ifstream corr("CORRCOEF", ifstream::in );
  ifstream egm96("EGM96", ifstream::in );
  ifstream input("INPUT.DAT", ifstream::in );
  ofstream output("OUTF477.DAT");

       // Create instance of our num_get facet for char types
       // The 1 passed to the constrcutor means we keep ownership of
       // of the facet for memory management purposes. If we pass 0
       // the the locale will try to clean it up for us on exit with
       // nasty result.
       DFormatNumGet<char> dFormatNumGet(1);

       // Create a locale copy of the global locale replacing the
       // num_get facter with our own
       locale dFormatLocale( std::locale(), &dFormatNumGet );

       // Apply this locale to std::cin
      corr.imbue( dFormatLocale );
      input.imbue( dFormatLocale );

  
  long nmax = 360;
  cout << "Maximum degree = " <<nmax << endl;
        
  while (corr.good()) {
    double t1,t2;
    corr >> n >> m >> t1 >> t2;
    long ig = (n*(n+1))/2+m+1;
    cc[ig]=t1;
    cs[ig]=t2;
  }
  
  double rj2,rj4,rj6;
  dhcsin(egm96,nmax,rj2,rj4,rj6,hc,hs);
  
  int iflag = 1;
  int ir = 0;
  long k=nmax+1;
  double flatl=91.0;
  double rlon,flon,rlat1,u,gr,ge,haco;
  
  while (input.good()) {
    double flat,rlat;
    input >> flat >> flon;
//    CheckIStrm( input );
    radgra(flat,flon,ht,rlat,gr,ge);
    if (flatl != flat) {
      rlat1=rlat;
      rlat = 1.5707963267948966 - rlat;
      flatl=flat;
      for (long j=1;j<=k;j++) {
        m = j - 1;
        legfdn(m,rlat,rleg,dleg,nmax,ir,rlnn,iflag);
        for (long i=j;i<=k;i++) {
          n = i-1;
          long loc=(n*(n+1))/2+m+1;
          p[loc]=rleg[i];
        }
      } 
    }
    rlon = flon/rad;
    dscml(rlon,nmax,sinml,cosml);
    hundu(u,nmax,p,hc,hs,sinml,cosml,gr,ge,rlat1,cc,cs,haco);
    output << flat << " " << flon << " " << u << endl;
  }
}
