#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include <stdexcept>

namespace osm {
  using namespace std;
  
  Element::Element() {
  }
  Element::Element(long id,int version) {
    _id=id;
    _version=version;
  }
  Element::Element(string id,string version) {
    _id = atol(id.c_str());
    _version = atol(version.c_str());
  }
  Element::~Element() {}
  
  void Element::addMember(long ref,string type,string role){
    throw new std::range_error("Member kan niet in dit type element");
  }
  
  void Element::addNd(long ref) {
    throw new std::range_error("Nd kan niet in dit type element");
  }
  
  string Element::printTags() {
    unsigned int i;
    stringstream s;
    
    for (i=0;i<_k.size();i++) 
      s << "   " << _k[i] << " => " << _v[i] << endl;
    return s.str();
  }

  std::string Element::operator[](const std::string tagkey) const {
    for (unsigned int i=0;i<_k.size();i++) {
      if (_k[i] == tagkey)
	return _v[i];
    }
    throw range_error("Key bestaat niet: "+tagkey);
  }

  void Element::setLat(string ref) {
    throw new std::range_error("Lat kan alleen bij Node");
  }
  void Element::setLon(string ref) {
    throw new std::range_error("Lon kan alleen bij Node");
  }
  
  string Element::output (){
    throw new std::range_error("Kan Element niet printen");
  }
  
  void Element::addTag(string k,string v) {
    _k.push_back(k);
    _v.push_back(v);
  }  
}
