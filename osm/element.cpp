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
    throw range_error("Member kan niet in dit type element");
  }
  
  void Element::addNd(long ref) {
    throw range_error("Nd kan niet in dit type element");
  }
  
  void Element::getTags(const string type,osm_db::database &con) {
    vector<string> k,v;
    con.getTags(_id,type,k,v);
    
   for (unsigned int i=0;i<k.size();i++)
     _tags[k[i]] = v[i];
  }
  
  string Element::printTags() {
    stringstream s;
    
    for (tag_type::iterator i=_tags.begin();i != _tags.end();i++) 
      s << "   " << i->first << " => " << i->second << endl;
    return s.str();
  }

  string Element::operator[](const string tagkey) {
    tag_type::iterator i = _tags.find(tagkey);
    if (i != _tags.end()) 
      return i->second;
    else
      throw range_error("Key bestaat niet: "+tagkey);
  }

  string Element::output (){
    throw range_error("Kan Element niet printen");
  } 
}
