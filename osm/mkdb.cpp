#include "osm_db.h"
#include <cstring>
#include <cstdio>
#include <vector>
#include <glibmm/ustring.h>
#include <fstream>
#include <sstream>
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

#define MAXELEM 50

using namespace std;
using namespace osm_db;
using namespace GZSTREAM_NAMESPACE;

class sql_commands {
public:
  string element;
  string sqlcmd;
};

sql_commands fixups[] = {
  {"relation","SELECT id FROM member GROUP BY id HAVING count(seq)-1 != max(seq)"},
  {"relation","SELECT DISTINCT ref FROM member WHERE type='relation' AND NOT ref IN (SELECT id FROM relation)"},
  {"relation","SELECT DISTINCT ref FROM member WHERE type='relation' AND NOT ref IN (SELECT id FROM relation)"},
  {"way","SELECT DISTINCT ref FROM member WHERE type='way' AND NOT ref IN (SELECT id FROM way)"},
  {"way","SELECT id FROM nd GROUP BY id HAVING count(seq)-1 != max(seq)"},
  {"node","SELECT DISTINCT ref FROM member WHERE type='node' AND NOT ref IN (SELECT id FROM node)"},
  {"node","SELECT DISTINCT ref FROM nd WHERE NOT ref IN (SELECT id FROM node)"},
  {"",""}
};

string postprocesses[] = {
  "UPDATE tag SET v='associatedStreet' WHERE type='relation' AND k='type' AND v='relatedStreet'",
  "UPDATE relation SET donotdelete='true' WHERE id IN (SELECT id FROM relationtag WHERE k='type' AND v in ('boundary','restriction','associatedStreet','boundary_segment')) AND donotdelete!='true'",
  "UPDATE relation SET donotdelete='true' WHERE id IN (SELECT id FROM relationtag WHERE (k='boundary' AND v='administrative') OR (k='admin_level')) AND id IN (SELECT id FROM relationtag WHERE k='type' AND v='multipolygon') AND donotdelete!='true'",
  "UPDATE relation SET donotdelete='true' WHERE id in (SELECT ref FROM member WHERE type='relation') and donotdelete !='true'",
  "DELETE FROM relation WHERE donotdelete='false'",
  //  "DELETE FROM relation WHERE (id in (SELECT id FROM relationtag WHERE k='type' AND NOT v in ('boundary','restriction','multipolygon','associatedStreet','boundary_segment'))) OR id IN (SELECT ref FROM member WHERE type='relation')",
  //  "DELETE FROM relation WHERE id IN (SELECT id FROM relationtag WHERE k='type' AND v='multipolygon' AND NOT id IN (SELECT relation.id FROM relation,relationtag as tag1,relationtag as tag2 where tag1.k='type' and tag1.v='multipolygon' and tag1.id=relation.id and ((tag2.k='boundary' and tag2.v='administrative') or tag2.k='admin_level') and tag2.id=tag1.id))",
  
  "update way set donotdelete='true' where id in (select ref from member where type='way') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='highway' or k='boundary') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='natural' and v='coastline') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='route' and v='ferry') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k like 'addr:%' or k like 'is_in:%') and donotdelete != 'true'",
  "delete from way where donotdelete='false'",
//    "DELETE FROM way WHERE NOT id in (SELECT id FROM waytag WHERE k in ('highway','boundary','route','natural') OR k like 'addr:%' OR k like 'is_in%' UNION SELECT ref FROM member WHERE type = 'way')",

  "update node set donotdelete='true' where id in (select id from nodetag) and donotdelete != 'true'",
  "update node set donotdelete='true' where id in (select ref from nd) and donotdelete != 'true'",
  "update node set donotdelete='true' where id in (select ref from member where type='node') and donotdelete != 'true'",
  "delete from node where donotdelete='false'",
//    "DELETE FROM node WHERE NOT id IN (SELECT id FROM nodetag UNION SELECT ref FROM nd UNION SELECT ref FROM member WHERE type='node')",

  "UPDATE tag SET v='yes' WHERE k IN ('bridge','oneway','tunnel') AND v IN ('1','YES','true','Yes')",
  "DELETE FROM tag WHERE k IN ('bridge','oneway','tunnel') AND v IN ('NO','FALSE','No','False','no','ny','false')",
  "UPDATE node SET x=osmcalc_x(lon),y=osmcalc_y(lat) WHERE x is null and id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)",
  "INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp",
  "INSERT OR REPLACE INTO adressen SELECT id,'node' AS type,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:country') AS country,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:city') AS city,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:street') AS street,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:postcode') AS postcode FROM node WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL",
  "INSERT OR REPLACE INTO adressen SELECT id,'way' AS type,(SELECT v FROM waytag WHERE id=way.id AND k='addr:country') AS country,(SELECT v FROM waytag WHERE id=way.id AND k='addr:city') AS city,(SELECT v FROM waytag WHERE id=way.id AND k='addr:street') AS street,(SELECT v FROM waytag WHERE id=way.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM waytag WHERE id=way.id AND k='addr:postcode') AS postcode FROM way WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL",
  "insert or replace into neighbor (id1,id2,way,distance) SELECT id1,id2,way,osmdistance(n1.lat,n1.lon,n2.lat,n2.lon) from nb,node as n1,node as n2 where n1.id=id1 and n2.id=id2",
  "vacuum",
  ""
};

#define BUFFERSIZE (1024)

static void postprocess(database &sql) {

  for(int i = 0; postprocesses[i]!= "";i++) 
    sql.executenonquery(postprocesses[i]);
}

static string apiRequest(string apistr) {
  SocketHandler h(NULL);
  osmapi::osmapiSocket sock(h, apistr);
  h.Add(&sock);
  while (h.GetCount()) {
    h.Select(1, 0);
  }
  string status = sock.GetStatus();
  string statusText = sock.GetStatusText();
  cout << "Status = " << status << endl;
  if (status == "404" || status == "410") 
    throw out_of_range("Een of meer id's ontbreken");
  else if (status != "200")
    throw runtime_error("apiRequest returned status: "+status+" "+statusText);
  
  string buf = sock.GetData();
  //	cout << buf;
  return buf;
}

static void splitRequest(database &sql,osmparser::OSMParser &p,string elemType,string apistr) {
  unsigned int start = apistr.find("=")+1;
  while (start < apistr.length()) {
    unsigned int komma = apistr.find(",",start);
    if (komma == string::npos)
      komma = apistr.length()+1;
    komma--;
    stringstream s;
    s << elemType << "/" << apistr.substr(start,komma-start);
    try {
      string buf = apiRequest(s.str());
      p.parse_memory(buf);
    } catch ( const out_of_range &ex) {
      sql.delElem(s.str());
    }
  }
}

int main(int argc, char* argv[])
{
  Argument::StringArgument dbArg("-db","value","\tSQLite database name",string("newosm.sqlite"),false);
  Argument::StringArgument schemaArg("-schema","value","schema definition file",string(DATADIR)+string("/schema.sqlite.txt"),false);
  Argument::BooleanArgument updArg("-update","\tUpdate the database");
  Argument::StringArgument apiArg("-api","value","\tOnline API request e.g. node/nodeid",false);
  Argument::BooleanArgument fixArg("-fix","\t\tcompletes incomplete relations and ways");
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
    if (remaining.size() == 0)
      remaining.push_back("-");
    post = true;
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
      try {
	string buf = apiRequest(apistr);
	osmparser.parse_memory(buf);
      } catch (const out_of_range &ex) {
	cerr << ex.what() << endl;
      }
    } else  {
      for (it=remaining.begin();it!=remaining.end();it++) {
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
      for(int i=0;fixups[i].element != ""; i++) {
	vector<long> ids;
	vector<long>::iterator id;
	string elemtype = fixups[i].element;
	cout << "Fixup: " << fixups[i].sqlcmd <<endl;
	sql.getids(fixups[i].sqlcmd,ids);

	stringstream apistring;
	apistring.str("");
	int count = 0;
	for(id=ids.begin();id != ids.end();id++) {
	  if (count == 0) {
	    apistring << elemtype << "s?" << elemtype << "s=" << *id;
	  } else
	    apistring << "," << *id;

	  if (count++ == ((MAXELEM)-1)) {
	    cout << "        " << apistring.str()  << endl;
	    try {
	      string buf = apiRequest(apistring.str());
	      osmparser.parse_memory(buf);
	    } catch (const out_of_range &ex) {
	      cerr << ex.what() << endl;
	      splitRequest(sql,osmparser,elemtype,apistring.str());
	      //	      sql.delElem(apistring);
	    }
	    count = 0;
	    apistring.str("");
	  }
 	}
	if (count != 0) {
	  cout << "        " << apistring.str()  << endl;
	  try {
	    string buf = apiRequest(apistring.str());
	    osmparser.parse_memory(buf);
	  } catch (const out_of_range &ex) {
	    cerr << ex.what() << endl;
	    splitRequest(sql,osmparser,elemtype,apistring.str());
	    //	      sql.delElem(apistring);
	  }
	}
      }    
    }
    
    if (post) {
      cout << "Starting postprocessing" << endl;
      postprocess(sql);
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
  } catch (const std::exception &ex) {
    cerr << "Exception in program: " << ex.what() <<endl;
  }
  
  return 0;
}
