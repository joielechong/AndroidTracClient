#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <string.h>
#include "osm_db.h"
#include "osm.h"

#include <fstream>
#include <iostream>

#include "myparser.h"

using namespace std;
using namespace osm_db;

#define BUFFERSIZE (1024)

int main(int argc, char* argv[])
{
  string filepath;
  string dbname;
  
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
  
  try {
    database sql(dbname);
    ifstream schema;
    char regel[2048];
    schema.open("schema.sqlite.txt");
    while (schema.good()) {
      schema.getline(regel,2047);
      //      cout << regel << endl;
      if ((strncmp(regel,"CREATE",6) == 0) || (strncmp(regel,"PRAGMA",5) == 0)) {
	sql.executenonquery(regel);
      }
    }
    schema.close();
    
    // Parse the entire document in one go:
    MySaxParser parser;
    parser.setDBconn(&sql);
    
    //      parser.set_substitute_entities(true); //
    if (filepath == "-") {
      parser.parse_stream(cin);
    } else {
      parser.parse_file(filepath);
    }
    sql.executenonquery("INSERT OR REPLACE INTO neighbor (way,id1,id2) SELECT DISTINCT way,id1,id2 FROM nb");
    sql.executenonquery("INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp");
    sql.executenonquery("UPDATE node SET x=round((lon+90)*20),y=round((lat+180)*20) WHERE id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)");
  } catch(const xmlpp::exception& ex) {
    cout << "libxml++ exception: " << ex.what() << endl;
    return 1;
  } catch (const exception &ex) {
    cout << "Exception in sqlite: " << ex.what() <<endl;
    return 1;
  }
  
  return 0;
}
