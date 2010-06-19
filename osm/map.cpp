#include "osm.h"
#include "osm_db.h"
#include "grootcirkel.h"
#include <string>
#include <stdbool.h>
#include <iostream>
#include <map>
#include <vector>

namespace osm {
  using namespace std;


  Map::Map(osm_db::database *sql,const unsigned long cacheSize,string conffile) :
    _con(sql),
    _cacheSize(cacheSize),
    _vehicle(""),
    _nodes(_con,_cacheSize),
    _ways(_con,_cacheSize),
    _relations(_con,_cacheSize),
    _conffile(conffile) {
    
	load_conf();
  }
  
  bool Map::insideRelation(const long relationid,const long nodeid) {
    double lat,lon;
    vector<double> lats;
    vector<double> lons;


    lat = _nodes[nodeid].lat();
    lon = _nodes[nodeid].lon();

    _con->getRelCoords(relationid,lats,lons);

    int nvert = lats.size();
    bool c=false;
    //    cout << nvert << " nodes"<<endl;
    int j=nvert - 1;
    for (int i=0;i<nvert;j=i++) {
      if ((lons[i] > lon ) != (lons[j] > lon)) {
	double l2 = (lats[j]-lats[i])*(lon-lons[i])/(lons[j]-lons[i]) + lats[i];
	//	cout << "l2 = " << l2 << endl;
	if (lat < l2)
	  c = !c;
      }
      //      cout << i << " " << lon << " " << lons[j] << " " << lons[i] << " : " << lat << " " << lats[j] << " " << lats[i] << " = " << c << endl;
    }
    return c;
  }

  void Map::findAdmin(const string querystring,vector<string> &naam,vector<int> &level) {
    vector<long> nodeids;
    vector<double> nodelats;
    vector<double> nodelons;
    vector<long> relids;
    vector<string> relname;
    vector<int> admlevel;
    vector<long> admid;
    
    _con->findAddress(querystring,nodeids,nodelats,nodelons);
    //    cout << nodeids.size() << " nodes gevonden bij " << querystring << endl;
    for (unsigned int j=0;j<nodeids.size();j++) {
      cout << "Node: " << j << " " << nodelats[j] << "," << nodelons[j] << " " <<nodeids[j] << endl;
      _con->findAdmin(nodelats[j],nodelons[j],relids,relname,admlevel);
      cout << relids.size() << " admins gevonden" << endl;
      for (unsigned int i=0;i<relids.size();i++) {
	int found=-1;
	//	cout << relname[i] << endl;
	for (unsigned int k=0;found<0 && k<admid.size();k++) {
	  if (admid[k]==relids[i])
	    found=k;
	}
	if (found == -1) {
	  //	  cout << "  Admin: "<< i << " " << relids[i] << " " <<nodeids[j] << endl;
	  Node& n=_nodes[nodeids[j]];
	  if (_relations[relids[i]].isInside(_con,n.lat(),n.lon())) {
	    //	    cout << "    Omvattend"<<endl;
	    admid.push_back(relids[i]);
	    naam.push_back(relname[i]);
	    level.push_back(admlevel[i]);
	  }
	}
      }
    }
  }

  void Map::findLocation(const long id,vector<long> &adminlist) {
    vector<long> admins;
    
    adminlist.clear();
    _con->adminNode(id,admins);
    Node &n = _nodes[id];
    for (vector<long>::iterator i=admins.begin();i != admins.end();i++) {
      try {
	Relation &r = _relations[*i];
	if (r.isInside(_con,n.lat(),n.lon()))
	  adminlist.push_back(*i);
      } catch (runtime_error &ex) {}
    }
  }

  long Map::findCoor(const double lat,const double lon,const string vehicle) {

    vector<long> ways;
    vector<long> nodes;
    vector<double> distances;

    cout << lat << " " << lon << " " << vehicle << endl;
    _con->findCoor(lat,lon,ways,nodes,distances);

    for (unsigned int i = 0; i < ways.size(); i++) {
      cout << ways[i] << " " << nodes[i] << " " << distances[i] << " "+_ways[ways[i]]["highway"] << endl;
      if (_profiles[vehicle].is_allowed(_ways[ways[i]]["highway"]))
	return nodes[i];
    }
    throw runtime_error("Kan geen geschikte node vinden bij coordinaten");
  }
  
  long Map::findAddress(const string country,const string city,const string street,const string number,const string postcode,const string vehicle) {
    string query;

    // eerst de adressen database proberen of het adres er is.

    query.clear();
    if (! country.empty())
      query = "country='"+country+"'";
    if (!city.empty())
      query += " AND city='"+city+"'";
    if (!street.empty())
      query += " AND street='"+street+"'";
    if (!number.empty())
      query += " AND housenumber='"+number+"'";
    if (!postcode.empty())
      query += " AND postcode='"+postcode+"'";
    if (query.substr(0,5) == " AND ") 
      query.replace(0,5,"");
    
    vector<long> nodes,ways,intpolways,assocways;
    vector<double> distances;
    _con->ndAddress(query,ways,nodes,distances,intpolways,assocways);
    if (ways.size() > 0) {
      long nodefnd = 0;
      for (unsigned int i = 0; i < ways.size(); i++) {
	cout << ways[i] << " " << nodes[i] << " " << distances[i] << " "+_ways[ways[i]]["highway"] << endl;
	if (vehicle == "" || _profiles[vehicle].is_allowed(_ways[ways[i]]["highway"])) {
	  if (nodefnd == 0) 
	    nodefnd = nodes[i];
	  try {
	    if (_ways[ways[i]]["name"] == street)
	      return nodes[i];
	  } catch (range_error &ex) {}
	}
      }
      if (nodefnd != 0)
	return nodefnd;
    }

    //    throw runtime_error("Kan geen geschikte node vinden bij coordinaten");
    
    return 0;
  }
  
  double Map::distance(const Node &n1,const Node &n2) const {
    return grootcirkel(n1.lat(),n1.lon(),n2.lat(),n2.lon());
  }
  
  double Map::direction(const long n1, const long n2) {
    double dx = _nodes[n2].lon() - _nodes[n1].lon();
    double dy = _nodes[n2].lat() - _nodes[n1].lat();
    return 180.0*atan2(dx,dy)/PI;  // PI is gedefinieerd in grootcirkel.h
  }
}
