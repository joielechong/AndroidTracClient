#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include "cache.h"
#include <stdexcept>
#include <ArgumentParser.h>
#include <StringArgument.h>
#include <cmath>
 
int main(int argc, char *argv[]) {
  Argument::StringArgument dbArg("-db","value",string("newosm.sqlite"),"SQLite database name");
  Argument::ArgumentParser parser;
  parser.addArgument(dbArg);
  list<string> extra = parser.parse(argc,argv);
  list<string>::iterator it;

  string dbname = dbArg.getValue();

  osm_db::database sql(dbname);

  //  osm::Cache<osm::Node> nodes;


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

    sql.findNode(lat,lon,diff,ids,lats,lons,distance);
    for (unsigned int i = 0; i<lats.size();i++) 
      cout << ids[i]<<": "<<lats[i] <<","<<lons[i] << "  " << distance[i] << endl;
  }
  return 0;

}
