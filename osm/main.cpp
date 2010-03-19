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

#include <math.h>
double pi = 0;
double radius = 6378137;
double drad = 21385;

double grootcirkel(double lat1,double lon1,double lat2,double lon2) {
  if (pi == 0)
    pi = atan2(0,1)*2;
  return (radius-drad*(sin((lat1+lat2)*pi/360)))*2*asin(sqrt((pow(sin((lat2-lat1)*pi/360),2)+cos(lat1*pi/180)*cos(lat2*pi/180)*pow(sin((lon2-lon1)*pi/360),2))));
}

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
    sql.setupSchemas("schema.sqlite.txt");
    sql.initializeFill();
    
    // Parse the entire document in one go:
    MySaxParser parser;
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
    cout << "Exception in program: " << ex <<endl;
  }
  
  return 0;
}
