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
}

database::~database() {
  delete _createNode;
  delete _createWay;
  delete _createRelation;
  delete _createTag;
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

void database::createTags(long id,std::map<std::string,Glib::ustring> tags) {
  map<string,Glib::ustring>::iterator it;
  
  for (it=_tags.begin();it != _tags.end(); it++) 
	createTag(id,(*it).first,(*it).second.c_str());
}

void database::createTag(long id,std::string k,std::string v) {
   _createTag->bind(1,(sqlite3x::int64_t)id);
   _createTag->bind(2,k);
   _createTag->bind(3,v);
   _createTag->executenonquery();
}

}

