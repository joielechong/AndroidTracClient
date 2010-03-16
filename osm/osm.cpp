#include "osm.h"
#include <sqlite3x.hpp>
#include <stdlib.h>
#include <string>
#include <glibmm/ustring.h>

namespace osm {
using namespace std;
using namespace sqlite3x;

void Element::store(sqlite3_connection *con) {
  cout << "Element id = " << _id << " version = " << _version << endl;
}

  void Element::addMember(long ref,string type,string role){} 
  void Element::printTags() {
    map<string,Glib::ustring>::iterator it;
    
    for (it=_tags.begin();it != _tags.end(); it++) 
      cout << "   " << (*it).first << " => " << (*it).second << endl;
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

  void Element::setLat(string ref) {};
  void Element::setLon(string ref) {};

// Relation 

void Relation::addMember(long ref,string type,string role) {
	Member *mem = new Member(ref,type,role);
	_members.push_back(*mem);
}

void Relation::store(sqlite3_connection *con) {
  cout << "Relation id = " << _id << " version = " << _version << endl;
  printTags();
  int i;
  cout << "  members size = " << _members.size() << endl;
  for (i=0;i<_members.size();i++)
    cout << "   Member[" << i << "] = " << _members[i].ref() << "," << _members[i].type << "," << _members[i].role << endl;
}


// Node

void Node::store(sqlite3_connection *con) {
  cout << "Node id = " << _id << " version = " << _version << " lat,lon = " << _lat << "," << _lon << endl;
  printTags();
}

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

}

