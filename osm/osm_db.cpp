#include "osm_db.h"
#include <string>
#include <iostream>
#include <fstream>
#include <string.h>
#include <math.h>

namespace osm_db {
  
  using namespace std;
  using namespace sqlite3x;

  static void osmdistance(sqlite3_context *sc,int n,sqlite3_value **values);
 
  database::database(string naam) {
    _sql = new sqlite3_connection(naam);
	_trans = new sqlite3_transaction(*_sql,false); // no automatic begin
	_getCounts = NULL;
	sqlite3_create_function(_sql->db(),"osmdistance",4,SQLITE_ANY,NULL,osmdistance,NULL,NULL);
  }
  
  database::~database() {
    _trans->commit();
	delete _trans;
    delete _createNode;
    delete _createWay;
    delete _createRelation;
    delete _createTag;
    delete _createNd;
    delete _createMember;
	delete _getCounts;
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
	    executenonquery(regel);
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
    executenonquery("DELETE FROM relation WHERE id in (SELECT id FROM tag WHERE type='relation' AND k='type' AND NOT v in ('boundary','restriction','multipolygon'))");
    executenonquery("DELETE FROM way WHERE NOT id in (SELECT id FROM tag WHERE k in ('highway','boundary','route','natural') UNION SELECT ref FROM member WHERE type = 'way')");
    executenonquery("DELETE FROM way WHERE id in (SELECT id FROM tag WHERE type = 'way' AND k='route' AND NOT v like 'ferry%')");
    executenonquery("DELETE FROM way WHERE id in (SELECT id FROM tag WHERE type = 'way' AND k='natural' AND NOT v like 'coastline%')");
    executenonquery("DELETE FROM node WHERE NOT id IN (SELECT id FROM tag WHERE type='node' UNION SELECT ref FROM nd UNION SELECT ref FROM member WHERE type='node')");
    executenonquery("DELETE FROM nd WHERE NOT ref IN (SELECT id FROM node)");
    executenonquery("DELETE FROM member WHERE (type='way' AND NOT ref IN (SELECT id FROM way)) OR (type='node' AND NOT ref IN (SELECT id FROM node)) OR (type='relation' AND NOT ref IN (SELECT id FROM relation))");
	
    //    executenonquery("INSERT OR REPLACE INTO neighbor (way,id1,id2,distance) SELECT DISTINCT way,id1,id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM nb,node as nd1,node as nd2 WHERE id1=nd1.id AND id2=nd2.id");
    executenonquery("CREATE TABLE test AS SELECT way,id1,id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM nb,node as nd1,node as nd2 WHERE id1=nd1.id AND id2=nd2.id");
    executenonquery("INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp");
    executenonquery("UPDATE node SET x=round((lon+90)*20),y=round((lat+180)*20) WHERE id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)");
  }
  
  void database::executenonquery(std::string query) {
    std::cout << "DB: " << query << std::endl;
    _sql->executenonquery(query);
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
    _createNd->bind(1,(sqlite3x::int64_t)id);
    _createNd->bind(2,seq);
    _createNd->bind(3,(sqlite3x::int64_t)ref);
    _createNd->executenonquery();
  }
  
  void database::createMember(long id,int seq,long ref,string type,string role) {
    _createMember->bind(1,(sqlite3x::int64_t)id);
    _createMember->bind(2,seq);
    _createMember->bind(3,(sqlite3x::int64_t)ref);
    _createMember->bind(4,type);
    _createMember->bind(5,role);
    _createMember->executenonquery();
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
  
static double grootcirkel(double lat1,double lon1,double lat2,double lon2) {
static double pi = 0;
static double radius = 6378137;
static double drad = 21385;

  if (pi < 3.14)
    pi = atan2(0,1)*2;
  cerr << "grootcirkel: " << pi << " " << radius << " " << drad << " " << lat1 << " " << lon1 << " " << lat2 << " " << lon2 << " " << result << endl;
  return (radius-drad*(sin((lat1+lat2)*pi/360)))*2*asin(sqrt((pow(sin((lat2-lat1)*pi/360),2)+cos(lat1*pi/180)*cos(lat2*pi/180)*pow(sin((lon2-lon1)*pi/360),2))));
}

static void osmdistance(sqlite3_context *sc,int n,sqlite3_value **values) {
  double result,lat1,lon1,lat2,lon2;
  
  lat1 = sqlite3_value_double(values[0]);
  lon1 = sqlite3_value_double(values[1]);
  lat2 = sqlite3_value_double(values[2]);
  lon2 = sqlite3_value_double(values[3]);
  result = grootcirkel(lat1,lon1,lat2,lon2);
  cerr << "osmdistance: " << lat1 << " " << lon1 << " " << lat2 << " " << lon2 << " " << result << endl;
  sqlite3_result_double(sc, result);
}

}
