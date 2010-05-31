#ifndef _OSM_H
#define _OSM_H

#include "config.h"
#include "osm_db.h"
#include <libxml++/libxml++.h>
#include <vector>
#include <string>
#include <iostream>
#include <cstdlib>
#include <map>
#include <list>

#define INFINITY (9.99E99)

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
  
  typedef map<string,string> tag_type;
  
  class Element {
  public:
    Element();
    Element(long id,int version);
    Element(string id,string version);
    virtual ~Element();
    
    inline long id() const { return _id;}
    inline int version() const { return _version;}
    inline void addTag(const string k,const string v) {_tags[k] = v;}
    
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
    void getTags(const string type,osm_db::database &con);
    string operator[](const string tagkey);
    
  protected:
    long _id;
    int	_version;
    string _type;
    tag_type _tags;
    string _name;
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
    inline long getNodesCount() const {return _nds.size();}
    inline long getNd(const long seq) const {return _nds[seq];}
    Way *_prev,*_next;

  private:
    vector<long> _nds;
    string _highway;
    string _ref;
    int _oneway;
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

    Relation *_prev,*_next;

  private:
    vector<Member> _members;
    bool _coordsLoaded;
    vector<double> _lats;
    vector<double> _lons;
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

    Node *_prev,*_next;
    
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

    typedef map<long,T*> cache_t;
    typedef typename map<long,T*>::iterator cache_iter;
    
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
      cout << "top to bottom" << endl;
      for (T *xxx=_top;xxx != NULL; xxx=xxx->_next)
	cout << xxx->id() << " ";
      cout << endl;
      cout << "bottom to top" << endl;
      for (T *xxx=_bottom;xxx != NULL; xxx=xxx->_prev)
	cout << xxx->id() << " ";
      cout << endl;
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

  class Highway {
  public:
    inline Highway() : _extracost(0),_speed(0) {}
    inline unsigned int speed() const {return _speed;}
    inline void speed(const unsigned int s) {_speed=s;}
    inline unsigned int extracost() const {return _extracost;}
    inline void extracost(const unsigned int e) {_extracost=e;}
    inline void output(ostream &out) {out << "Speed = " << _speed << " extracost = " << _extracost;}

  private:
    int _extracost;
    unsigned int _speed;
  };

  class Profile {
  public:
    inline Profile() : _maxspeed(0),_avgspeed(0),_ignore_oneway(false) {}

    inline unsigned int maxspeed() const {return _maxspeed;}
    inline void maxspeed(const unsigned int s) {_maxspeed=s;}
    inline unsigned int avgspeed() const {return _avgspeed;}
    inline void avgspeed(const unsigned int s) {_avgspeed=s;}
    inline bool ignore_oneway() const {return _ignore_oneway;}
    inline void set_ignore_oneway() {_ignore_oneway = true;}
    unsigned int allowed(const string h);
    inline void allowed(string h,const unsigned int e) {_allowed[h]=e;}
    inline unsigned int traffic_calming(const string t) {return _traffic_calming[t];}
    inline void traffic_calming(const string t,const unsigned int e) {_traffic_calming[t]=e;}
    inline unsigned int barrier(const string b) {return _barrier[b];}
    inline void barrier(const string b,const unsigned int e) {_barrier[b]=e;}
    void output(ostream &out);

  private:
    unsigned int _maxspeed;
    unsigned int _avgspeed;
    bool _ignore_oneway;
    map<string,unsigned int> _allowed;
    map<string,unsigned int> _traffic_calming; 
    map<string,unsigned int> _barrier;
  };

  typedef map<string,Highway> highway_type;
  typedef map<string,Profile> profile_type;
  typedef map<long,int> set_type;
  typedef map<long,double> score_type;
  typedef map<long,long> route_type; 
  
  class Map {
  public:
    Map(osm_db::database *con,const unsigned long cacheSize,const string conffile="astarconf.xml");

    inline osm::Cache<osm::Node>& nodes() {return _nodes;}
    inline osm::Node& nodes(const long id) {return _nodes[id];}
    inline osm::Cache<osm::Way>& ways() {return _ways;}
    inline osm::Way& ways(const long id) {return _ways[id];}
    inline osm::Cache<osm::Relation>& relations() {return _relations;}
    inline osm::Relation& relations(const long id) {return _relations[id];}

    void InterpolatedAddresses();
    void InterpolatedAddresses(Way &w);
    inline void InterpolatedAddresses(long id) {InterpolatedAddresses(_ways[id]);}
    osm::Node& Address(const string country,const string city,const string street,const string housenumber,const string postcode) const;

    inline void findNode(const double latinp,const double loninp,const double diff,vector<long> &id,vector<double> &lat,vector<double> &lon,vector<double> &distance) { _con->findNode(latinp,loninp,diff,id,lat,lon,distance);}
    void findLocation(const long nodeid,vector<long> &admins);
    bool insideRelation(const long relationid,long nodeid);
    void findAdmin(const string querystring,vector<string> &naam,vector<int> &level);
    double Astar(const long n1,const long n2,const string &vehicle,list<long> &route);
    double Astar(const long n1,const double lat2,const double lon2,const string &vehicle,list<long> &route);
    double Astar(const double lat1,const double lon1,const long n2,const string &vehicle,list<long> &route);
    double Astar(const double lat1,const double lon1,const double lat2,const double lon2,const string &vehicle,list<long> &route);
    void initRoute(const string &vehicle);
    inline void getNeighbours(const long nodeid,vector<long> &ids) const {_con->getNeighbours(nodeid,ids);}
    double distance(const Node &n1,const Node &n2) const;
    inline double distance(const long n1,const long n2) {return distance(_nodes[n1],_nodes[n2]);}  
    inline long getConnectingWay(const long n1,const long n2) {return _con->getConnectingWay(n1,n2);}
    double direction(const long n1,const long n2);
    inline void getWays(long id,vector<long> &ways) {_con->getWays(id,ways);}
  private:
    void process_conf(const xmlpp::Node* node);
    void process_profiles(const xmlpp::Node* node);
    void process_highways(const xmlpp::Node* node);
    long AstarHelper(int set,long goal,set_type &openset,set_type &closedset,score_type &f,score_type &g,score_type &h,score_type &d,route_type &to);
    double cost(const long n1,const long n2,const long prevnode);
    double calc_h_score(const long n1,const long n2);
    bool wrong_direction(Node &nodex,Node &nodey, Way &ww,string onew);
    
    osm_db::database *_con;
    unsigned long _cacheSize;
    string _vehicle;

    Cache<Node> _nodes;
    Cache<Way> _ways;
    Cache<Relation> _relations;
    string _conffile;

    highway_type _highways;
    profile_type _profiles;
  };
}

#endif
