#include "osm.h"

using namespace osm;

Element::Element() {}

void Element::addTag(string k,string v) {
  _tags[k] = v;
}

void Element::setId(string id) {
  _id = id;
}

void Element::setVersion(string version) {
  _version = version;
}

//Way


Way::Way() : Element() {}

void Way::addNd(long ref) {
    Nd nd(ref);
    _nds.push_back(nd);
}

void Way::store(sqlite3_connection con) {
  std::cout << "Way id = " << _id << " version = " << _version << std::endl;
}


// Relation 


Relation::Relation() : Element() {}

void Relation::addMember(long ref,string type,string role) {
	Member mem(ref,type,role);
	_members.push_back(mem);
}
void Relation::store(sqlite3_connection con) {
  std::cout << "Relation id = " << _id << " version = " << _version << std::endl;
}


// Node


Node::Node() : Element() {}

void Node::store(sqlite3_connection con) {
  std::cout << "Node id = " << _id << " version = " << _version << std::endl;
}
