#include "osm.h"
#include "osm_db.h"
#include <string>
#include <iostream>
#include <fstream>
#include <stdlib.h>

namespace osm_db {

using namespace std;
using namespace sqlite3x;

database::database(string naam) {
  sql = new sqlite3_connection(naam);
    ifstream schema;
    char regel[2048];
    schema.open("schema.sqlite.txt");
    while (schema.good()) {
      schema.getline(regel,2047);
      //      cout << regel << endl;
      if ((strncmp(regel,"CREATE",6) == 0) || (strncmp(regel,"PRAGMA",5) == 0)) {
	sql.executenonquery(regel);
      }
    }
    schema.close();
  createNode = new sqlite3_command(*sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
  createWay = new sqlite3_command(*sql,"INSERT INTO way (id,version,lat,lon) VALUES (?,?)");
  createRelation = new sqlite3_command(*sql,"INSERT INTO relation (id,version,lat,lon) VALUES (?,?)");
}

database::~database() {
  delete createNode;
  delete createWay;
  delete createRelation;
  delete sql;
  sql = NULL;
}

}

