#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <string.h>
#include <sqlite3x.hpp>

#include <fstream>
#include <iostream>

#include "myparser.h"

using namespace sqlite3x;
using namespace std;

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

  // Parse the entire document in one go:
  try
    {
      MySaxParser parser;
      //      parser.set_substitute_entities(true); //
	  if (filepath == "-") {
	    parser.parse_stream(cin);
	  } else {
		parser.parse_file(filepath);
	  }
    }
  catch(const xmlpp::exception& ex)
    {
      cout << "libxml++ exception: " << ex.what() << endl;
	  return 1;
    }

  try {
    sqlite3_connection sql(dbname);
	ifstream schema;
	char regel[2048];
	schema.open("schema.sqlite.txt");
	while (schema.good()) {
	    schema.getline(regel,2047);
		cout << regel << endl;
		if (strncmp(regel,"CREATE",6)==0) {
  		   sql.executenonquery(regel);
		}
	}
	schema.close();
  } catch (const exception &ex) {
    cout << "Exception in sqlite: " << ex.what() <<endl;
	return 1;
  }

  // Demonstrate incremental parsing, sometimes useful for network connections:
  {
    //std::cout << "Incremental SAX Parser:" << std:endl;

    std::ifstream is(filepath.c_str());
    char buffer[BUFFERSIZE];

    MySaxParser parser;
    do {
      is.read(buffer, (BUFFERSIZE-1));
      Glib::ustring input(buffer, is.gcount());
      
      parser.parse_chunk(input);
    }
    while(is);
    
    parser.finish_chunk_parsing();
  }
  
  
  return 0;
}
