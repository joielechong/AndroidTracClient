#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <stdexcept>
#include <ArgumentParser.h>
#include <ArgumentGroup.h>
#include <StringArgument.h>
#include <DoubleArgument.h>
#include <BooleanArgument.h>
#include <ListArgument.h>
#include <list>
#include "routeinfo.h"

using namespace std;

routeinfo inforoute;

int main(int argc, char *argv[]) {
  Argument::StringArgument dbArg("-db","value","SQLite database name",string("osm.sqlite"));
  Argument::DoubleArgument cacheArg("-cs","positive-integer","Cache size",10000L,false);
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
    routelex_init(cost.c_str());
    if (yyparse() != 0) 
      throw runtime_error("routestring niet juist: "+cost);

    string vehicle = inforoute.vehicle;
    if (vehicle=="no")
      vehicle = "";

    long start = 0;
    long eind = 0;

    if (inforoute.locations[0].node)
      start = inforoute.locations[0].nodeid;
    else if (inforoute.locations[0].coord)
      start = map.findCoor(inforoute.locations[0].lat,inforoute.locations[0].lon,vehicle);
    else if (inforoute.locations[0].address)
      start = map.findAddress(inforoute.locations[0].country,inforoute.locations[0].city,inforoute.locations[0].street,inforoute.locations[0].number,inforoute.locations[0].postcode,vehicle);
    else
      throw runtime_error("geen geldig startadres gegeven");

    if (inforoute.locations[1].node)
      eind = inforoute.locations[1].nodeid;
    else if (inforoute.locations[1].coord)
      eind = map.findCoor(inforoute.locations[1].lat,inforoute.locations[1].lon,vehicle);
    else if (inforoute.locations[1].address)
      eind = map.findAddress(inforoute.locations[1].country,inforoute.locations[1].city,inforoute.locations[1].street,inforoute.locations[1].number,inforoute.locations[1].postcode,vehicle);
    else
      throw runtime_error("geen geldig eindadres gegeven");

    cout << "start = "<< start << " eind = " << eind << endl;


    //    map.Astar(46071276,295961436,string("car"),route);
    map.Astar(start,eind,vehicle,route);
    
    double dist = 0;
    long prevnode = 0;
    long prevway = 0;
    
    ofstream gpx("route.gpx");
    gpx << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" << endl;
    gpx << "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\"  version=\"1.0\" creator=\"osmtest\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" << endl;
    gpx << "<trk>" <<endl << "<trkseg>" << endl;

    for(list<long>::iterator rp=route.begin(); rp != route.end(); rp++) {
      cout << "node = " << *rp << " prev = " << prevnode;
      gpx << "<trkpt lat=\"" << setiosflags(ios::fixed) << setprecision(6)<< map.nodes(*rp).lat() << "\" lon=\"" << map.nodes(*rp).lon() << "\" />" << endl;
      vector<long> ways;
      map.getWays(*rp,ways);
      long w = 0;
      if (ways.size() == 1)
        w=ways[0];
      else if (prevnode != 0)
        w=map.getConnectingWay(prevnode,*rp);

      if (w != 0) {
        cout << " way(" << w;
	osm::Way &way =map.ways()[w];
        try {
          string naam=way["name"];
          cout << " " << naam;
        } catch (range_error &ex) {}
        try {
          string ref=way["ref"];
          cout << " " << ref;
        } catch (range_error &ex) {}
        try {
          string highway=way["highway"];
          cout << " " << highway;
        } catch (range_error &ex) {}
        try {
          string maxspeed=way["maxspeed"];
          cout << " " << maxspeed;
        } catch (range_error &ex) {}
        cout << ")";
      }
      
      if (prevnode != 0) {
        double d = map.distance(prevnode,*rp);
        dist += d;
        double direction= map.direction(prevnode,*rp);
        cout << " distance: " << d << " (" << dist << ") direction: " << direction;
      }
      
      vector<long> adminlist;
      map.findLocation(*rp,adminlist);
      for (vector<long>::iterator i=adminlist.begin();i != adminlist.end();i++) {
	osm::Relation &r = map.relations()[*i];
        cout << " " << r["name"] << "(" << r["admin_level"] << ")";
      }
      cout << endl;
      prevnode = *rp;
      prevway = w;
    }
    gpx << "</trkseg>"<<endl<<"</trk>"<<endl;
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
