#ifndef _OSM_DB_H
#define _OSM_DB_H

#include "config.h"
#include <string>
#include <sqlite3x.hpp>
#include <sqlite3.h>
#include <vector>
#include <stdbool.h>
#include <stdexcept>

namespace osm_db {

  using namespace std;

  class database {
  public:
    database(string naam);
    ~database();
    
    void setupSchemas(const char *filename);
    inline void setupSchemas(const string s) {setupSchemas(s.c_str());}
    void initializeFill();
    void initTemp();
    inline void update(bool mode) {_update = mode;};
	
    void createNode(long id,int version,double lat,double lon);
    void createWay(long id,int version);
    void createRelation(long id,int version);
    void createTag(long id,string type,string k,string v);
    void createNd(long id,int seq,long ref);
    void createMember(long id,int seq,long ref,string type,string role);
    void createAdres(long nid,string type,string country,string ccity,string street,string housenumber,string postcode);
    void setBoundaries();
    void delElem(string name);
    inline const char *errmsg() { return sqlite3_errmsg(_sql->db());}

    inline long createTemporaryNode(double lat,double lon) {_tempnodes--;createNode(_tempnodes,0,lat,lon); return _tempnodes;}
    inline long creaTemporaryWay() {_tempways--;createWay(_tempways,0); return _tempways;}
    inline long createTemporaryRelation() {_temprelations--;createRelation(_temprelations,0); return _temprelations;}

    inline void begin() { _trans->begin();_in_transaction=1;}
    inline void commit() { _trans->commit();_in_transaction=0;}
    void executenonquery(string query,bool rep=true);

    void getCounts(long &nodes,long &ways,long &rel, long &bounds, long &tags,long &nds, long &mems);
    void getNode(const long id,int &version,double &lat,double &lon,int &x,int &y);
    void getWay(const long id,int &version);
    void getRelation(const long id,int &version);
    void getTags(const long id,string type,vector<string> &k,vector<string> &v);
    void getNds(const long id,vector<long> &ref);
    void getWays(const long nodeid,vector<long> &ways);
    void getMembers(const long id,vector<string> &type,vector<string> &role,vector<long> &ref);
    void findNode(const double latinp,const double loninp,double diff,vector<long> &id,vector<double> &lat,vector<double> &lon,vector<double> &distance);
    void getRelCoords(const long relationid, vector<double> &lat,vector<double> &lon);
    void findAdmin(const double lat,const double lon,vector<long> &ids,vector<string> &names, vector<int> & admlevel);
    void findAddress(const string querystring,vector<long> &nodeids,vector<double> &nodelats,vector<double> &nodelons);
    void findHouses(const long id,vector<long> &nodeids,vector<double> &nodelats,vector<double> &nodelons,vector<string> &countriesd,vector<string> &citiess,vector<string> &streets,vector<string> &housenumbers,vector<string> &postcodes);
    int getDirection(const long x,const long y,const long w);
    void getInterpolationWays(vector<long> &ids);
    void getids(string &sqlcmd,vector<long> &ids);
    void getNeighbours(const long id,vector<long> &ids);
    long getConnectingWay(const long n1,const long n2);
    void adminNode(const long nodeid,vector<long> &admins);
    void findCoor(const double lat,const double lon,vector<long> &ways,vector<long> &nodes,vector<double> &distances);
    void ndAddress(const string querystring,vector<long> &ways,vector<long> &nodes,vector<double> &distances,vector<long> &intpolways,vector<long> &assocways);
 
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
    sqlite3x::sqlite3_command *_getWays;
    sqlite3x::sqlite3_command *_getMembers;
    sqlite3x::sqlite3_command *_getNeighbours;
    sqlite3x::sqlite3_command *_getConn;
    sqlite3x::sqlite3_command *_getDirection;

    sqlite3x::sqlite3_command *_getRelCoords;
    sqlite3x::sqlite3_command *_getRelWays;
    sqlite3x::sqlite3_command *_getWayAsc;
    sqlite3x::sqlite3_command *_getWayDesc;

    sqlite3x::sqlite3_command *_findNode;
    sqlite3x::sqlite3_command *_findCoor;
    sqlite3x::sqlite3_command *_findAdmin;
    sqlite3x::sqlite3_command *_findAddress;
    sqlite3x::sqlite3_command *_findHouses;
    sqlite3x::sqlite3_command *_adminNode;
    

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

  class osm_db_error : public exception {
  public:
    osm_db_error(const char *format,...);
    virtual ~osm_db_error() throw();
    virtual char const * what() const throw();
    
  private:
    string m_what;
  };
}

#endif
