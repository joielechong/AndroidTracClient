#include "osm.h"
#include <sstream>
#include <stdexcept>
#include <cstdlib>

namespace osm {
  using namespace std;

  void Map::InterpolatedAddresses() {
    vector<long> ids;
    _con->getInterpolationWays(ids);
    for (unsigned int i=0;i<ids.size();i++) {
      try {
	InterpolatedAddresses(_ways[ids[i]]);
      } catch (const range_error &ex) {
	cout << "Id : " << ids[i] << " : " <<ex.what() << endl;
      }
    }
  }

  void Map::InterpolatedAddresses(osm::Way &w) {
    string method = w["addr:interpolation"];
    int step;

    if (method =="even" || method == "odd") 
      step = 2;
    else if (method == "all") 
      step = 1;
    else 
      throw range_error("(Nog) niet ondersteunde methode "+method);
    
    vector<long> ids;
    vector<double> lats;
    vector <double> lons;
    vector<string> countries;
    vector<string> cities;
    vector<string> streets;
    vector<string> housenumbers;
    vector<string> postcodes;

    _con->findHouses(w.id(),ids,lats,lons,countries,cities,streets,housenumbers,postcodes);

    if (ids.size() < 2) 
      throw range_error("Minimaal 2 adres nodes verwacht");
    
    for (unsigned int i = 1; i< ids.size();i++) {
      cout <<"Interpol : l= " << ids[i-1] << " " <<lats[i-1] <<" " << lons[i-1] << " " <<housenumbers[i-1] << endl;
      cout <<"Interpol : h= " << ids[i] << " " <<lats[i] <<" " << lons[i] << " " <<housenumbers[i] << endl;
      int l = atol(housenumbers[i-1].c_str());
      int h = atol(housenumbers[i].c_str());
      double dlat = (lats[i] - lats[i-1])/(h-l);
      double dlon = (lons[i] - lons[i-1])/(h-l);
      int step1 = step;
      if (h < l) 
	step1 = -step;
      for (int n=l+step1;(step1>0?n<h:n>h);n+=step1) {
	double latn = lats[i-1] + (dlat *((n-l)));
	double lonn = lons[i-1] + (dlon *((n-l)));
	cout << "  nr : " << n << " " << latn << ","  << lonn << endl;
	_con->begin();
	long nid = _con->createTemporaryNode(latn,lonn);
	stringstream nstr;
	nstr << n;
	_con->createTag(nid,"node","addr:housenumber",nstr.str());
	_con->createTag(nid,"node","addr:country",countries[i-1]);
	_con->createTag(nid,"node","addr:city",cities[i-1]);
	_con->createTag(nid,"node","addr:street",streets[i-1]);
	_con->createTag(nid,"node","addr:postcode",postcodes[i-1]);
	_con->createAdres(nid,"node",countries[i-1],cities[i-1],streets[i-1],nstr.str(),postcodes[i-1]);
	_con->commit();
	Node n=_nodes[nid];

	cout << n << endl;
      }
    }
  }
}
