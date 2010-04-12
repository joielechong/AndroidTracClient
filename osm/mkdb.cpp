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
  Argument::BooleanArgument updArg("-update","Update the database");
  Argument::BooleanArgument postArg("-postonly","Perform only postprocessing on the database");
  Argument::BooleanArgument newArg("-new","Create new database");
  Argument::StringArgument fileArg("-file","value",string("-"),"Input file (- = stdin)");
  
  Argument::ArgumentParser argparser;
  argparser.addArgument(dbArg);
  argparser.addArgument(schemaArg);
  argparser.addArgument(newArg);  
  argparser.addArgument(updArg);  
  argparser.addArgument(postArg);  
  argparser.addArgument(fileArg);
  list<string> extra = argparser.parse(argc,argv);
  
  string filepath = fileArg.getValue();
  string dbname = dbArg.getValue();
  string schema = schemaArg.getValue();
  bool nieuw = newArg.getValue();
  bool update = updArg.getValue();
  bool postonly = postArg.getValue();

  if (nieuw)
    unlink(dbname.c_str());

  try {
    database sql(dbname);

    if (nieuw) 
      sql.setupSchemas(schema);

    if (update) 
      sql.update(true);
    
    sql.initializeFill();
    sql.initTemp();
    
    if (nieuw || update) {
      // Parse the entire document in one go:
      osmparser::MySaxParser osmparser;
      osmparser.setDBconn(&sql);
      //    osmparser.set_substitute_entities(true);
      
      if (filepath == "-") {
	osmparser.parse_stream(cin);
      } else {
	osmparser.parse_file(filepath);
      }
    }
    if (nieuw || postonly) {
      cout << "Starting postprocessing" << endl;
      sql.postprocess();
    }
  } catch(const xmlpp::exception& ex) {
    cout << "libxml++ exception: " << ex.what() << endl;
    return 1;
  } catch (const sqlite3x::database_error& ex) {
    cout << "Exception in sqlite: " << ex.what() <<endl;
    return 1;
  } catch (const Glib::ustring &ex) {
    cout << "Exception in parser: " << ex <<endl;
  } catch (const std::exception *ex) {
    cout << "Exception in program: " << ex->what() <<endl;
  }
  
  return 0;
}
