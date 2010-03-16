#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>

namespace osm_db {

  using namespace sqlite3x;

  class database {
  public:
    database(std::string naam);
    ~database();

    inline void executenonquery(std::string query) {sql->executenonquery(query);}

  private: 
    sqlite3_connection *sql;
    sqlite3_command *createNode;
    sqlite3_command *createWay;
    sqlite3_command *createRelation;
    sqlite3_command *createTag;
    sqlite3_command *createNd;
    sqlite3_command *createMember;
  };

}

#endif
