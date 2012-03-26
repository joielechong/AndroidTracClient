#include "osm.h"
#include "osm_sql3db.h"
#include <cstdlib>
#include <string>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <stdexcept>
#include <list>
#include <boost/program_options.hpp>
#include "routeinfo.h"

namespace po = boost::program_options;

using namespace std;

routeinfo inforoute;

int main(int argc, char *argv[]) {
  string dbname;
  string query;
  string cost;
  long cachesize;

  try {
    po::options_description desc("Aanroep osmtest [opties]\nGeldige opties");
    desc.add_options()
      ("db", po::value<string>(&dbname)->default_value("osm.sqlite"), "SQLite database name")
      ("cost", po::value<string>(&cost), "Bereken kost")
      ("cs",po::value<long>(&cachesize)->default_value(10000L),"Cache size")
      ("adres",po::value<string>(&query),"SQLite where clause")
      ("block",po::value< vector<string> >(),"Block list")
      ("ignoreextra","Extracost altijd op 0")
      ("help","Help om usage");

    po::variables_map vm;
    po::store(po::command_line_parser(argc, argv).
	  options(desc).run(), vm);
    po::notify(vm);
    
    if (vm.count("help")) {
      cout << desc << endl <<endl;
      return 0;
    }
    bool ignoreExtra = (vm.count("ignoreextra")>0);

    osm_db::sql3database sql(dbname);

    sql.initializeFill();
    sql.initTemp();
    osm::Map map(&sql,cachesize);
    
    if (vm.count("adres") ) {
      std::vector<string> naam;
      std::vector<int> level;
      cout <<query <<endl;
      map.findAdmin(query,naam,level);
      for (unsigned int i=0;i<naam.size();i++) 
	cout << naam[i] <<"("<<level[i]<<")"<<std::endl;
    }
    
    if (vm.count("cost")) {
      list<long> route;
      
      cout << "cost = " << cost<< endl;
      vector<string> blocklist;
      if (vm.count("block")) {
        blocklist = vm["block"].as< vector<string> >();
      }
      vector<string>::iterator it;

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
      

      map.Astar(start,eind,vehicle,route,ignoreExtra);
      
      cout << "Astar is klaar" << endl;
      
      double dist = 0;
      long prevnode = 0;
      long ppnode = 0;
      long prevway = 0;
      double kost = 0.0;
      
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
	  double c = map.cost(prevnode,*rp,ppnode,ignoreExtra);
	  kost += c;
	  cout << " cost: " << c << " (" << kost << ")";
	}
	
	vector<long> adminlist;
	map.findLocation(*rp,adminlist);
	for (vector<long>::iterator i=adminlist.begin();i != adminlist.end();i++) {
	  osm::Relation &r = map.relations()[*i];
	  cout << " " << r["name"] << "(" << r["admin_level"] << ")";
	}
	cout << endl;
	ppnode = prevnode;
	prevnode = *rp;
	prevway = w;
      }
      gpx << "</trkseg>"<<endl<<"</trk>"<<endl<<"</gpx>"<<endl;
      cout << map.costcounter() << " verschillende wegen geprobeerd" << endl;
      cout << "Gevonden weg bestaat uit "<< route.size() << " elementen"<<endl;
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
    
  } catch (exception& e) {
    cerr << "error: " << e.what() << endl;
    return 1;
  } catch(...) {
    cerr << "Exception of unknown type!" << endl;
  }
  
  return 0;

}
