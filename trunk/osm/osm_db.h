#ifndef _OSM_DB_H
#define _OSM_DB_H

#include <string>
#include <sqlite3x.hpp>

namespace osm_db {

  using namespace sqlite3x;
  using namespace std;

  class database {
  public:
    database(string naam);
    ~database();

    inline void executenonquery(string query) {con->executenonquery(query);}

  private: 
    sqlite3_connection *con;
    sqlite3_command *createNode;
    sqlite3_command *createWay;
    sqlite3_command *createRelation;
    sqlite3_command *createTag;
    sqlite3_command *createNd;
    sqlite3_command *createMember;
  };
}

#endif
