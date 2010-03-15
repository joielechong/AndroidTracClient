#include "osm.h"
#include <sqlite3x.hpp>
#include <stdlib.h>

namespace osm {
using namespace std;
using namespace sqlite3x;

Element::Element() {}
Element::~Element() {}

void Element::addTag(string k,string v) {
  _tags[k] = v;
}

void Element::setId(string id) {
  _id = atol(id.c_str());
}

void Element::setVersion(string version) {
  _version = atol(version.c_str());
}

void Element::store(sqlite3_connection *con) {
  cout << "Element id = " << _id << " version = " << _version << endl;
}

void Element::addNd(long ref) {};
void Element::addMember(long ref,string type,string role) {};

void Element::printTags() {
  map<string,string>::iterator it;
  
  for (it=_tags.begin();it != _tags.end(); it++) 
    cout << "   " << (*it).first << " => " << (*it).second << endl;
}

//Way


Way::Way() : Element() {}
Way::~Way() {}

void Way::addNd(long ref) {
    Nd nd(ref);
    _nds.push_back(nd);
}

void Way::store(sqlite3_connection *con) {
  cout << "Way id = " << _id << " version = " << _version << endl;
  printTags();
  
  int i;
  cout << "  Nd size = " << _nds.size() << endl;
  for (i=0;i<_nds.size();i++)
    cout << "   Node[" << i << "] = " << _nds[i].ref() << endl;
  
}


// Relation 


Relation::Relation() : Element() {}
Relation::~Relation() {}

void Relation::addMember(long ref,string type,string role) {
	Member mem(ref,type,role);
	_members.push_back(mem);
}

void Relation::store(sqlite3_connection *con) {
  cout << "Relation id = " << _id << " version = " << _version << endl;
  printTags();
}


// Node


Node::Node() : Element() {}
Node::~Node() {}

void Node::store(sqlite3_connection *con) {
  cout << "Node id = " << _id << " version = " << _version << endl;
  printTags();
}

Nd::Nd(long ref) : _ref(ref) {}

Nd::~Nd() {}

long Nd::ref() {
  return _ref;
}

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

Member::~Member() {}

}

//using namespace osm;
//using namespace std;

//inline ostream& operator<<(ostream& o,const Nd& nd) {
//  o << nd.ref();
//  return o;
//}
