#include "osm.h"
#include <sqlite3x.hpp>

using namespace osm;
using namespace std;
using namespace sqlite3x;

Element::Element() {}

void Element::addTag(string k,string v) {
  _tags[k] = v;
}

void Element::setId(string id) {
  _id = atol(id);
}

void Element::setVersion(string version) {
  _version = atol(version);
}

//Way


Way::Way() : Element() {}

void Way::addNd(long ref) {
    Nd nd(ref);
    _nds.push_back(nd);
}

void Way::store(sqlite3_connection *con) {
  cout << "Way id = " << _id << " version = " << _version << endl;
}


// Relation 


Relation::Relation() : Element() {}

void Relation::addMember(long ref,string type,string role) {
	Member mem(ref,type,role);
	_members.push_back(mem);
}

void Relation::store(sqlite3_connection *con) {
  cout << "Relation id = " << _id << " version = " << _version << endl;
}


// Node


Node::Node() : Element() {}

void Node::store(sqlite3_connection *con) {
  cout << "Node id = " << _id << " version = " << _version << endl;
}
