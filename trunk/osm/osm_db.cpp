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
}

database::~database() {
  delete _createNode;
  delete _createWay;
  delete _createRelation;
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

}

