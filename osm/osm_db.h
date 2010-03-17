#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>
#include <map>
#include <vector>
#include <glibmm/ustring.h>

namespace osm_db {
  
  using namespace sqlite3x;
  
  class database {
  public:
    database(std::string naam);
    ~database();
    
    void createNode(long id,int version,double lat,double lon);
    void createWay(long id,int version);
    void createRelation(long id,int version);
    void createTag(long id,std::string k,std::string v);
    void createNd(long id,int seq,long ref);
    void createMember(long id,int seq,long ref,std::string type,std::string role);
    
    inline void executenonquery(std::string query) {_sql->executenonquery(query);}
    
  private: 
    sqlite3_connection *_sql;
    sqlite3_command *_createNode;
    sqlite3_command *_createWay;
    sqlite3_command *_createRelation;
    sqlite3_command *_createTag;
    sqlite3_command *_createNd;
    sqlite3_command *_createMember;
  };
  
}

#endif
