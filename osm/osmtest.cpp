#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <iostream>
#include <stdexcept>
#include <ArgumentParser.h>
#include <ArgumentGroup.h>
#include <StringArgument.h>
#include <DoubleArgument.h>
#include <BooleanArgument.h>
#include <ListArgument.h>

using namespace std;

int main(int argc, char *argv[]) {
  Argument::StringArgument dbArg("-db","value","SQLite database name",string("osm.sqlite"));
  Argument::DoubleArgument cacheArg("-cs","positive-integer","Cache size",1000L,false);
  Argument::BooleanArgument helpArg("-help","Help on usage");
  Argument::StringArgument addrArg("-adres","adres","SQLite where clause",false);
  Argument::StringArgument costArg("-cost","start,eind","Bereken kost",false);
  Argument::ListArgument extraArg("arg","Extra argumenten",false);
  Argument::ArgumentParser parser;

  parser.addArgument(dbArg);
  parser.addArgument(cacheArg);
  parser.addArgument(addrArg);
  parser.addArgument(costArg);
  parser.addArgument(helpArg);
  parser.addArgument(extraArg);
  parser.parse(argc,argv);

  if (!parser.isValid()) {
    parser.printErrors(cout);
    return 1;
  }

  string dbname = dbArg.getValue();
  long cachesize = cacheArg.getValue();
  bool help = helpArg.getValue();
  list<string> extra = extraArg.getValue();
  list<string>::iterator it;

  if (help) {
    parser.printUsage(cout);
    return 0;
  }

  osm_db::database sql(dbname);
  sql.initializeFill();
  sql.initTemp();
  osm::Map map(&sql,cachesize);

  if (addrArg.hasValue() ) {
    string query = addrArg.getValue();    
    std::vector<string> naam;
    std::vector<int> level;
    cout <<query <<endl;
    map.findAdmin(query,naam,level);
    for (unsigned int i=0;i<naam.size();i++) 
      cout << naam[i] <<"("<<level[i]<<")"<<std::endl;
  }

  if (costArg.hasValue()) {
    string cost = costArg.getValue();
    cout << "cost = " << cost<< endl;
    //    map.Astar(atol(start.c_str()),atol(eind.c_str())),"");
    map.Astar(46071276,603337746,"");
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
