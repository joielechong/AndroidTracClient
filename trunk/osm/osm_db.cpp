#include "osm_db.h"
#include <sqlite3x.hpp>
#include <string>
#include <iostream>
#include <fstream>
#include <cstring>
#include <cmath>
#include <vector>
#include <stdexcept>

namespace osm_db {  
  using namespace std;
  using namespace sqlite3x;
  
#include "myfuncs.c"

  database::database(string naam) {
    _sql = new sqlite3_connection(naam);
    _trans = new sqlite3_transaction(*_sql,false); // no automatic begin
    _createNode = NULL;
    _createWay = NULL;
    _createRelation = NULL;
    _createTag = NULL;
    _createNd = NULL;
    _createMember = NULL;
    _getCounts = NULL;
    _getNode = NULL;
    _getWay = NULL;
    _getRelation = NULL;
    _getNds = NULL;
    _getMembers = NULL;
    _getTags = NULL;
    _findNode = NULL;
    _in_transaction=0;
    sqlite3_create_function(_sql->db(),"osmdistance",4,SQLITE_ANY,NULL,osmdistance,NULL,NULL);
  }
  
  database::~database() {
    if (_in_transaction == 1) 
      _trans->commit();
    _in_transaction=0;
    if (_trans != NULL)
      delete _trans;
    if (_createNode != NULL)
      delete _createNode;
    if (_createWay != NULL)
      delete _createWay;
    if (_createRelation != NULL)
      delete _createRelation;
    if (_createTag != NULL)
      delete _createTag;
    if (_createNd != NULL)
      delete _createNd;
    if (_createMember != NULL)
      delete _createMember;
    if (_getCounts != NULL)
      delete _getCounts;
    if (_getNode != NULL)
      delete _getNode;
    if (_getTags != NULL)
      delete _getTags;
    if (_getNds != NULL)
      delete _getNds;
    if (_getMembers != NULL)
      delete _getMembers;
    if (_findNode != NULL)
      delete _findNode;
    delete _sql;
    _sql = NULL;
  }
  
  void database::setupSchemas(const char *filename) {
    ifstream schema;
    char regel[2048];
    schema.open(filename);
    while (schema.good()) {
      schema.getline(regel,2047);
      //      cout << regel << endl;
      if (strncmp(regel,"DROP",4) != 0 && strlen(regel) > 0) {
	executenonquery(regel,false);
      }
    }
    schema.close();  
  }
  
  void database::initializeFill() {
    _createNode = new sqlite3_command(*_sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
    _createWay = new sqlite3_command(*_sql,"INSERT INTO way (id,version) VALUES (?,?)");
    _createRelation = new sqlite3_command(*_sql,"INSERT INTO relation (id,version) VALUES (?,?)");
    _createTag = new sqlite3_command(*_sql,"INSERT INTO tag (id,type,k,v) VALUES(?,?,?,?)");
    _createNd = new sqlite3_command(*_sql,"INSERT INTO nd (id,seq,ref) VALUES(?,?,?)");
    _createMember = new sqlite3_command(*_sql,"INSERT INTO member (id,seq,ref,type,role) VALUES(?,?,?,?,?)");
  }
  
  void database::postprocess() {
    executenonquery("UPDATE tag SET v='associatedStreet' WHERE type='relation' AND k='type' AND v='relatedStreet'");
    executenonquery("DELETE FROM relation WHERE id in (SELECT id FROM relationtag WHERE k='type' AND NOT v in ('boundary','restriction','multipolygon','associatedStreet'))");
    executenonquery("DELETE FROM way WHERE NOT id in (SELECT id FROM waytag WHERE k in ('highway','boundary','route','natural') OR k like 'addr:%' OR k like 'is_in%' UNION SELECT ref FROM member WHERE type = 'way')");
    executenonquery("DELETE FROM way WHERE id in (SELECT id FROM waytag as tag WHERE ((k='route' AND NOT v like 'ferry%') OR ( k='natural' AND NOT v like 'coastline%')))");
    executenonquery("DELETE FROM nd WHERE NOT ref IN (SELECT id FROM node)");
    executenonquery("DELETE FROM member WHERE (type='way' AND NOT ref IN (SELECT id FROM way)) OR (type='node' AND NOT ref IN (SELECT id FROM node)) OR (type='relation' AND NOT ref IN (SELECT id FROM relation))");
    executenonquery("DELETE FROM node WHERE NOT id IN (SELECT id FROM nodetag UNION SELECT ref FROM nd UNION SELECT ref FROM member WHERE type='node')");
    executenonquery("UPDATE tag SET v='yes' WHERE k IN ('bridge','oneway','tunnel') AND v IN ('1','YES','true','Yes')");
    executenonquery("DELETE FROM tag WHERE k IN ('bridge','oneway','tunnel') AND v IN ('NO','FALSE','No','False','no','ny','false')");
	
    executenonquery("UPDATE node SET x=round((lon+90)*20),y=round((lat+180)*20) WHERE id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)");
    executenonquery("INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp");
    executenonquery("INSERT OR REPLACE INTO neighbor (way,id1,id2,distance) SELECT DISTINCT way,id1,id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM nb,node as nd1,node as nd2 WHERE id1=nd1.id AND id2=nd2.id");
    executenonquery("INSERT INTO adressen SELECT id,'node' AS type,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:country') AS country,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:city') AS city,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:street') AS street,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:postcode') AS postcode FROM node WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL");
    executenonquery("INSERT INTO adressen SELECT id,'way' AS type,(SELECT v FROM waytag WHERE id=way.id AND k='addr:country') AS country,(SELECT v FROM waytag WHERE id=way.id AND k='addr:city') AS city,(SELECT v FROM waytag WHERE id=way.id AND k='addr:street') AS street,(SELECT v FROM waytag WHERE id=way.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM waytag WHERE id=way.id AND k='addr:postcode') AS postcode FROM way WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL");
  }
  
  void database::executenonquery(std::string query,bool repcount) {
    try {
    std::cout << "DB: " << query << std::endl;
    _sql->executenonquery(query);
    if (repcount) 
      std::cout << "DB: " << _sql->changes() << " records" << std::endl;
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij uitvoeren van opdracht" <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    } 
  }
  
  void database::createNode(long id,int version,double lat,double lon) {
    _createNode->bind(1,(sqlite3x::int64_t)id);
    _createNode->bind(2,version);
    _createNode->bind(3,lat);
    _createNode->bind(4,lon);
    _createNode->executenonquery();
  }
  
  void database::createWay(long id,int version) {
    _createWay->bind(1,(sqlite3x::int64_t)id);
    _createWay->bind(2,version);
    _createWay->executenonquery();
  }
  
  void database::createRelation(long id,int version) {
    _createRelation->bind(1,(sqlite3x::int64_t)id);
    _createRelation->bind(2,version);
    _createRelation->executenonquery();
  }
  
  void database::createTag(long id,string type,string k,string v) {
    _createTag->bind(1,(sqlite3x::int64_t)id);
    _createTag->bind(2,type);
    _createTag->bind(3,k);
    _createTag->bind(4,v);
    _createTag->executenonquery();
  }
  
  void database::createNd(long id,int seq,long ref) {
    try {
      _createNd->bind(1,(sqlite3x::int64_t)id);
      _createNd->bind(2,seq);
      _createNd->bind(3,(sqlite3x::int64_t)ref);
      _createNd->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij invoeren in tabel Nd("<<id<<","<<seq<<","<<ref<<")"<<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      throw std::domain_error("node bestaat niet");
    }
  }
  
  void database::createMember(long id,int seq,long ref,string type,string role) {
    try {
      _createMember->bind(1,(sqlite3x::int64_t)id);
      _createMember->bind(2,seq);
      _createMember->bind(3,(sqlite3x::int64_t)ref);
      _createMember->bind(4,type);
      _createMember->bind(5,role);
      _createMember->executenonquery();
    }  catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij invoeren in tabel Member("<<id<<","<<seq<<","<<ref<<","<<role<<","<<type<<")"<<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      throw std::domain_error(type+" bestaat niet");
    }
  }
  
  void database::getCounts(long &nodes,long &ways,long &rels, long &bounds, long &tags,long &nds, long &mems) {
    if (_getCounts == NULL) 
      _getCounts = new sqlite3_command(*_sql,"SELECT * FROM counts");
    sqlite3_cursor cur(_getCounts->executecursor());
    cur.step();
    nodes = cur.getint64(0);
    ways = cur.getint64(1);
    rels = cur.getint64(2);
    bounds = cur.getint64(3);
    tags = cur.getint64(4);
    nds = cur.getint64(5);
    mems = cur.getint64(6);
  }
  
  void database::getNode(long id,int &version,double &lat,double &lon, int&x, int &y) {
    std::cout << "getNode("<<id<<")"<<std::endl;
    try {
      if (_getNode == NULL) 
        _getNode = new sqlite3_command(*_sql,"SELECT version,lat,lon,x,y FROM node  WHERE id = ?");
      _getNode->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getNode->executecursor());
      if (cur.step()) {
	version = cur.getint(0);
	lat = cur.getdouble(1);
	lon = cur.getdouble(2);
	x = cur.getint(3);
	y = cur.getint(4);
      } else {
        throw std::range_error("Node does not exist");
      }
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cout << "Exception in sqlite: " << ex.what() <<endl;
      throw std::range_error("Node does not exist");
    }
  }
  
  void database::getWay(long id,int &version) {
    try {
      if (_getWay == NULL) 
        _getWay = new sqlite3_command(*_sql,"SELECT version FROM node  WHERE id = ?");
      _getWay->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getWay->executecursor());
      if (cur.step()) {
	version = cur.getint(0);
      } else {
	version = -1;
      }
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cout << "Exception in sqlite: " << ex.what() <<endl;
      version = -1;
    }
  }
  
  void database::getRelation(long id,int &version) {
    try {
      if (_getRelation == NULL) 
        _getRelation = new sqlite3_command(*_sql,"SELECT version FROM relation  WHERE id = ?");
      _getRelation->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getRelation->executecursor());
      if (cur.step()) {
	version = cur.getint(0);
      } else {
	version = -1;
      }
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cout << "Exception in sqlite: " << ex.what() <<endl;
      version = -1;
    }
  }

  void database::getTags(long id,std::string type,std::vector<std::string> &k,std::vector<std::string> &v) {
    if (_getTags == NULL) 
      _getTags = new sqlite3_command(*_sql,"SELECT k,v FROM  tag WHERE id=? and type = ?");
    _getTags->bind(1,(sqlite3x::int64_t)id);
    _getTags->bind(2,type);
    sqlite3_cursor cur(_getTags->executecursor());
    while(cur.step()) {
      k.push_back(cur.getstring(0));
      v.push_back(cur.getstring(1));
    }
  }

  void database::getNds(long id,std::vector<long> &ref) {
    if (_getNds == NULL) 
      _getNds = new sqlite3_command(*_sql,"SELECT seq,ref FROM nd WHERE id=?");
    _getNds->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_getNds->executecursor());
    while(cur.step()) {
      unsigned int seq = cur.getint(0);
      long r = cur.getint64(1);
      while (ref.size() < seq)
	ref.push_back(-1);
      if (seq < ref.size())
	ref[seq] = r;
      else
	ref.push_back(r);
    }
  }

  void database::getMembers(long id,std::vector<std::string> &type,std::vector<std::string> &role,std::vector<long> &ref) {
    if (_getMembers == NULL) 
      _getMembers = new sqlite3_command(*_sql,"SELECT seq,type,role,ref FROM member WHERE id=?");
    _getMembers->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_getMembers->executecursor());
    while(cur.step()) {
      unsigned int seq = cur.getint(0);
      while (ref.size() < seq) {
	ref.push_back(-1);
	type.push_back("");
	role.push_back("");
      }
      if (seq < ref.size()) {
	type[seq] = cur.getstring(1);
	role[seq] = cur.getstring(2);
	ref[seq] = cur.getint64(3);
      } else {
	type.push_back(cur.getstring(1));
	role.push_back(cur.getstring(2));
	ref.push_back(cur.getint64(3));
      }
    }
  }

  void database::findNode(double latinp,double loninp,double diff,std::vector<long> &id,std::vector<double> &lat,std::vector<double> &lon,std::vector<double> &distance) {
    if (_findNode == NULL)
      _findNode = new sqlite3_command(*_sql,"SELECT node.id,node.lat,node.lon,osmdistance(node.lat,node.lon,inp.lat,inp.lon) from node,(select ? as lat,? as lon,? as diff) as inp WHERE node.x IN (round((inp.lon+90)*20),round((inp.lon-inp.diff*2+90)*20),round((inp.lon+inp.diff*2+90)*20)) and node.y in (round((inp.lat+180)*20), round((inp.lat-inp.diff*2+180)*20),round((inp.lat+inp.diff*2+180)*20)) AND abs(node.lat-inp.lat) < inp.diff and abs(node.lon-inp.lon) < inp.diff order by 4");
    _findNode->bind(1,latinp);
    _findNode->bind(2,loninp);
    _findNode->bind(3,diff);
    sqlite3_cursor cur(_findNode->executecursor());
    while (cur.step()) {
      id.push_back(cur.getint64(0));
      lat.push_back(cur.getdouble(1));
      lon.push_back(cur.getdouble(2));
      distance.push_back(cur.getdouble(3));
    }
  }
}
