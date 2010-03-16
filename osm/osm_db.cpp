#include "osm.h"
#include "osm_db.h"
#include <string>

namespace osm_db {

using namespace std;
using namespace sqlite3x;
using namespace osm;

database::database(string naam) {
  sql = new sqlite3_connection(naam);
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

database& database::operator<<(database& db,const Node& n) {
	createNode.bind(1,n.ref());
	createNode.bind(2,n.version());
	createNode.bind(3,n.lat());
	createNode.bind(4,n.lon());
	createNode.executenonquery();
	return db;
}

database& database::operator<<(database& db,const Way& n) {
	createWay.bind(1,n.ref());
	createWay.bind(2,n.version());
	createWay.executenonquery();
	return db;
}

database& database::operator<<(database& db,const Relation& n) {
	createRelation.bind(1,n.ref());
	createRelation.bind(2,n.version());
	createRelation.executenonquery();
	return db;
}
}
