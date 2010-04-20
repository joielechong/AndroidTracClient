#include "osm.h"
#include "osm_db.h"
#include <string>
#include <stdbool.h>

namespace osm {
  using namespace std;
  
  Map::Map(osm_db::database *sql,const unsigned long cacheSize) :_con(sql),
								 _cacheSize(cacheSize),
								 _nodes(_con,_cacheSize),
								 _ways(_con,_cacheSize),
								 _relations(_con,_cacheSize) {
  }
 
  bool Map::insideRelation(const long relationid,const long nodeid) {
    double lat,lon;
    std::vector<double> lats;
    std::vector<double> lons;


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

  void Map::findAdmin(const string querystring,std::vector<string> &naam,std::vector<int> &level) {
    std::vector<long> nodeids;
    std::vector<double> nodelats;
    std::vector<double> nodelons;
    std::vector<long> relids;
    std::vector<string> relname;
    std::vector<int> admlevel;
    std::vector<long> admid;
    
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
}
