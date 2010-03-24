#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>
#include <sqlite3.h>
#include <vector>

namespace osm_db {
  
  class database {
  public:
    database(std::string naam);
    ~database();
    
    void setupSchemas(const char *filename);
    inline void setupSchemas(const std::string s) {setupSchemas(s.c_str());}
    void initializeFill();
    void postprocess();
	
    void createNode(long id,int version,double lat,double lon);
    void createWay(long id,int version);
    void createRelation(long id,int version);
    void createTag(long id,std::string type,std::string k,std::string v);
    void createNd(long id,int seq,long ref);
    void createMember(long id,int seq,long ref,std::string type,std::string role);

    inline void begin() { _trans->begin();_in_transaction=1;}
    inline void commit() { _trans->commit();_in_transaction=0;}
    void executenonquery(std::string query);

    void getCounts(long &nodes,long &ways,long &rel, long &bounds, long &tags,long &nds, long &mems);
    void getNode(long id,int &version,double &lat,double &lon,int &x,int &y);
    void getWay(long id,int &version);
    void getRelation(long id,int &version);
    void getTags(long id,std::string type,std::vector<std::string> &k,std::vector<std::string> &v);
    void getNds(long id,std::vector<long> &ref);
    void getMembers(long id,std::vector<std::string> &type,std::vector<std::string> &role,std::vector<long> &ref);
    void findNode(double latinp,double loninp,double diff,std::vector<long> &id,std::vector<double> &lat,std::vector<double> &lon,std::vector<double> &distance);


  private: 
    sqlite3x::sqlite3_connection *_sql;
    
    sqlite3x::sqlite3_command *_createNode;
    sqlite3x::sqlite3_command *_createWay;
    sqlite3x::sqlite3_command *_createRelation;
    sqlite3x::sqlite3_command *_createTag;
    sqlite3x::sqlite3_command *_createNd;
    sqlite3x::sqlite3_command *_createMember;
    sqlite3x::sqlite3_command *_getCounts;
    sqlite3x::sqlite3_command *_getNode;
    sqlite3x::sqlite3_command *_getWay;
    sqlite3x::sqlite3_command *_getRelation;
    sqlite3x::sqlite3_command *_getTags;
    sqlite3x::sqlite3_command *_getNds;
    sqlite3x::sqlite3_command *_getMembers;
    sqlite3x::sqlite3_command *_findNode;
    
    sqlite3x::sqlite3_transaction *_trans;
    int _in_transaction;
  };
}

#endif
