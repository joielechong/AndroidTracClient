#include "osm_db.h"
#include <string>
#include <iostream>
#include <fstream>
#include <string.h>

namespace osm_db {

  using namespace std;
  using namespace sqlite3x;

database::database(string naam) {
  _sql = new sqlite3_connection(naam);
    ifstream schema;
    char regel[2048];
    schema.open("schema.sqlite.txt");
    while (schema.good()) {
      schema.getline(regel,2047);
      //      cout << regel << endl;
      if ((strncmp(regel,"CREATE",6) == 0) || (strncmp(regel,"PRAGMA",5) == 0)) {
	_sql->executenonquery(regel);
      }
    }
    schema.close();
  _createNode = new sqlite3_command(*_sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
  _createWay = new sqlite3_command(*_sql,"INSERT INTO way (id,version) VALUES (?,?)");
  _createRelation = new sqlite3_command(*_sql,"INSERT INTO relation (id,version) VALUES (?,?)");
  _createTag = new sqlite3_command(*_sql,"INSERT INTO tag (id,k,v) VALUES(?,?,?)");
  _createNd = new sqlite3_command(*_sql,"INSERT INTO nd (id,seq,ref) VALUES(?,?,?)");
  _createMember = new sqlite3_command(*_sql,"INSERT INTO member (id,seq,ref,type,role) VALUES(?,?,?,?,?)");
}

database::~database() {
  delete _createNode;
  delete _createWay;
  delete _createRelation;
  delete _createTag;
  delete _createNd;
  delete _createMember;
  delete _sql;
  _sql = NULL;
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

void database::createTag(long id,std::string k,std::string v) {
   _createTag->bind(1,(sqlite3x::int64_t)id);
   _createTag->bind(2,k);
   _createTag->bind(3,v);
   _createTag->executenonquery();
}

void database::createNd(long id,int seq,long ref) {
   _createNd->bind(1,(sqlite3x::int64_t)id);
   _createNd->bind(2,seq);
   _createNd->bind(3,(sqlite3x::int64_t)ref);
   _createNd->executenonquery();
}

void createMember(long id,int seq,long ref,string type,string role) {
   _createMember->bind(1,(sqlite3x::int64_t)id);
   _createMember->bind(2,seq);
   _createMember->bind(3,(sqlite3x::int64_t)ref);
   _createMember->bind(4,type);
   _createMember->bind(5,role);
   _createMember->executenonquery();
}

}
