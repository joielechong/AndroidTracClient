#include "osm_db.h"
#include <string>

namespace osm_db {

using namespace std;
using namespace sqlite3x;

database::database(string naam) {
  sql = new sqlite3_connection(naam);
  createNode = new sqlite3_command(*sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
}

database::~database() {
  delete createNode;
  sql.close();
  sql = NULL;
}
}
