#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>
#include <sqlite3.h>

namespace osm_db {
  
  class database {
  public:
    database(std::string naam);
    ~database();
    
    void setupSchemas(const char *filename);
    void initializeFill();
    void postprocess();
	
    void createNode(long id,int version,double lat,double lon);
    void createWay(long id,int version);
    void createRelation(long id,int version);
    void createTag(long id,std::string type,std::string k,std::string v);
    void createNd(long id,int seq,long ref);
    void createMember(long id,int seq,long ref,std::string type,std::string role);

    inline void begin() { _trans->begin();}
    inline void commit() { _trans->commit();}
    void executenonquery(std::string query);

    void getCounts(long &nodes,long &ways,long &rel, long &bounds, long &tags,long &nds, long &mems);

  private: 
    sqlite3x::sqlite3_connection *_sql;
    
    sqlite3x::sqlite3_command *_createNode;
    sqlite3x::sqlite3_command *_createWay;
    sqlite3x::sqlite3_command *_createRelation;
    sqlite3x::sqlite3_command *_createTag;
    sqlite3x::sqlite3_command *_createNd;
    sqlite3x::sqlite3_command *_createMember;
    sqlite3x::sqlite3_command *_getCounts;
    
    sqlite3x::sqlite3_transaction *_trans;
  };
}

#endif
