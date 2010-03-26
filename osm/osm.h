#ifndef _OSM_H
#define _OSM_H

#include <vector>
#include <string>
#include "osm_db.h"
#include <iostream>
#include <cstdlib>
#include <map>

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
    Element();
    Element(long id,int version);
    Element(string id,string version);
    ~Element();
    
    inline long id() const { return _id;}
    inline int version() const { return _version;}
    void addTag(string k,string v);
    
    inline void setId(string id) {_id=atol(id.c_str());}
    inline void setVersion(string version) {_version=atol(version.c_str());}
    virtual void addNd(long ref);
    inline void addNd(string ref) {addNd(atol(ref.c_str()));};
    virtual void addMember(long ref,string type,string role);
    inline void addMember(string ref,string type,string role) {addMember(atol(ref.c_str()),type,role);}
    virtual string printTags();
    virtual void setLat(string lat);
    virtual void setLon(string lon);
    virtual string output();
    
  protected:
    long _id;
    int	_version;
    string _type;
    vector<string> _k;
    vector<string> _v;
  };
  
  class Way  : public Element {
  public:
    inline Way() : Element() {_type="way";}
    inline Way(long id,int version) : Element(id,version) {}
    inline Way(string id,string version) : Element(id,version) {}
    Way(long id,osm_db::database &con);
    inline ~Way() {}
    string output ();
    
    inline void addNd(long ref) {_nds.push_back(ref);}
    
  private:
    vector<long> _nds;
  };
  
  class Relation  : public Element {
  public:
    inline Relation() : Element() {_type="relation";}
    inline Relation(long id,int version) : Element(id,version) {}
    inline Relation(string id,string version) : Element(id,version) {}
    Relation(long id,osm_db::database &con);
    inline ~Relation() {}
    string output ();
    
    void addMember(long ref,string type,string role);
    
  private:
    vector<Member> _members;
  };
  
  class Node  : public Element {
  public:
    inline Node() : Element() {_type="node";}
    inline Node(long id,int version,double lat,double lon) : Element(id,version),_lat(lat),_lon(lon)  {}
    inline Node(string id,string version,string lat,string lon) : Element(id,version),_lat(atof(lat.c_str())),_lon(atof(lon.c_str())) {}
    Node(long id,osm_db::database &con);
    inline ~Node() {}
    inline double lat() const { return _lat;}
    inline double lon() const { return _lon;}
    string output ();
    
    inline virtual void setLat(string lat) {_lat=atof(lat.c_str());}
    inline virtual void setLon(string lon) {_lon=atof(lon.c_str());}

    Node * _prev,*_next;
    
  private:
    double _lat;
    double _lon;
    int _x;
    int _y;
  };
  
  inline ostream& operator<<(ostream& o,osm::Element& n) {
    o << n.output();
    return o;
  }
  
  template <class T> class Cache{

    typedef typename std::map<long,T*> cache_t;
    typedef typename std::map<long,T*>::iterator cache_iter;
    
  public:
    Cache(osm_db::database *con,const unsigned long size){_top = NULL;_bottom = NULL;_con=con;_size=size;}
    ~Cache() {}
    
    T operator[](long id) {
      cache_iter it;
      T *e = NULL;

      it =_cache.find(id);
      if (it == _cache.end()) {  // does not exist
	e = new T(id,*_con);
	e->_prev=NULL;
	e->_next=_top;
	if (_top != NULL) 
	  _top->_prev = e;
	_top = e;
	if (_bottom == NULL)
	  _bottom = e;
	_cache[id]=e;
	if (_cache.size() > _size) {
	  T *p = _bottom->_prev;
	  p->_next = NULL;
	  _cache.erase(_bottom->id());
	  _bottom = p;
	}
      } else if (_bottom != _top) {
	e = it->second;
	T *p = e->_prev;
	T *n = e->_next;
	if (p != NULL) { // top element remains at top so no action for ==
	  p->_next = n;
	  if (n != NULL)
	    n->_prev = p;
	  else
	    _bottom = p;
	  e->_prev = NULL;
	  e->_next = _top;
	  _top = e;
	}
      }
      return *e;
    }
    
  private:
    cache_t _cache;
    T *_top;
    T *_bottom;
    osm_db::database *_con;
    unsigned long _size;
    
  };
}

#endif
