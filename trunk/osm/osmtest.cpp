#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <iostream>
#include <stdexcept>
#include <ArgumentParser.h>
#include <StringArgument.h>
#include <DoubleArgument.h>
#include <BooleanArgument.h>

using namespace std;

int main(int argc, char *argv[]) {
  Argument::StringArgument dbArg("-db","value",string("osm.sqlite"),"SQLite database name");
  Argument::DoubleArgument cacheArg("-cs","positive-integer",1000,"Cache size");
  Argument::DoubleArgument interpolArg("-intp","positive-integer",0,"Wegnummer voor interpolatie");
  Argument::BooleanArgument intpallArg("-intpall","Interpolate alles");
  Argument::StringArgument addrArg("-adres","value",string("invalid"),"SQLite where clause");
  Argument::ArgumentParser parser;
  parser.addArgument(dbArg);
  parser.addArgument(cacheArg);
  parser.addArgument(interpolArg);
  parser.addArgument(intpallArg);
  parser.addArgument(addrArg);
  list<string> extra = parser.parse(argc,argv);
  list<string>::iterator it;

  string dbname = dbArg.getValue();
  long cachesize = cacheArg.getValue();
  string query = addrArg.getValue();
  long interp = interpolArg.getValue();
  bool intpall = intpallArg.getValue();

  osm_db::database sql(dbname);
  sql.initializeFill();
  sql.initTemp();
  osm::Map map(&sql,cachesize);

  if (query != "invalid") {
    std::vector<string> naam;
    std::vector<int> level;
    cout <<query <<endl;
    map.findAdmin(query,naam,level);
    for (unsigned int i=0;i<naam.size();i++) 
      cout << naam[i] <<"("<<level[i]<<")"<<std::endl;
  }

  if (intpall)
    map.InterpolatedAddresses();

  if (interp != 0) {
    map.InterpolatedAddresses(interp);
    for (it=extra.begin();it!= extra.end();it++) {
      string intp=*it++;
      map.InterpolatedAddresses(atol(intp.c_str()));
    }
  }
  /*
  for (it=extra.begin();it!=extra.end();it++) {
    string latstr = *it++;
    string lonstr = *it++;
    string diffstr = *it;
    vector<double> lats;
    vector<double> lons;
    vector<long> ids;
    vector<double> distance;
    double lat,lon,diff;

    lat = atof(latstr.c_str());
    lon=atof(lonstr.c_str());
    diff=atof(diffstr.c_str());

    map.findNode(lat,lon,diff,ids,lats,lons,distance);
    for (unsigned int i = 0; i<lats.size();i++) {
      cout << ids[i]<<": "<<lats[i] <<","<<lons[i] << "  " << distance[i] << endl;
      osm::Node n=map.nodes()[ids[i]];
      cout << n << endl;
    }
  }
  */
  return 0;

}