#include "osm.h"
#include "osm_db.h"
#include <stdlib.h>
#include <string>
#include <glibmm/ustring.h>
#include <sstream>
#include "cache.h"

namespace osm {
  using namespace std;
  
  void Element::addMember(long ref,string type,string role){ throw Glib::ustring("Member kan niet in dit type element");}  // throw exception
  void Element::addNd(long ref) { throw "Nd kan niet in dit type element";}   // throw exception
  
  string Element::printTags() {
    int i;
    stringstream s;
    
    for (i=0;i<_k.size();i++) 
      s << "   " << _k[i] << " => " << _v[i] << endl;
    return s.str();
  }
  
  void Element::createTags(osm_db::database& con) {
    int i;
    
    for (i=0;i<_k.size();i++) 
      con.createTag(_id,_type,_k[i],_v[i]);
  }  
  
  
  void Element::setLat(string ref) {throw "Lat kan alleen bij Node";};  // throw exception
  void Element::setLon(string ref) {throw "Lon kan alleen bij Node";};  // throw exception
  
  string Element::output (){
    throw "Kan Element niet printen";
  }
  
  void Element::store (osm_db::database& con) {
    throw "Kan Element niet opslaan";
  }

  //Way
  
  string Way::output () {
    stringstream s;
    int i;
    
    s << "Way: Id = " << _id << " version = " << _version << endl;
    
    for(i=0;i<_nds.size();i++) 
      s << "   Node[" << i << "] : " << _nds[i] << endl;  
    
    s << printTags();
    return s.str();
  }
  
  void Way::store(osm_db::database& con){
    int i;
    
    con.createWay(_id,_version);
    createTags(con);
    for(i=0;i<_nds.size();i++) 
      con.createNd(_id,i,_nds[i]);
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
    
    s << "Relation: Id = " << _id << " version = " << _version << endl;;
    
    for(i=0;i<_members.size();i++) 
      s << "   Member[" << i << "] : " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;  
    
    s << printTags();
    return s.str();
  }
  
  void Relation::store(osm_db::database& con){
    int i;
    
    con.createRelation(_id,_version);
    createTags(con);
    for(i=0;i<_members.size();i++) 
      con.createMember(_id,i,_members[i].ref(),_members[i].type(),_members[i].role());  
  }
  
  // Node
  
  Member::Member(long ref,string type, string role) : _ref(ref),_type(type),_role(role) {}
  
  string Node::output () {
    stringstream s;
    s << "Node: Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon << endl;
    s << printTags();
    return s.str();
  }
  
  void Node::store(osm_db::database& con){
    con.createNode(_id,_version,_lat,_lon);
    this->createTags(con);
  }
  
}
