#ifndef _OSM_H
#define _OSM_H

#include "config.h"
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
    virtual ~Element();
    
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

    string operator[](const string tagkey) const;
    
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

    inline void addNd(const long ref) {_nds.push_back(ref);}
    inline long getNodesCount() const { return _nds.size();}
    inline long getNd(const long seq) const { return _nds[seq];}
    Way * _prev,*_next;

  private:
    vector<long> _nds;
  };
  
  class Relation  : public Element {
  public:
    inline Relation() : Element() {_type="relation";_coordsLoaded=false;}
    inline Relation(long id,int version) : Element(id,version) {_coordsLoaded=false;}
    inline Relation(string id,string version) : Element(id,version) {_coordsLoaded=false;}
    Relation(long id,osm_db::database &con);
    inline ~Relation() {}
    string output ();
    
    void addMember(long ref,string type,string role);
    bool isInside(osm_db::database *con,const double lat,const double lon);

    Relation * _prev,*_next;

  private:
    vector<Member> _members;
    bool _coordsLoaded;
    std::vector<double> _lats;
    std::vector<double> _lons;
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
    
    T& operator[](long id) {
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
	  delete _bottom;
	  _bottom = p;
	}
      } else if (_bottom != _top) {
	e = it->second;
	if (e != _top) {
	  T *p = e->_prev;
	  T *n = e->_next;
	  p->_next = n;
	  if (n != NULL)
	    n->_prev = p;
	  else
	    _bottom = p;
	  e->_prev = NULL;
	  e->_next = _top;
	  _top->_prev = e;
	  _top = e;
	}
      } else 
	e = it->second;
      
      /*
      std::cout << "top to bottom" << std::endl;
      for (T *xxx=_top;xxx != NULL; xxx=xxx->_next)
	std::cout << xxx->id() << " ";
      std::cout << std::endl;
      std::cout << "bottom to top" << std::endl;
      for (T *xxx=_bottom;xxx != NULL; xxx=xxx->_prev)
	std::cout << xxx->id() << " ";
      std::cout << std::endl;
      */
      return *e;
    }
    
  private:
    cache_t _cache;
    T *_top;
    T *_bottom;
    osm_db::database *_con;
    unsigned long _size;
  };

  class Map {
  public:
    Map(osm_db::database *con,const unsigned long cacheSize,const string conffile="astarconf.xml");

    inline osm::Cache<osm::Node>& nodes() {return _nodes;}
    inline osm::Cache<osm::Way>& ways() {return _ways;}
    inline osm::Cache<osm::Relation>& relations() {return _relations;}

    void InterpolatedAddresses();
    void InterpolatedAddresses(Way &w);
    inline void InterpolatedAddresses(long id) {InterpolatedAddresses(_ways[id]);}
    osm::Node& Address(const string country,const string city,const string street,const string housenumber,const string postcode) const;
    double distance(const Node &n1,const Node &n2) const;
    inline double distance(const long n1,const long n2) {return distance(_nodes[n1],_nodes[n2]);}
    double cost(const Node &n1,const Node &n2);
    inline double cost(const long n1,const long n2) {return cost(_nodes[n1],_nodes[n2]);}
    

    inline void findNode(const double latinp,const double loninp,const double diff,std::vector<long> &id,std::vector<double> &lat,std::vector<double> &lon,std::vector<double> &distance) { _con->findNode(latinp,loninp,diff,id,lat,lon,distance);}
    bool insideRelation(long relationid,long nodeid);
    void findAdmin(const string querystring,std::vector<string> &naam,std::vector<int> &level);
    double Astar(const long n1,const long n2);
    double Astar(const long n1,const double lat2,const double lon2);
    double Astar(const double lat1,const double lon1,const long n2);
    double Astar(const double lat1,const double lon1,const double lat2,const double lon2);

  private:
    osm_db::database *_con;
    unsigned long _cacheSize;

    Cache<Node> _nodes;
    Cache<Way> _ways;
    Cache<Relation> _relations;
    string _conffile;
  };
}

#endif
