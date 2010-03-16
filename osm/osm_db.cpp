#include "osm.h"
#include "osm_db.h"
#include <string>

namespace osm_db {

using namespace std;
using namespace sqlite3x;

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

/*

database& database::operator<<(database& db,const osm::Element& n) {
  throw "database store van Element";
}

database& database::operator<<(database& db,const osm::Node& n) {
	createNode.bind(1,n.ref());
	createNode.bind(2,n.version());
	createNode.bind(3,n.lat());
	createNode.bind(4,n.lon());
	createNode.executenonquery();
	return this;
}

database& database::operator<<(database& db,const osm::Way& n) {
	createWay.bind(1,n.ref());
	createWay.bind(2,n.version());
	createWay.executenonquery();
	return this;
}

database& database::operator<<(database& db,const osm::Relation& n) {
	createRelation.bind(1,n.ref());
	createRelation.bind(2,n.version());
	createRelation.executenonquery();
	return this;
}
*/
}
