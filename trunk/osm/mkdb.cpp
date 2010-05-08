#include "osm_db.h"
#include <cstring>
#include <cstdio>
#include <glibmm/ustring.h>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <ArgumentParser.h>
#include <ArgumentGroup.h>
#include <StringArgument.h>
#include <BooleanArgument.h>
#include <ListArgument.h>
#include <SocketHandler.h>
#include <StdoutLog.h>
#include "gzstream.h"
#include "myparser.h"
#include "osmapi.h"

using namespace std;
using namespace osm_db;
using namespace GZSTREAM_NAMESPACE;

class sql_commands {
public:
  string apistr;
  string sqlcmd;
};

sql_commands fixups[] = {
  {"relation/%ld","SELECT id FROM member GROUP BY id HAVING count(seq)-1 != max(seq)"},
  {"relation/%ld","SELECT DISTINCT ref FROM member WHERE type='relation' AND NOT ref IN (SELECT id FROM relation)"},
  {"relation/%ld","SELECT DISTINCT ref FROM member WHERE type='relation' AND NOT ref IN (SELECT id FROM relation)"},
  {"way/%ld/full","SELECT DISTINCT ref FROM member WHERE type='way' AND NOT ref IN (SELECT id FROM way)"},
  {"node/%ld","SELECT DISTINCT ref FROM member WHERE type='node' AND NOT ref IN (SELECT id FROM node)"},
  {"way/%ld/full","SELECT id FROM nd GROUP BY id HAVING count(seq)-1 != max(seq)"},
  {"node/%ld","SELECT DISTINCT id FROM nd WHERE NOT ref IN (SELECT id FROM node)"},
  {"",""}
};

#define BUFFERSIZE (1024)

int main(int argc, char* argv[])
{
  Argument::StringArgument dbArg("-db","value","\tSQLite database name",string("newosm.sqlite"),false);
  Argument::StringArgument schemaArg("-schema","value","schema definition file",string(DATADIR)+string("/schema.sqlite.txt"),false);
  Argument::BooleanArgument updArg("-update","\tUpdate the database");
  Argument::StringArgument apiArg("-api","value","\tOnline API request e.g. node/nodeid",false);
  Argument::BooleanArgument fixArg("-fix","\t\tcompletes incomplete relations and ways (implied by -new)");
  Argument::BooleanArgument postArg("-post","\t\tPerform postprocessing on the database (implied by -new and -fix)");
  Argument::BooleanArgument helpArg("-help","\t\tHelp on usage");
  Argument::BooleanArgument newArg("-new","\t\tCreate new database");
  Argument::ListArgument extraArg("file","\tFilename[s] to process (none or - implies stdin)",false);


  Argument::ArgumentParser argparser;

  argparser.addArgument(helpArg);
  argparser.addArgument(newArg);
  argparser.addArgument(updArg);
  argparser.addArgument(dbArg);
  argparser.addArgument(schemaArg);
  argparser.addArgument(fixArg);  
  argparser.addArgument(postArg);  
  argparser.addArgument(apiArg);  
  argparser.addArgument(extraArg);  

  list<string> remaining = argparser.parse(argc,argv);
  if (!argparser.isValid()) {
    argparser.printErrors(cout);
    return 1;
  }
  
  string dbname = dbArg.getValue();
  string schema = schemaArg.getValue();
  bool nieuw = newArg.getValue();
  bool update = updArg.getValue();
  bool fixup = fixArg.getValue();
  bool post = postArg.getValue();
  bool helponly = helpArg.getValue();
  string apistr = apiArg.getValue();
  list<string>extra = extraArg.getValue();

  list<string>::iterator it;

  if (helponly) {
    argparser.printUsage(cout);
    return 0;
  }

  if ((nieuw == update)) {
    cerr << "Either -new or -update must be provided" << endl;
    return 1;
  }
  
  if (nieuw) {
    unlink(dbname.c_str());
    if (extra.size() == 0)
      extra.push_back("-");
    fixup = true;
  }

  if (fixup)
    post = true;
  
  try {
    database sql(dbname);

    if (nieuw) 
      sql.setupSchemas(schema);

    if (update) 
      sql.update(true);
    
    sql.initializeFill();
    sql.initTemp();
    
    // Parse the entire document in one go:
    osmparser::OSMParser osmparser;
    osmparser.setDBconn(&sql);
    //    osmparser.set_substitute_entities(true);
    
    if (apistr != "") {
      string buf;
      
      SocketHandler h(NULL);
      osmapi::osmapiSocket sock(h, apistr);
      h.Add(&sock);
      while (h.GetCount()) {
	h.Select(1, 0);
      }
      buf = sock.GetData();
      //	cout << buf;
      osmparser.parse_memory(buf);
    } else  {
      for (it=extra.begin();it!=extra.end();it++) {
	string filepath = *it;
	if (filepath == "-") {
	  osmparser.parse_stream(cin);
	} else {
	  if (filepath.find(".gz") == filepath.length()-2) {
	    igzstream input(filepath.c_str());
	    osmparser.parse_stream(input);
	  } else 
	    osmparser.parse_file(filepath);
	}
      }
    }

    if (nieuw) {
      sql.setBoundaries();
    }
    
    if (fixup) {
      for(int i=0;fixups[i].apistr != ""; i++) {
	vector<long> ids;
	vector<long>::iterator id;
	cout << "Fixup: " << fixups[i].sqlcmd <<endl;
	sql.getids(fixups[i].sqlcmd,ids);
	for(id=ids.begin();id != ids.end();id++) {
	  char apistring[1024];
	  sprintf(apistring,fixups[i].apistr.c_str(),*id);
	  
	  string buf;
	  cout << "        " << apistring  << endl;
	  SocketHandler h(NULL);
	  osmapi::osmapiSocket sock(h, apistring);
	  h.Add(&sock);
	  while (h.GetCount()) {
	    h.Select(1, 0);
	  }
	  buf = sock.GetData();
	  //	cout << buf;
	  osmparser.parse_memory(buf);
	}
      }    
    }
    
    if (post) {
      cout << "Starting postprocessing" << endl;
      sql.postprocess();
    }

  } catch(const xmlpp::exception& ex) {
    cerr << "libxml++ exception: " << ex.what() << endl;
    return 1;
  } catch (const sqlite3x::database_error& ex) {
    cerr << "Exception in sqlite: " << ex.what() <<endl;
    return 1;
  } catch (const osm_db_error& ex) {
    cerr << "Exception in osm_db: " << ex.what() <<endl;
    return 1;
  } catch (const Glib::ustring &ex) {
    cerr << "Exception in parser: " << ex <<endl;
  } catch (const std::exception *ex) {
    cerr << "Exception in program: " << ex->what() <<endl;
  }
  
  return 0;
}
