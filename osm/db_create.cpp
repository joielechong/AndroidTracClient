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
  
  void database::createNode(long id,int version,double lat,double lon) {
    try {
      _createNode->bind(1,(sqlite3x::int64_t)id);
      _createNode->bind(2,version);
      _createNode->bind(3,lat);
      _createNode->bind(4,lon);
      _createNode->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe node "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    }
  }
  
  void database::createWay(long id,int version) {
    try {
      _createWay->bind(1,(sqlite3x::int64_t)id);
      _createWay->bind(2,version);
      _createWay->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe weg "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    }
  }
  
  void database::createRelation(long id,int version) {
    try {
      _createRelation->bind(1,(sqlite3x::int64_t)id);
      _createRelation->bind(2,version);
      _createRelation->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe relatie "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    }
  }
  
  void database::createTag(long id,string type,string k,string v) {
    try {
      _createTag->bind(1,(sqlite3x::int64_t)id);
      _createTag->bind(2,type);
      _createTag->bind(3,k);
      _createTag->bind(4,v);
      _createTag->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe tag voor "<<type<<":"<<id << " k = " <<k << " v = " <<v <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    }
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
      throw osm_db_error("node %ld bestaat niet",id);
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
      throw osm_db_error("%s bestaat niet",type.c_str());
    }
  }
}
