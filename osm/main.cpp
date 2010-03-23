#include <cstring>
#include <glibmm/ustring.h>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <ArgumentParser.h>
#include <StringArgument.h>
#include <BooleanArgument.h>

#include "myparser.h"
#include "osm_db.h"

using namespace std;
using namespace osm_db;

#define BUFFERSIZE (1024)

int main(int argc, char* argv[])
{
  Argument::StringArgument dbArg("-db","value",string("newosm.sqlite"),"SQLite database name");
  Argument::StringArgument schemaArg("-schema","value",string("schema.sqlite.txt"),"schema definition file");
  Argument::BooleanArgument newArg("-new","Create new database");
  Argument::StringArgument fileArg("-file","value",string("-"),"Input file (- = stdin)");
  
  Argument::ArgumentParser parser;
  parser.addArgument(dbArg);
  parser.addArgument(schemaArg);
  parser.addArgument(newArg);  
  parser.addArgument(fileArg);
  list<string> extra = parser.parse(argc,argv);
  
  string filepath = fileArg.getValue();
  string dbname = dbArg.getValue();
  string schema = schemaArg.getValue();
  bool nieuw = newArg.getValue();

  if (nieuw)
    unlink(dbname.c_str());

/*  
  cout << "argc = " << argc << endl;
  if (argc > 2) {
    filepath = argv[1];
    dbname = argv[2];
  } else {
    if (argc > 1 )
      filepath = argv[1]; //Allow the user to specify a different XML file to parse.
    else 
      filepath = "-";  // use stdin when no argument
    dbname = "newosm.sqlite";
  }
*/

  try {
    database sql(dbname);
    sql.setupSchemas(schema);
    sql.initializeFill();
    
    // Parse the entire document in one go:
    osmparser::MySaxParser parser;
    parser.setDBconn(&sql);
    //    parser.set_substitute_entities(true);

    if (filepath == "-") {
      parser.parse_stream(cin);
    } else {
      parser.parse_file(filepath);
    }
    cout << "Starting postprocessing" << endl;
    sql.postprocess();
  } catch(const xmlpp::exception& ex) {
    cout << "libxml++ exception: " << ex.what() << endl;
    return 1;
  } catch (const sqlite3x::database_error& ex) {
    cout << "Exception in sqlite: " << ex.what() <<endl;
    return 1;
  } catch (const Glib::ustring &ex) {
    cout << "Exception in parser: " << ex <<endl;
  } catch (const std::exception &ex) {
    cout << "Exception in program: " << ex.what() <<endl;
  }
  
  return 0;
}
