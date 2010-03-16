#include "osm.h"
#include <stdlib.h>
#include <string>
#include <glibmm/ustring.h>

namespace osm {
using namespace std;

  void Element::addMember(long ref,string type,string role){ throw "Member kan niet in dit type element";}  // throw exception
  void Element::addNd(long ref) { throw "Nd kan niet in dit type element";}   // throw exception
  
  void Element::printTags() {
    map<string,Glib::ustring>::iterator it;
    
    for (it=_tags.begin();it != _tags.end(); it++) 
      cout << "   " << (*it).first << " => " << (*it).second.c_str() << endl;
  }
  
  void Element::setLat(string ref) {throw "Lat kan alleen bij Node";};  // throw exception
  void Element::setLon(string ref) {throw "Lon kan alleen bij Node";};  // throw exception

//Way


// Relation 

void Relation::addMember(long ref,string type,string role) {
	Member *mem = new Member(ref,type,role);
	_members.push_back(*mem);
	delete mem;
}

// Node

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

}

using namespace std;

ostream& operator<<(ostream& o,const osm::Node& n) {
  o << "Id = " << n.id() << " version = " << n.version() << " lat,lon = " << n.lat() << " , " << n.lon() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Way& n) {
  o << "Id = " << n.id() << " version = " << n.version() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Relation& n) {
  o << "Id = " << n.id() << " version = " << n.version() << endl;
  return o;
}

