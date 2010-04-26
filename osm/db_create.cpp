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
      if (strlen(regel) > 0) {
	try {
	  executenonquery(regel,false);
	} catch (const sqlite3x::database_error& ex) {
	  if (strncmp(regel,"DROP",4) != 0) {
	    cout << "Exception in sqlite: " << ex.what() <<endl;
	    std::cerr << "  errmsg = " << this->errmsg() << std::endl;
	    throw osm_db_error("Database error na commando: %s",regel);
 	  }
	}
      }
    }
    schema.close();  
  }
  
  void database::initializeFill() {
    if (_update) {
      _createNode = new sqlite3_command(*_sql,"INSERT OR REPLACE INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
      _createWay = new sqlite3_command(*_sql,"INSERT OR REPLACE INTO way (id,version) VALUES (?,?)");
      _createRelation = new sqlite3_command(*_sql,"INSERT OR REPLACE INTO relation (id,version) VALUES (?,?)");
      _createNeighbour = new sqlite3_command(*_sql,"INSERT OR REPLACE INTO neighbor (way,id1,id2,distance) SELECT inp.way,inp.id1,inp.id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM (SELECT ? as way,? as id1, ? as id2) as inp,node as nd1,node as nd2 WHERE inp.id1=nd1.id AND inp.id2=nd2.id");
      _delTags = new sqlite3_command(*_sql,"DELETE FROM tag WHERE id=? AND type = ?");
      _delNds = new sqlite3_command(*_sql,"DELETE FROM nd WHERE id=?");
      _delMems = new sqlite3_command(*_sql,"DELETE FROM member WHERE id=?");
    } else {
      _createNode = new sqlite3_command(*_sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
      _createWay = new sqlite3_command(*_sql,"INSERT INTO way (id,version) VALUES (?,?)");
      _createNeighbour = new sqlite3_command(*_sql,"INSERT INTO neighbor (way,id1,id2,distance) SELECT inp.way,inp.id1,inp.id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM (SELECT ? as way,? as id1, ? as id2) as inp,node as nd1,node as nd2 WHERE inp.id1=nd1.id AND inp.id2=nd2.id");
      _createRelation = new sqlite3_command(*_sql,"INSERT INTO relation (id,version) VALUES (?,?)");
    }
    _createTag = new sqlite3_command(*_sql,"INSERT INTO tag (id,type,k,v) VALUES(?,?,?,?)");
    _createNd = new sqlite3_command(*_sql,"INSERT INTO nd (id,seq,ref) VALUES(?,?,?)");
    _createMember = new sqlite3_command(*_sql,"INSERT INTO member (id,seq,ref,type,role) VALUES(?,?,?,?,?)");
  }
  
  void database::createNode(long id,int version,double lat,double lon) {
    if (_update) {
      _delTags->bind(1,(sqlite3x::int64_t)id);
      _delTags->bind(2,"node");
      _delTags->executenonquery();
    }

    try {
      _createNode->bind(1,(sqlite3x::int64_t)id);
      _createNode->bind(2,version);
      _createNode->bind(3,lat);
      _createNode->bind(4,lon);
      _createNode->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe node "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
    }
  }
  
  void database::createWay(long id,int version) {
    if (_update) {
      _delTags->bind(1,(sqlite3x::int64_t)id);
      _delTags->bind(2,"way");
      _delTags->executenonquery();
      _delNds->bind(1,(sqlite3x::int64_t)id);
      _delNds->executenonquery();
    }

    try {
      _createWay->bind(1,(sqlite3x::int64_t)id);
      _createWay->bind(2,version);
      _createWay->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe weg "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
    }
  }
  
  void database::createRelation(long id,int version) {
    if (_update) {
      _delTags->bind(1,(sqlite3x::int64_t)id);
      _delTags->bind(2,"relation");
      _delTags->executenonquery();
      _delMems->bind(1,(sqlite3x::int64_t)id);
      _delMems->executenonquery();
    }

    try {
      _createRelation->bind(1,(sqlite3x::int64_t)id);
      _createRelation->bind(2,version);
      _createRelation->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij aanmaken nieuwe relatie "<<id <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
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
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
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
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
      throw osm_db_error("node %ld bestaat niet",id);
    }
  }
  
  void database::createNeighbour(long way,int long id1,long id2) {
    try {
      _createNeighbour->bind(1,(sqlite3x::int64_t)way);
      _createNeighbour->bind(2,(sqlite3x::int64_t)id1);
      _createNeighbour->bind(3,(sqlite3x::int64_t)id2);
      _createNeighbour->executenonquery();
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij invoeren in tabel neighbor("<<way<<","<<id1<<","<<id2<<")"<<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
      //      throw osm_db_error("weg %ld bestaat niet",way);
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
      std::cerr << "  errmsg = " << this->errmsg() << std::endl;
      throw osm_db_error("%s bestaat niet",type.c_str());
    }
  }

  void database::createAdres(long id,string type,string country,string city,string street, string housenumber,string postcode) {
    if (_createAdres == NULL)
      _createAdres = new sqlite3_command(*_sql,"INSERT INTO adressen (id,type,country,city,street,housenumber,postcode) VALUES (?,?,?,?,?,?,?)");
    
    _createAdres->bind(1,(sqlite3x::int64_t)id);
    _createAdres->bind(2,type);
    _createAdres->bind(3,country);
    _createAdres->bind(4,city);
    _createAdres->bind(5,street);
    _createAdres->bind(6,housenumber);
    _createAdres->bind(7,postcode);
    _createAdres->executenonquery();
  }

  void database::setBoundaries() {
    executenonquery("DELETE FROM bound");
    executenonquery("INSERT INTO bound (minlat,maxlat,minlon,maxlon) SELECT min(lat),max(lat),min(lon),max(lon) FROM node,neighbor AS nb WHERE (NOT nb.distance IS NULL) AND (node.id=nb.id1 OR node.id=nb.id2)");
  }
}
