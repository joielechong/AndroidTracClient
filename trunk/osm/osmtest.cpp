#include "osm.h"
#include "osm_db.h"
#include <cstdlib>
#include <string>
#include <sstream>
#include "cache.h"
#include <stdexcept>
#include <ArgumentParser.h>
#include <StringArgument.h>
 
int main() {
  Argument::StringArgument dbArg("-db","value",string("newosm.sqlite"),"SQLite database name");
  Argument::ArgumentParser parser;
  parser.addArgument(dbArg);
  list<string> extra = parser.parse(argc,argv);
  string dbname = dbArg.getValue();

  database sql(dbname);
  osm::Cache<Node> nodes;

  osm::Node *nd = new Node(123357,sql);
  cout << *nd << endl;
  return 0;

}
