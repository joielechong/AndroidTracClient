#include "osm_sql3db.h"
#include "myparser.h"
#include <sqlite3x.hpp>
#include <vector>

namespace osm_db {
  using namespace sqlite3x;
  using namespace std;

  void sql3database::getids(string &sqlcmd,vector<long> &ids) {
    sqlite3_command getids(*_sql,sqlcmd);
    sqlite3_cursor cur(getids.executecursor());

    ids.clear();
    while (cur.step())
      ids.push_back(cur.getint64(0));
    
  }

}
