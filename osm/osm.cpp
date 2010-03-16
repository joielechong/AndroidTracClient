#include "osm.h"
#include <sqlite3x.hpp>
#include <stdlib.h>
#include <string>
#include <glibmm/ustring.h>

namespace osm {
using namespace std;
using namespace sqlite3x;

  void Element::addMember(long ref,string type,string role){ throw "Member kan niet in dit type element";}  // throw exception
  void Element::addNd(long ref) { throw "Nd kan niet in dit type element";}   // throw exception
  
  void Element::printTags() {
    map<string,Glib::ustring>::iterator it;
    
    for (it=_tags.begin();it != _tags.end(); it++) 
      cout << "   " << (*it).first << " => " << (*it).second.c_str() << endl;
  }
  
//Way

void Way::store(sqlite3_connection *con) {
  cout << "Way id = " << _id << " version = " << _version << endl;
  printTags();
  int i;
  cout << "  Nd size = " << _nds.size() << endl;
  for (i=0;i<_nds.size();i++)
    cout << "   Node[" << i << "] = " << _nds[i] << endl;
  
}

  void Element::setLat(string ref) {throw "Lat kan alleen bij Node";};  // throw exception
  void Element::setLon(string ref) {throw "Lon kan alleen bij Node";};  // throw exception

// Relation 

void Relation::addMember(long ref,string type,string role) {
	Member *mem = new Member(ref,type,role);
	_members.push_back(*mem);
	delete mem;
}

void Relation::store(sqlite3_connection *con) {
  cout << "Relation id = " << _id << " version = " << _version << endl;
  printTags();
  int i;
  cout << "  members size = " << _members.size() << endl;
  for (i=0;i<_members.size();i++)
    cout << "   Member[" << i << "] = " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;
}


// Node

void Node::store(sqlite3_connection *con) {
  cout << "Node id = " << _id << " version = " << _version << " lat,lon = " << _lat << "," << _lon << endl;
  printTags();
}

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

}

using namespace std;

ostream& operator<<(ostream& o,const osm::Element& n) {
  throw "Output van Element";
}

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