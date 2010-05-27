#include "osm_db.h"
#include <sqlite3x.hpp>
#include <string>
#include <iostream>
#include <fstream>
#include <cstring>
#include <cmath>
#include <vector>
#include <stdbool.h>
#include <stdexcept>

namespace osm_db {  
  using namespace std;
  using namespace sqlite3x;
  
#include "myfuncs.h"

  database::database(string naam) {
    _sql = new sqlite3_connection(naam);
    _trans = new sqlite3_transaction(*_sql,false); // no automatic begin
    _createNode = NULL;
    _createWay = NULL;
    _createRelation = NULL;
    _createTag = NULL;
    _createNd = NULL;
    _createMember = NULL;
    _createAdres = NULL;
    _createNeighbour = NULL;
    _getCounts = NULL;
    _getNode = NULL;
    _getWay = NULL;
    _getRelation = NULL;
    _getNds = NULL;
    _getMembers = NULL;
    _getTags = NULL;
    _getNeighbours = NULL;
    _findNode = NULL;
    _findAdmin = NULL;
    _findAddress = NULL;
    _findHouses = NULL;
    _getRelCoords = NULL;
    _getRelWays = NULL;
    _getWayAsc = NULL;
    _getWayDesc = NULL;
    _delTags = NULL;
    _delNds = NULL;
    _delMems = NULL;
    _in_transaction=0;
    _update = false;
    sqlite3_create_function(_sql->db(),"osmdistance",4,SQLITE_ANY,NULL,osmdistance,NULL,NULL);
    sqlite3_create_function(_sql->db(),"int",1,SQLITE_ANY,NULL,osmint,NULL,NULL);
    sqlite3_create_function(_sql->db(),"osmcalc_x",1,SQLITE_ANY,NULL,osmcalc_x,NULL,NULL);
    sqlite3_create_function(_sql->db(),"osmcalc_y",1,SQLITE_ANY,NULL,osmcalc_y,NULL,NULL);

    _tempnodes = 0;
    _tempways = 0;
    _temprelations = 0;
  }

  void database::initTemp() {
    sqlite3_command *s = new sqlite3_command(*_sql,"SELECT (SELECT min(id) FROM node) AS node, (SELECT min(id) FROM way) AS way,(SELECT min(id) FROM relation) AS relation");
    sqlite3_cursor cur(s->executecursor());
    if (cur.step()) {
      _tempnodes = min(0L,(long)cur.getint64(0));
      _tempways = min(0L,(long)cur.getint64(1));
      _temprelations = min(0L,(long)cur.getint64(2));
    }
  }
  
  database::~database() {
    if (_in_transaction == 1) 
      _trans->commit();
    _in_transaction=0;
    if (_trans != NULL)
      delete _trans;
    if (_createNode != NULL)
      delete _createNode;
    if (_createWay != NULL)
      delete _createWay;
    if (_createRelation != NULL)
      delete _createRelation;
    if (_createTag != NULL)
      delete _createTag;
    if (_createNd != NULL)
      delete _createNd;
    if (_createMember != NULL)
      delete _createMember;
    if (_createAdres != NULL)
      delete _createAdres;
    if (_createNeighbour != NULL)
      delete _createNeighbour;
    if (_getCounts != NULL)
      delete _getCounts;
    if (_getNode != NULL)
      delete _getNode;
    if (_getTags != NULL)
      delete _getTags;
    if (_getNds != NULL)
      delete _getNds;
    if (_getMembers != NULL)
      delete _getMembers;
    if (_getNeighbours != NULL)
      delete _getNeighbours;
    if (_findNode != NULL)
      delete _findNode;
    if (_findAdmin != NULL)
      delete _findAdmin;
    if (_findAddress != NULL)
      delete _findAddress;
    if (_findHouses != NULL)
      delete _findHouses;
    if (_getRelCoords != NULL)
      delete _getRelCoords;
    if (_getRelCoords != NULL)
      delete _getRelCoords;
    if (_getRelWays != NULL)
      delete _getRelWays;
    if (_getWayAsc != NULL)
      delete _getWayAsc;
    if (_getWayDesc != NULL)
      delete _getWayDesc;
    if (_delTags == NULL)
      delete _delTags;
    if (_delNds == NULL)
      delete _delNds;
    if (_delMems == NULL)
      delete _delMems;
    delete _sql;
    _sql = NULL;
  }
  
  void database::executenonquery(std::string query,bool repcount) {
    try {
      std::cout << "DB: " << query << std::endl;
      _sql->executenonquery(query);
      if (repcount) 
        std::cout << "DB: " << _sql->changes() << " records" << std::endl;
    } catch (sqlite3x::database_error &ex) {
      std::cerr << "Probleem bij uitvoeren van opdracht" <<std::endl;
      std::cerr << "Exception  = " << ex.what() << std::endl;
    } 
  }
}
