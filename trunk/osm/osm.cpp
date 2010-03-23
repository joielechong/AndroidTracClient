#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include "cache.h"
#include <stdexcept>

namespace osm {
  using namespace std;
  
  void Element::addMember(long ref,string type,string role){ throw new std::range_error("Member kan niet in dit type element");}  // throw exception
  void Element::addNd(long ref) { throw new std::range_error("Nd kan niet in dit type element");}   // throw exception
  
  string Element::printTags() {
    unsigned int i;
    stringstream s;
    
    for (i=0;i<_k.size();i++) 
      s << "   " << _k[i] << " => " << _v[i] << endl;
    return s.str();
  }
  
  void Element::createTags(osm_db::database& con) {
    unsigned int i;
    
    for (i=0;i<_k.size();i++) 
      con.createTag(_id,_type,_k[i],_v[i]);
  }  
  
  
  void Element::setLat(string ref) {throw new std::range_error("Lat kan alleen bij Node");};  // throw exception
  void Element::setLon(string ref) {throw new std::range_error("Lon kan alleen bij Node");};  // throw exception
  
  string Element::output (){
    throw new std::range_error("Kan Element niet printen");
  }
  
  void Element::store (osm_db::database& con) {
    throw new std::range_error("Kan Element niet opslaan");
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
  
  void Way::store(osm_db::database& con){
    unsigned int i;
    
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
    unsigned int i;
    
    s << "Relation: Id = " << _id << " version = " << _version << endl;;
    
    for(i=0;i<_members.size();i++) 
      s << "   Member[" << i << "] : " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;  
    
    s << printTags();
    return s.str();
  }
  
  void Relation::store(osm_db::database& con){
    unsigned int i;
    
    con.createRelation(_id,_version);
    createTags(con);
    for(i=0;i<_members.size();i++) 
      con.createMember(_id,i,_members[i].ref(),_members[i].type(),_members[i].role());  
  }
  
  // Node
  
  Member::Member(long ref,string type, string role) : _ref(ref),_role(role),_type(type) {}
  
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
  
  Nodes::Nodes() {}
  Nodes::~Nodes() {}

  Node Nodes::operator[](long id) {
    Node n(id,1,52.,4.);
    n.addTag("created_by","Michiel van Loon");
    n.addTag("highway","unclassified");
    n.addTag("note","dit is een test van het size methode. Ik ben benieuwd of het werkt");
    n.addTag("verhaal","dit is een ander verhaal maar vult wel lekker op zo");
    return n;
  }

  void Element::addTag(string k,string v) {_k.push_back(k);_v.push_back(v);}


  long Node::size() const {
    return sizeof *this;
  }
}


int main() {

  osm::Nodes nodes;

  osm::Node nd = nodes[1];
  long lengte = nd.size();
  cout << nd << endl;

  osm::Node nd1= nd;

  int j = 0;
  for (int i=1;i<=100;i++)
    j+= i;
  cout << j << endl;
  cout <<"Lengte is " << lengte << endl;
  for (int i=1;i<=100;i++)
    j+= i;
  cout << j << endl;
  cout << nd1.size() << endl;
  return 0;

}
