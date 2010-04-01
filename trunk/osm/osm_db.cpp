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
  
#include "myfuncs.c"

  database::database(string naam) {
    _sql = new sqlite3_connection(naam);
    _trans = new sqlite3_transaction(*_sql,false); // no automatic begin
    _createNode = NULL;
    _createWay = NULL;
    _createRelation = NULL;
    _createTag = NULL;
    _createNd = NULL;
    _createMember = NULL;
    _getCounts = NULL;
    _getNode = NULL;
    _getWay = NULL;
    _getRelation = NULL;
    _getNds = NULL;
    _getMembers = NULL;
    _getTags = NULL;
    _findNode = NULL;
    _findAdmin = NULL;
    _findAddress = NULL;
    _getRelCoords = NULL;
    _in_transaction=0;
    sqlite3_create_function(_sql->db(),"osmdistance",4,SQLITE_ANY,NULL,osmdistance,NULL,NULL);
    sqlite3_create_function(_sql->db(),"int",1,SQLITE_ANY,NULL,osmint,NULL,NULL);
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
    if (_findNode != NULL)
      delete _findNode;
    if (_findAdmin != NULL)
      delete _findAdmin;
    if (_findAddress != NULL)
      delete _findAddress;
    if (_getRelCoords != NULL)
      delete _getRelCoords;
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
