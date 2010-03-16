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

Element::string output () {
throw "Kan Element niet printen";
}

//Way

Way::string output () {
  string s;
  s << "Id = " << _id << " version = " << _version;
  return s;
}

// Relation 

void Relation::addMember(long ref,string type,string role) {
	Member *mem = new Member(ref,type,role);
	_members.push_back(*mem);
	delete mem;
}

Relation::string output () {
  string s;
  s << "Id = " << _id << " version = " << _version;
  return s;
}

// Node

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

Node::string output () {
  string s;
  s << "Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon;
  return s;
}

}

using namespace std;

ostream& operator<<(ostream& o,const osm::Element& n) {
  o << n.output() << endl;
  return o;
}

ostream& operator<<(ostream& o,const osm::Node& n) {
  o <<  << endl;
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

