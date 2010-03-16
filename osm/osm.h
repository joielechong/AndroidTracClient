#ifndef _OSM_H
#define _OSM_H

#include <vector>
#include <map>
#include <string>
#include <glibmm/ustring.h>
#include "osm_db.h"
#include <iostream>
#include <stdlib.h>

namespace osm {
  
  using namespace std;
  
  class Member {
  public:
    Member(long ref,string type,string role);
    inline ~Member() {}
	inline long ref() const {return _ref;}
	inline string role() const {return _role;}
	inline string type() const {return _type;}
    
  private:
    long _ref;
    string _role;
    string _type;
  };
  
  class Element {
  public:
    inline Element() {}
    inline Element(long id,int version) :_id(id),_version(version) {}
    inline Element(string id,string version) :_id(atol(id.c_str())),_version(atol(version.c_str())) {}
    inline ~Element() {}

	inline long id() const { return _id;}
	inline int version() const { return _version;}
    inline void addTag(string k,string v) {_tags[k] = v;}
    inline void setId(string id) {_id=atol(id.c_str());}
    inline void setVersion(string version) {_version=atol(version.c_str());}
    virtual void addNd(long ref);
    inline void addNd(string ref) {addNd(atol(ref.c_str()));};
    virtual void addMember(long ref,string type,string role);
    inline void addMember(string ref,string type,string role) {addMember(atol(ref.c_str()),type,role);}
    string printTags();
    virtual void setLat(string lat);
    virtual void setLon(string lon);
	virtual string output();
    
  protected:
    long _id;
    int	_version;
    map<string,Glib::ustring> _tags;
  };
  
  class Way  : public Element {
  public:
    inline Way() : Element() {}
	inline Way(long id,int version) : Element(id,version) {}
	inline Way(string id,string version) : Element(id,version) {}
    inline ~Way() {}
	string output ();
    
    inline void addNd(long ref) {_nds.push_back(ref);}
    
  private:
    vector<long> _nds;
  };
  
  class Relation  : public Element {
  public:
    inline Relation() : Element() {}
	inline Relation(long id,int version) : Element(id,version) {}
	inline Relation(string id,string version) : Element(id,version) {}
    inline ~Relation() {}
	string output ();
    
    void addMember(long ref,string type,string role);
    
  private:
    vector<Member> _members;
  };
  
  class Node  : public Element {
  public:
    inline Node() : Element() {}
	inline Node(long id,int version,double lat,double lon) : _lat(lat),_lon(lon), Element(id,version) {}
	inline Node(string id,string version,string lat,string lon) : _lat(atof(lat.c_str())),_lon(atof(lon.c_str())),Element(id,version) {}
    inline ~Node() {}
	inline double lat() const { return _lat;}
	inline double lon() const { return _lon;}
	string output ();
    
    inline virtual void setLat(string lat) {_lat=atof(lat.c_str());}
    inline virtual void setLon(string lon) {_lon=atof(lon.c_str());}
    
  private:
    double _lat;
    double _lon;
    int _x;
    int _y;
  };
  

inline ostream& operator<<(ostream& o,const osm::Element& n) {
  o << n.output();
  return o;
}
}

#endif
