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

string Element::output () const {
throw "Kan Element niet printen";
}

//Way

string Way::output () const {
  stringstream s;
  s << "Id = " << _id << " version = " << _version;
  return s.str();
}

// Relation 

void Relation::addMember(long ref,string type,string role) {
	Member *mem = new Member(ref,type,role);
	_members.push_back(*mem);
	delete mem;
}

string Relation::output () const {
  stringstream s;
  s << "Id = " << _id << " version = " << _version;
  return s.str();
}

// Node

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

string Node::output () const {
  stringstream s;
  s << "Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon;
  return s.str();
}

}

using namespace std;

ostream& operator<<(ostream& o,const osm::Element& n) {
  o << n.output() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Node& n) {
  o << n.output() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Way& n) {
  o << n.output() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Relation& n) {
  o << n.output() << endl;
  return o;
}

