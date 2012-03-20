#ifndef _OSM_DB_H
#define _OSM_DB_H

#include "config.h"
#include <string>
#include <vector>
#include <stdbool.h>
#include <stdexcept>

namespace osm_db {

  using namespace std;

  class database {
  public:
    database(string naam) {}
    ~database() {}
    
    virtual void setupSchemas(const char *filename)=0;
    virtual void setupSchemas(const string s)=0;
    virtual void initializeFill()=0;
    virtual void initTemp()=0;
    inline void update(bool mode) {_update = mode;};
	
    virtual void createNode(long id,int version,double lat,double lon)=0;
    virtual void createWay(long id,int version)=0;
    virtual void createRelation(long id,int version)=0;
    virtual void createTag(long id,string type,string k,string v)=0;
    virtual void createNd(long id,int seq,long ref)=0;
    virtual void createMember(long id,int seq,long ref,string type,string role)=0;
    virtual void createAdres(long nid,string type,string country,string ccity,string street,string housenumber,string postcode)=0;
    virtual void setBoundaries()=0;
    virtual void delElem(string name)=0;
    virtual const char *errmsg()=0;

    inline long createTemporaryNode(double lat,double lon) {_tempnodes--;createNode(_tempnodes,0,lat,lon); return _tempnodes;}
    inline long creaTemporaryWay() {_tempways--;createWay(_tempways,0); return _tempways;}
    inline long createTemporaryRelation() {_temprelations--;createRelation(_temprelations,0); return _temprelations;}

    virtual void begin()=0;
    virtual void commit()=0;
    virtual void executenonquery(string query,bool rep=true)=0;

    virtual void getCounts(long &nodes,long &ways,long &rel, long &bounds, long &tags,long &nds, long &mems)=0;
    virtual void getNode(const long id,int &version,double &lat,double &lon,int &x,int &y)=0;
    virtual void getWay(const long id,int &version)=0;
    virtual void getRelation(const long id,int &version)=0;
    virtual void getTags(const long id,string type,vector<string> &k,vector<string> &v)=0;
    virtual void getNds(const long id,vector<long> &ref)=0;
    virtual void getWays(const long nodeid,vector<long> &ways)=0;
    virtual void getMembers(const long id,vector<string> &type,vector<string> &role,vector<long> &ref)=0;
    virtual void findNode(const double latinp,const double loninp,double diff,vector<long> &id,vector<double> &lat,vector<double> &lon,vector<double> &distance)=0;
    virtual void getRelCoords(const long relationid, vector<double> &lat,vector<double> &lon)=0;
    virtual void findAdmin(const double lat,const double lon,vector<long> &ids,vector<string> &names, vector<int> & admlevel)=0;
    virtual void findAddress(const string querystring,vector<long> &nodeids,vector<double> &nodelats,vector<double> &nodelons)=0;
    virtual void findHouses(const long id,vector<long> &nodeids,vector<double> &nodelats,vector<double> &nodelons,vector<string> &countriesd,vector<string> &citiess,vector<string> &streets,vector<string> &housenumbers,vector<string> &postcodes)=0;
    virtual int getDirection(const long x,const long y,const long w)=0;
    virtual void getInterpolationWays(vector<long> &ids)=0;
    virtual void getids(string &sqlcmd,vector<long> &ids)=0;
    virtual void getNeighbours(const long id,vector<long> &ids)=0;
    virtual long getConnectingWay(const long n1,const long n2)=0;
    virtual void adminNode(const long nodeid,vector<long> &admins)=0;
    virtual void findCoor(const double lat,const double lon,vector<long> &ways,vector<long> &nodes,vector<double> &distances)=0;
    virtual void ndAddress(const string querystring,vector<long> &ways,vector<long> &nodes,vector<double> &distances,vector<long> &intpolways,vector<long> &assocways)=0;
    virtual void getBound(double &minlat,double &minlon,double &maxlat,double &maxlon)=0;
 
  protected: 

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
