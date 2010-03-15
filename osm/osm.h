#ifndef _OSM_H
#define _OSM_H

#include <vector>
#include <map>
#include <string>
#include <sqlite3x.hpp>
#include <iostream>

namespace osm {
  
  using namespace std;
  using namespace sqlite3x;

  class Nd {
  public:
    Nd(long ref);
    ~Nd();
	
	long ref();

  private:
    long _ref;
  };
  
  class Member {
  public:
    Member(long ref,string type,string role);
    ~Member();

  private:
    long _ref;
    string _role;
	string _type;
  };
  
  class Element {
  public:
    Element();
    ~Element();
    
    void addTag(string k,string v);
	void setId(string id);
	void setVersion(string version);
	virtual void store(sqlite3_connection *con);
	virtual void addNd(long ref);
	virtual void addMember(long ref,string type,string role);
	void printTags();
    
  protected:
    long _id;
    int	_version;
    map<string,string> _tags;
  };
  
  class Way  : public Element {
  public:
    Way();
    ~Way();
	
	void addNd(long ref);
	virtual void store(sqlite3_connection *con);
    
  private:
    vector<Nd> _nds;
  };
  
  class Relation  : public Element {
  public:
    Relation();
    ~Relation();
	
	void addMember(long ref,string type,string role);
 	virtual void store(sqlite3_connection *con);
   
  private:
    vector<Member> _members;
  };
  
  class Node  : public Element {
  public:
    Node();
    ~Node();

	virtual void store(sqlite3_connection *con);
    
  private:
    double _lat;
    double _lon;
    int _x;
    int _y;
  };
}

#endif
