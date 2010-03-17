#include <string.h>
#include <glibmm/ustring.h>
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
    
    // Parse the entire document in one go:
    MySaxParser parser;
    parser.setDBconn(&sql);
    
    //      parser.set_substitute_entities(true); //
    if (filepath == "-") {
      parser.parse_stream(cin);
    } else {
      parser.parse_file(filepath);
    }
	sql.executenonquery("DELETE FROM way WHERE NOT id in (SELECT id FROM tag WHERE k in ('highway','boundary','route','natural'))");
	sql.executenonquery("DELETE FROM way WHERE id in (SELECT id FROM tag WHERE k='route' AND NOT v like 'ferry%')");
	sql.executenonquery("DELETE FROM way WHERE id in (SELECT id FROM tag WHERE k='natural' AND NOT v like 'coastline%')");
	sql.executenonquery("DELETE FROM relation WHERE id in (SELECT id FROM tag WHERE k='type' AND NOT v in ('boundary','restriction','multipolygon')");
	sql.executenonquery("DELETE FROM node WHERE NOT id IN (SELECT id FROM tag UNION SELECT id FROM nd UNION SELECT id FROM member)");
	
    sql.executenonquery("INSERT OR REPLACE INTO neighbor (way,id1,id2) SELECT DISTINCT way,id1,id2 FROM nb");
    sql.executenonquery("INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp");
    sql.executenonquery("UPDATE node SET x=round((lon+90)*20),y=round((lat+180)*20) WHERE id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)");
  } catch(const xmlpp::exception& ex) {
    cout << "libxml++ exception: " << ex.what() << endl;
    return 1;
  } catch (const sqlite3x::database_error& ex) {
    cout << "Exception in sqlite: " << ex.what() <<endl;
    return 1;
  } catch (const Glib::ustring &ex) {
     cout << "Exception in program: " << ex <<endl;
 }
  
  return 0;
}
