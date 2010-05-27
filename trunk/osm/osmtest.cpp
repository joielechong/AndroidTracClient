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
#include <list>

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
    list<long> route;
    cout << "cost = " << cost<< endl;
    //    map.Astar(atol(start.c_str()),atol(eind.c_str())),string(""));
    map.Astar(46071276,603337746,string(""),route);
    list<long>::iterator rp;

    double dist = 0;
    long prevnode = 0;
    long prevway = 0;
    
    for(rp=route.begin(); rp != route.end(); rp++) {
      cout << *rp;
      vector<long> ways;
      getWays(*rp,ways);
      long w;
      if (ways.size() == 1)
        w=ways[0];
      else {
        w=getConnectingWay(prevnode,*rp);
      }
      
      if (w != 0) {
        cout << " way(" << w ;
        Way &w = _ways[w];
        try {
          string naam=w[name];
          cout << " " << naam;
        } catch (range_error &ex) {}
        try {
          string ref=w[ref];
          cout << " " << ref;
        } catch (range_error &ex) {}
        try {
          string highway=w[highway];
          cout << " " << highway;
        } catch (range_error &ex) {}
        try {
          string maxspeed=w[maxspeed];
          cout << " " << maxspeed;
        } catch (range_error &ex) {}
        cout << ")";
      }
      
      if (prevnode != 0) {
        double d = distance(prevnode,*rp);
        dist += d;
        double direction= direction(prevnode,*rp);
        cout << " distance: " << d << " (" << dist << ") direction: " << direction;        
      }
      
      vector<long> adminlist;
      for (vector<long>::iterator i=adminlist.begin();i != adminlist.end();i++) {
        Relation &r = _relations[*i];
        cout << " " << r[name] << "(" << r[admin_level] << ")";
      }
      cout << endl;
      prevnode = *rp;
      prevway = w;
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
