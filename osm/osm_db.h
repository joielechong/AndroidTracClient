#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>
#include <sqlite3.h>
#include <vector>
#include <stdbool.h>
#include <stdexcept>

namespace osm_db {
  
  class database {
  public:
    database(std::string naam);
    ~database();
    
    void setupSchemas(const char *filename);
    inline void setupSchemas(const std::string s) {setupSchemas(s.c_str());}
    void initializeFill();
    void postprocess();
    inline void update(bool mode) {_update = mode;};
	
    void createNode(long id,int version,double lat,double lon);
    void createWay(long id,int version);
    void createRelation(long id,int version);
    void createTag(long id,std::string type,std::string k,std::string v);
    void createNd(long id,int seq,long ref);
    void createMember(long id,int seq,long ref,std::string type,std::string role);

    void createAdres(long nid,std::string type,std::string country,std::string ccity,std::string street,std::string housenumber,std::string postcode);

    inline long createTemporaryNode(double lat,double lon) {_tempnodes--;createNode(_tempnodes,0,lat,lon); return _tempnodes;}
    inline long creaTemporaryWay() {_tempways--;createWay(_tempways,0); return _tempways;}
    inline long createTemporaryRelation() {_temprelations--;createRelation(_temprelations,0); return _temprelations;}

    inline void begin() { _trans->begin();_in_transaction=1;}
    inline void commit() { _trans->commit();_in_transaction=0;}
    void executenonquery(std::string query,bool rep=true);

    void getCounts(long &nodes,long &ways,long &rel, long &bounds, long &tags,long &nds, long &mems);
    void getNode(const long id,int &version,double &lat,double &lon,int &x,int &y);
    void getWay(const long id,int &version);
    void getRelation(const long id,int &version);
    void getTags(const long id,std::string type,std::vector<std::string> &k,std::vector<std::string> &v);
    void getNds(const long id,std::vector<long> &ref);
    void getMembers(const long id,std::vector<std::string> &type,std::vector<std::string> &role,std::vector<long> &ref);
    void findNode(const double latinp,const double loninp,double diff,std::vector<long> &id,std::vector<double> &lat,std::vector<double> &lon,std::vector<double> &distance);
    void getRelCoords(const long relationid, std::vector<double> &lat,std::vector<double> &lon);
    void findAdmin(const double lat,const double lon,std::vector<long> &ids,std::vector<std::string> &names, std::vector<int> & admlevel);
    void findAddress(const std::string querystring,std::vector<long> &nodeids,std::vector<double> &nodelats,std::vector<double> &nodelons);
    void findHouses(const long id,std::vector<long> &nodeids,std::vector<double> &nodelats,std::vector<double> &nodelons,std::vector<std::string> &countriesd,std::vector<std::string> &citiess,std::vector<std::string> &streets,std::vector<std::string> &housenumbers,std::vector<std::string> &postcodes);
    void getInterpolationWays(std::vector<long> &ids);

  private: 
    sqlite3x::sqlite3_connection *_sql;
    
    sqlite3x::sqlite3_command *_createNode;
    sqlite3x::sqlite3_command *_createWay;
    sqlite3x::sqlite3_command *_createRelation;
    sqlite3x::sqlite3_command *_createTag;
    sqlite3x::sqlite3_command *_createNd;
    sqlite3x::sqlite3_command *_createMember;
    sqlite3x::sqlite3_command *_createAdres;

    sqlite3x::sqlite3_command *_getCounts;
    sqlite3x::sqlite3_command *_getNode;
    sqlite3x::sqlite3_command *_getWay;
    sqlite3x::sqlite3_command *_getRelation;
    sqlite3x::sqlite3_command *_getTags;
    sqlite3x::sqlite3_command *_getNds;
    sqlite3x::sqlite3_command *_getMembers;

    sqlite3x::sqlite3_command *_getRelCoords;
    sqlite3x::sqlite3_command *_getRelWays;
    sqlite3x::sqlite3_command *_getWayAsc;
    sqlite3x::sqlite3_command *_getWayDesc;

    sqlite3x::sqlite3_command *_findNode;
    sqlite3x::sqlite3_command *_findAdmin;
    sqlite3x::sqlite3_command *_findAddress;
    sqlite3x::sqlite3_command *_findHouses;

    sqlite3x::sqlite3_command *_delTags;
    sqlite3x::sqlite3_command *_delNds;
    sqlite3x::sqlite3_command *_delMems;
    
    sqlite3x::sqlite3_transaction *_trans;
    int _in_transaction;
    bool _update;

    long _tempnodes;
    long _tempways;
    long _temprelations;
  };

  class osm_db_error : public std::exception {
  public:
    osm_db_error(const char *format,...);
    virtual ~osm_db_error() throw();
    virtual char const * what() const throw();
    
  private:
    std::string m_what;
  };
}

#endif
