#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include "cache.h"
#include <stdexcept>
#include <ArgumentParser.h>
#include <StringArgument.h>
 
int main(int argc, char *argv[]) {
  Argument::StringArgument dbArg("-db","value",string("newosm.sqlite"),"SQLite database name");
  Argument::ArgumentParser parser;
  parser.addArgument(dbArg);
  list<string> extra = parser.parse(argc,argv);
  list<string>::iterator it;

  string dbname = dbArg.getValue();

  osm_db::database sql(dbname);

  //  osm::Cache<osm::Node> nodes;


  for (it=extra.begin();it!=extra.end();it++) {
    osm::Node *nd = new osm::Node(atol(it->c_str()),sql);
    cout << *nd << endl;
    delete nd;
  }
  return 0;

}
