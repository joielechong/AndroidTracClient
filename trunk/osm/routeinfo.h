#ifndef ROUTEINFO_H
#define ROUTEINFO_H 1

#include <string>
#include <stdbool.h>

using namespace std;

class location {

 public:
  inline location() :node(false), coord(false), address(false) {country.clear();city.clear();street.clear();number.clear();postcode.clear();}

  bool node;
  long nodeid;

  bool coord;
  double lat,lon;

  bool address;
  string country,city,street,number,postcode;
};

class routeinfo {
 public: 
  string vehicle;
  location locations[2];
};

extern routeinfo inforoute;
void routelex_init(const char *s);
int yyparse(void);


#endif
