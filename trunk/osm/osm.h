#ifndef _OSM_H
#define _OSM_H

#include <vector>
#include <map>
#include <string>

namespace osm {
  
  using namespace std;
  
  class Nd {
  public:
    Nd(long ref);
    ~Nd();

  private:
    long _ref;
  };
  
  class Member {
  public:
    Member(long ref,string role);
    ~Member();

  private:
    long _ref;
    string _role;
  };
  
  class Element {
  public:
    Element(long id,int version);
    ~Element();
    
    void addTag(string k,string v);
    
  protected:
    long _id;
    int	_version;
    map<string,string> _tags;
  };
  
  class Way : Element {
  public:
    Way(long id,int version);
    ~Way();
    
  private:
    vector<Nd> _nds;
  };
  
  class Relation : Element {
  public:
    Relation(long id,int version);
    ~Relation();
    
  private:
    vector<Member> _members;
  };
  
  class Node : Element {
  public:
    Node(long id,int version, double lat,double lon);
    ~Node();
    
  private:
    double _lat;
    double _lon;
    int _x;
    int _y;
  };
}

#endif
