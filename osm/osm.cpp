#include "osm.h"
#include <stdlib.h>
#include <string>
#include <glibmm/ustring.h>
#include <sstream>

namespace osm {
  using namespace std;

  void Element::addMember(long ref,string type,string role){ throw "Member kan niet in dit type element";}  // throw exception
  void Element::addNd(long ref) { throw "Nd kan niet in dit type element";}   // throw exception
  
  string Element::printTags() {
    map<string,Glib::ustring>::iterator it;
	stringstream s;
    
    for (it=_tags.begin();it != _tags.end(); it++) 
      s << "   " << (*it).first << " => " << (*it).second.c_str() << endl;
	return s.str();
  }
  
  void Element::setLat(string ref) {throw "Lat kan alleen bij Node";};  // throw exception
  void Element::setLon(string ref) {throw "Lon kan alleen bij Node";};  // throw exception

string Element::output (){
throw "Kan Element niet printen";
}

//Way

string Way::output () {
  stringstream s;
  int i;

  s << "Id = " << _id << " version = " << _version << endl;

  for(i=0;i<_nds.size();i++) {
    s << "Node[" << i << "] : " << _nds[i] << endl;  

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
  int i;
  
  s << "Id = " << _id << " version = " << _version << endl;;

  for(i=0;i<_members.size();i++) {
    s << "Member[" << i << "] : " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;  

  s << printTags();
  return s.str();
}

// Node

Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}

string Node::output () {
  stringstream s;
  s << "Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon << endl;
  s << printTags();
  return s.str();
}

}