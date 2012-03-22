#include "osm_sql3db.h"
#include <cstring>
#include <cstdio>
#include <vector>
#include <glibmm/ustring.h>
#include <fstream>
#include <sstream>
#include <iostream>
#include <stdexcept>
#include <SocketHandler.h>
#include <StdoutLog.h>
#include <boost/program_options.hpp>
#include "gzstream.h"
#include "osmparser.h"
#include "osmapi.h"

#define MAXELEM 75

namespace po = boost::program_options;

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
  
  "update way set donotdelete='true' where id in (select ref from member where type='way') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='highway' or k='boundary') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='natural' and v='coastline') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k ='route' and v='ferry') and donotdelete != 'true'",
  "update way set donotdelete='true' where id in (select id from waytag where k like 'addr:%' or k like 'is_in:%') and donotdelete != 'true'",
  "delete from way where donotdelete='false'",

  "update node set donotdelete='true' where id in (select id from nodetag) and donotdelete != 'true'",
  "update node set donotdelete='true' where id in (select ref from nd) and donotdelete != 'true'",
  "update node set donotdelete='true' where id in (select ref from member where type='node') and donotdelete != 'true'",
  "delete from node where donotdelete='false'",

  "DELETE FROM tag where k='bicycle:oneway' and  type='way' and id in (select id from waytag where k='oneway:bicycle')",
  "DELETE FROM tag where k='cycleway:oneway' and  type='way' and id in (select id from waytag where k='oneway:bicycle')",
  "DELETE FROM tag where k='access:bicycle:oneway' and  type='way' and id in (select id from waytag where k='oneway:bicycle')",
  "UPDATE tag set k='oneway:bicycle' WHERE k IN ('bicycle:oneway','access:bicycle:oneway','cycleway:oneway')",

  "DELETE FROM tag where k='moped:oneway' and  type='way' and id in (select id from waytag where k='oneway:moped')",
  "DELETE FROM tag where k='access:moped:oneway' and  type='way' and id in (select id from waytag where k='oneway:moped')",
  "UPDATE tag set k='oneway:moped' WHERE k IN ('moped:oneway','access:moped:oneway')",

  "DELETE FROM tag where k='moped_A:oneway' and  type='way' and id in (select id from waytag where k='oneway:moped_A')",
  "DELETE FROM tag where k='access:moped_A:oneway' and  type='way' and id in (select id from waytag where k='oneway:moped_A')",
  "UPDATE tag set k='oneway:moped_A' WHERE k IN ('moped_A:oneway','access:moped_A:oneway')",

  "DELETE FROM tag where k='foot:oneway' and  type='way' and id in (select id from waytag where k='oneway:foot')",
  "DELETE FROM tag where k='access:foot:oneway' and  type='way' and id in (select id from waytag where k='oneway:foot')",
  "UPDATE tag set k='oneway:foot' WHERE k IN ('foot:oneway','access:foot:oneway')",

  "DELETE FROM tag where k='motor_vehicle:oneway' and  type='way' and id in (select id from waytag where k='oneway:motor_vehicle')",
  "DELETE FROM tag where k='access:motor_vehicle:oneway' and  type='way' and id in (select id from waytag where k='oneway:motor_vehicle')",
  "UPDATE tag set k='oneway:motor_vehicle' WHERE k IN ('motor_vehicle:oneway','access:motor_vehicle:oneway')",

  "DELETE FROM tag where k='motorcar:oneway' and  type='way' and id in (select id from waytag where k='oneway:motorcar')",
  "DELETE FROM tag where k='access:motorcar:oneway' and  type='way' and id in (select id from waytag where k='oneway:motorcar')",
  "UPDATE tag set k='oneway:motorcar' WHERE k IN ('motorcar:oneway','access:motorcar:oneway')",

  "DELETE FROM tag where k='motorcylce:oneway' and  type='way' and id in (select id from waytag where k='oneway:motorcycle')",
  "DELETE FROM tag where k='access:motorcycle:oneway' and  type='way' and id in (select id from waytag where k='oneway:motorcycle')",
  "UPDATE tag set k='oneway:motorcycle' WHERE k IN ('motorcycle:oneway','access:motorcycle:oneway')",

  "DELETE FROM tag where k='emergency:oneway' and  type='way' and id in (select id from waytag where k='oneway:emergency')",
  "DELETE FROM tag where k='access:emergency:oneway' and  type='way' and id in (select id from waytag where k='oneway:emergency')",
  "UPDATE tag set k='oneway:emergency' WHERE k IN ('emergency:oneway','access:emergency:oneway')",

  "DELETE FROM tag where k='bus:oneway' and  type='way' and id in (select id from waytag where k='oneway:bus')",
  "DELETE FROM tag where k='access:bus:oneway' and  type='way' and id in (select id from waytag where k='oneway:bus')",
  "UPDATE tag set k='oneway:bus' WHERE k IN ('bus:oneway','access:bus:oneway')",

  "DELETE FROM tag where k='psv:oneway' and  type='way' and id in (select id from waytag where k='oneway:psv')",
  "DELETE FROM tag where k='access:psv:oneway' and  type='way' and id in (select id from waytag where k='oneway:psv')",
  "UPDATE tag set k='oneway:psv' WHERE k IN ('psv:oneway','access:psv:oneway')",

  "DELETE FROM tag where k='taxi:oneway' and  type='way' and id in (select id from waytag where k='oneway:taxi')",
  "DELETE FROM tag where k='access:taxi:oneway' and  type='way' and id in (select id from waytag where k='oneway:taxi')",
  "UPDATE tag set k='oneway:taxi' WHERE k IN ('taxi:oneway','access:taxi:oneway')",

  "UPDATE tag SET v='yes' WHERE (k IN ('bridge','tunnel','motorroad','bicycle','foot') OR k like 'oneway%') AND v IN ('1','YES','true','Yes')",
  "UPDATE tag SET v='-1' WHERE k LIKE 'oneway%' AND v = 'reverse'",
  "DELETE FROM tag WHERE k IN ('bridge','oneway','tunnel','motorroad') AND v IN ('NO','FALSE','No','False','no','ny','false')",
  "UPDATE tag SET v='no' WHERE k LIKE 'oneway:%' AND v IN ('0','NO','False','No','FALSE','false')",
  "UPDATE tag set v='opposite' WHERE k='cycleway' and v like 'opposite_%'",

  "UPDATE node SET x=osmcalc_x(lon),y=osmcalc_y(lat) WHERE x is null and id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)",

  "INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp",
  "INSERT OR REPLACE INTO adressen (id,type,country,city,street,housenumber,postcode,intpolway,assocway) SELECT id,'node' AS type,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:country') AS country,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:city') AS city,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:street') AS street,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:postcode') AS postcode, (SELECT wt.id FROM nd,waytag as wt WHERE nd.ref=node.id and nd.id=wt.id and wt.k='addr:interpolation') as intpolway,(select m1.ref from tag, member as m,relationtag as rt,member as m1 where tag.id=node.id and tag.type='node' and tag.k like 'addr:%' and m.type=tag.type and m.ref=tag.id and m.id=rt.id and rt.k='type' and rt.v='associatedStreet' and m1.id=rt.id and m1.role='street') as assocway FROM node WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL",

  "INSERT OR REPLACE INTO adressen (id,type,country,city,street,housenumber,postcode,assocway) SELECT id,'way' AS type,(SELECT v FROM waytag WHERE id=way.id AND k='addr:country') AS country,(SELECT v FROM waytag WHERE id=way.id AND k='addr:city') AS city,(SELECT v FROM waytag WHERE id=way.id AND k='addr:street') AS street,(SELECT v FROM waytag WHERE id=way.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM waytag WHERE id=way.id AND k='addr:postcode') AS postcode, (select m1.ref from tag, member as m,relationtag as rt,member as m1 where tag.id=way.id and tag.type='way' and tag.k like 'addr:%' and m.type=tag.type and m.ref=tag.id and m.id=rt.id and rt.k='type' and rt.v='associatedStreet' and m1.id=rt.id and m1.role='street') as assocway FROM way WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL",

  "vacuum",
  ""
};

#define BUFFERSIZE (1024)

static void postprocess(database &sql) {

  for(int i = 0; postprocesses[i]!= "";i++) 
    sql.executenonquery(postprocesses[i]);
}

static string apiRequest(string apistr,bool xapi,std::string host = "") {

// TODO
// recursie er uit halen

  if (xapi) {
    if (host == "")
      host="www.informationfreeway.org";
  } else {
    host = "api.openstreetmap.org";
  }

  SocketHandler h(NULL);
  osmapi::osmapiSocket sock(h, apistr,host);
  h.Add(&sock);
  while (h.GetCount()) {
    h.Select(1, 0);
  }
  string status = sock.GetStatus();
  string statusText = sock.GetStatusText();
  cout << "Status = " << status << endl;
  if (status == "404" || status == "410") 
    throw out_of_range("Een of meer id's ontbreken");
  else if (status == "302") {
    string location = sock.getLocation();
    cerr << "Location = " << location << endl;
    int c1 = location.find("/api/0.6");
    int c2 = location.find("//");
    string newhost = location.substr(c2+2,c1-c2-2);
    cerr << "Newhost = " << newhost << endl;
    return apiRequest(apistr,(1==1),newhost);
  } else if (status != "200")
    throw runtime_error("apiRequest returned status: "+status+" "+statusText);
  
  string buf = sock.GetData();
  //	cout << buf;
  return buf;
}

static void splitRequest(database &sql,osmparser::OSMParser &p,string elemType,string apistr,bool xapi) {
  unsigned int start = apistr.find("=")+1;
  while (start < apistr.length()) {
    unsigned int komma = apistr.find(",",start);
    if (komma == string::npos)
      komma = apistr.length()+1;
    stringstream s;
    s << elemType << "/" << apistr.substr(start,komma-start);
    start = komma+1;
    bool retry;
    int retrycount=0;
    do {
      try {
	retry=false;
	string buf = apiRequest(s.str(),xapi&&(retrycount<4));
	p.parse_memory(buf);
      } catch ( const out_of_range &ex) {
	sql.delElem(s.str());
      } catch (const runtime_error &ex) {
	retry = true;
	retrycount++;
	if (retrycount < 5) 
	  cout << "  retry: " << retrycount << endl;
      }
    } while (retry && retrycount < 5);
    if (retry)
      throw runtime_error("apiRequest failed after 5 retries");
  }
}

static void do_fixup(osmparser::OSMParser &osmparser,database &sql,bool xapi) {
  
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
	bool retry;
	int retrycount = 0;
	do {
	  try {
	    retry = false;
	    string buf = apiRequest(apistring.str(),xapi&&(retrycount<4));
	    osmparser.parse_memory(buf);
	  } catch (const out_of_range &ex) {
	    cerr << ex.what() << endl;
	    retry=false;
	    splitRequest(sql,osmparser,elemtype,apistring.str(),xapi);
	    //	      sql.delElem(apistring);
	  } catch (const runtime_error &ex) {
	    retry = true;
	    retrycount++;
	    if (retry < 5) 
	      cout << "  retry: " << retrycount << endl;
	  }
	} while (retry && retrycount < 5);
	if (retry)
	  throw runtime_error("apiRequest failed after 5 retries");
	count = 0;
	apistring.str("");
      }
    }
    if (count != 0) {
      cout << "        " << apistring.str()  << endl;
      bool retry;
      int retrycount = 0;
      do {
        try {
	  retry = false;
	  string buf = apiRequest(apistring.str(),xapi&&(retrycount<4));
	  osmparser.parse_memory(buf);
        } catch (const out_of_range &ex) {
	  cerr << ex.what() << endl;
	  retry=false;
	  splitRequest(sql,osmparser,elemtype,apistring.str(),xapi);
	//	sql.delElem(apistring);
	} catch (const runtime_error &ex) {
	  retry = true;
	  retrycount++;
	  if (retry < 5) 
	    cout << "  retry: " << retrycount << endl;
        }
      } while (retry && retrycount < 5);
      if (retry)
	throw runtime_error("apiRequest failed after 5 retries");
      count = 0;
      apistring.str("");
    }
  }    
}

int main(int argc, char* argv[])
{
  try {
    po::options_description desc("Geldige opties");
    desc.add_options()
      ("db", po::value<string>()->default_value("newosm.sqlite"), "SQLite database name")
      ("schema", po::value<string>()->default_value(string(DATADIR)+string("/schema.sqlite.txt")), "schema definition file")
      ("new","Create new database")
      ("update","Update database")
      ("fix","Completes incomplete relations and ways")
      ("post","Perform postprocessing on the database")
      ("api",po::value<string>(),"Online API request e.g. node/nodeid")
      ("xapi","Use the XAPI interface")
      ("help","Help om usage");

    po::options_description hidden("Hidden options");
    hidden.add_options()
      ("input-file", po::value< list<string> >(), "input file");
      
    po::positional_options_description p;
    p.add("input-file", -1);

    po::options_description cmdline_options;
    cmdline_options.add(desc).add(hidden);

    po::options_description visible("Aanroep mkdb [opties] file(s)");
    cmdline_options.add(desc);
    
    po::variables_map vm;
    po::store(po::command_line_parser(argc, argv).
	  options(cmdline_options).positional(p).run(), vm);
    po::notify(vm);
    
    if (vm.count("help")) {
      cout << visible << endl <<endl;
      return 0;
    }
/*
  Argument::StringArgument dbArg("-db","value","\tSQLite database name",string("newosm.sqlite"),false);
  Argument::StringArgument schemaArg("-schema","value","schema definition file",string(DATADIR)+string("/schema.sqlite.txt"),false);
  Argument::BooleanArgument updArg("-update","\tUpdate the database");
  Argument::StringArgument apiArg("-api","value","\tOnline API request e.g. node/nodeid",false);
  Argument::BooleanArgument xapiArg("-xapi","\tUse XAPI requests");
  Argument::BooleanArgument fixArg("-fix","\t\tcompletes incomplete relations and ways");
  Argument::BooleanArgument postArg("-post","\t\tPerform postprocessing on the database");
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
  argparser.addArgument(xapiArg);  
  argparser.addArgument(extraArg);  

  list<string> remaining = argparser.parse(argc,argv);
  if (!argparser.isValid()) {
    argparser.printErrors(cout);
    return 1;
  }
  
  list<string>extra = extraArg.getValue();
*/

  bool nieuw = (vm.count("new") > 0);
  bool update = (vm.count("update") > 0);
  bool fixup = (vm.count("fix") > 0);
  bool post = (vm.count("post") > 0);
  bool xapi = (vm.count("xapi") > 0);
  string dbname = vm["db"].as<string>();
  string schema = vm["schema"].as<string>();
  string apistr = vm["api"].as<string>();
  list<string>extra = vm["input-file"].as< list<string> >();
  
  list<string>::iterator it;

  if ((nieuw == update)) {
    cerr << "Either -new or -update must be provided" << endl;
    return 1;
  }
  
  if (nieuw) {
    unlink(dbname.c_str());
    if (extra.size() == 0)
      extra.push_back("-");
/*
      if (remaining.size() == 0)
      remaining.push_back("-");
*/
  }
   
  } catch (exception& e) {
    cerr << "error: " << e.what() << endl;
    return 1;
  } catch(...) {
    cerr << "Exception of unknown type!" << endl;
  }
  
  
  return 0;
  
  try {
    sql3database sql(dbname);
    
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
	string buf = apiRequest(apistr,xapi);
	osmparser.parse_memory(buf);
      } catch (const out_of_range &ex) {
	cerr << ex.what() << endl;
      }
    } else {
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

    if (nieuw)
      sql.setBoundaries();
    
    if (fixup)
      do_fixup(osmparser,sql,xapi);
    
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
  }
  return 0;
}
