#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include <stdexcept>

namespace osm {
  using namespace std;
  
  Element::Element() {
  }
  Element::Element(long id,int version) {
    _id=id;
    _version=version;
  }
  Element::Element(string id,string version) {
    _id = atol(id.c_str());
    _version = atol(version.c_str());
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

  std::string Element::operator[](const std::string tagkey) const {
    for (unsigned int i=0;i<_k.size();i++) {
      if (_k[i] == tagkey)
	return _v[i];
    }
    return "";
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
    s << "Node: Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon << " x,y = " << _x << "," << _y<< endl;
    s << printTags();
    return s.str();
  }
  
  Node::Node(long id,osm_db::database &con) {
    _id = id;
    con.getNode(id,_version,_lat,_lon,_x,_y);
    con.getTags(id,"node",_k,_v);
  }
  
  Way::Way(long id,osm_db::database &con) {
    _id = id;
    con.getWay(id,_version);
    con.getTags(id,"way",_k,_v);
    con.getNds(id,_nds);
  }
  
  Relation::Relation(long id,osm_db::database &con) {
    _id = id;
    con.getRelation(id,_version);
    con.getTags(id,"relation",_k,_v);
    std::vector<long> ref;
    std::vector<std::string> type;
    std::vector<std::string> role;
    con.getMembers(id,type,role,ref);
    for (unsigned int i=0;i<ref.size();i++) 
      _members.push_back(Member(ref[i],type[i],role[i]));
  }


  Map::Map(osm_db::database *sql,const unsigned long cacheSize) :_con(sql),
								 _cacheSize(cacheSize) {
    _nodes = new osm::Cache<osm::Node>(sql,cacheSize);
    _ways = new osm::Cache<osm::Way>(sql,cacheSize);
    _relations = new osm::Cache<osm::Relation>(sql,cacheSize);
  }
 
  Map::~Map() {
    delete _relations;
    delete _ways;
    delete _nodes;
  }
}
