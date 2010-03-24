#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include <stdexcept>

namespace osm {
  using namespace std;
  
  Element::Element() {
    _next = NULL;
	_prev = NULL;
  }
  Element::Element(long id,int version) {
    _id=id;
	_version=version;
	_next = NULL;
	_prev = NULL;
  }
  ElementElement(string id,string version) {
    _id = atol(id.c_str());
    _version = atol(version.c_str());
    _next = NULL;
	_prev = NULL;
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

  //Way
  
  string Way::output () {
    stringstream s;
    unsigned int i;
    
    s << "Way: Id = " << _id << " version = " << _version << endl;
    
    for(i=0;i<_nds.size();i++) 
      s << "   Node[" << i << "] : " << _nds[i] << endl;  
    
    s << printTags();
    return s.str();
  }
  
  // Relation 
  
  void Relation::addMember(long ref,string type,string role) {
    Member *mem = new Member(ref,type,role);
    _members.push_back(*mem);
    delete mem;
  }
  
  string Relation::output () {
    stringstream s;
    unsigned int i;
    
    s << "Relation: Id = " << _id << " version = " << _version << endl;;
    
    for(i=0;i<_members.size();i++) 
      s << "   Member[" << i << "] : " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;  
    
    s << printTags();
    return s.str();
  }
  
  // Node
  
  Member::Member(long ref,string type, string role) : _ref(ref),_role(role),_type(type) {}
  
  string Node::output () {
    stringstream s;
    s << "Node: Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon << endl;
    s << printTags();
    return s.str();
  }
  
  Node::Node(long id,osm_db::database con) {
    _id = id;
	con.getNode(id,version,lat,lon);
  }
}
