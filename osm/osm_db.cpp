#include "osm_db.h"
#include <string>

namespace osm_db {

using namespace std;
using namespace sqlite3x;

osm_db::osm_db(string naam) {
  sql = new sqlite3_connection(naam);
  createNode = new sqlite3_command(*sql,"INSERT INTO node (id,version,lat,lon) VALUES (?,?,?,?)");
}

osm_db::~osm_db() {
  delete createNode;
  sql.close();
  sql = NULL;
}
}
