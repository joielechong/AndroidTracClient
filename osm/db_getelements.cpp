#include "osm_db.h"
#include <sqlite3x.hpp>
#include <string>
#include <iostream>
#include <cstring>
#include <vector>
#include <stdexcept>

namespace osm_db {  
  using namespace std;
  using namespace sqlite3x;
  
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
      } else
        throw std::range_error("Node does not exist");
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
      if (cur.step())
	version = cur.getint(0);
      else
	version = -1;
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
      if (cur.step())
	version = cur.getint(0);
      else
	version = -1;
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
