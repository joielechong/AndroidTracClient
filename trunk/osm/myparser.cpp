#include "myparser.h"
#include <iostream>
#include "osm_db.h"
#include <string>
#include <glibmm/ustring.h>
#include <stdlib.h>
#include <string.h>

MySaxParser::MySaxParser() : depth(0),counter(0), lastid(0), ndcnt(0), memcnt(0), xmlpp::SaxParser() {}

MySaxParser::~MySaxParser() {}

void MySaxParser::printCounts() { 
  long nodes,ways,rels,bounds,tags,nds,mems; 
  _con->getCounts(nodes,ways,rels,bounds,tags,nds,mems); 
  std::cout << counter << ": " << nodes << " nodes " << ways << " ways " << rels << " relations " << tags << " tags " << nds << " nds " << mems << " members" << std::endl; 
}

void MySaxParser::on_start_document() {
  //  std::cout << "on_start_document()" << std::endl;
  _con->begin();
}

void MySaxParser::on_end_document() {
  //  std::cout << "on_end_document()" << std::endl;
  _con->commit();
  getCounts();
}

void MySaxParser::on_start_element(const Glib::ustring& name,const AttributeList& attributes) {
  depth++;
  //  std::cout << "node: name=" << name.c_str() << "(" << depth << ")" << std::endl;
  
  // Store attributes:
  long id,ref;
  int version;
  double lat,lon;
  std::string type,role,k,v;
  
  for(xmlpp::SaxParser::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter) {
    //    std::cout << "  Attribute: " << iter->name << " = " << iter->value.c_str() << std::endl;
    switch (depth) {
    case 2:
      if (iter->name == "id") {
	id = atol(iter->value.c_str());
	lastid = id;
	memcnt = 0;
	ndcnt = 0;
      } else if (iter->name == "version") {
	version = atol(iter->value.c_str());
      } else if (iter->name == "lat") {
	lat = atof(iter->value.c_str());
      } else if (iter->name == "lon") {
	lon = atof(iter->value.c_str());
      }
      break;
    case 3:
      if (iter->name == "k") {
	k = iter->value.c_str();
      } else if (iter->name == "v") {
	v = iter->value.c_str();
      }else if (iter->name == "ref") {
	ref = atol(iter->value.c_str());
      } else if (iter->name == "role") {
	role = iter->value.c_str();
      } else if (iter->name == "type") {
	type = iter->value.c_str();
      }
      break;
    }
  }

  switch (depth) {
  case 2:
    _type = name;
    if (name == "node") {
      _con->createNode(id,version,lat,lon);
    } else if (name == "way") {
      _con->createWay(id,version);
    } else if (name == "relation") {
      _con->createRelation(id,version);
    } else if (name == "bounds" || name == "bound") {
    } else {
      throw "Onbekend element "+name;
    }
    break;
  case 3:
    if (lastid != 0) {
      if (name == "tag") {
	if (! (k == "created_by" ||
	       k == "converted_by" || 
	       k == "time" ||
	       k == "timestamp" ||
	       k == "user" ||
	       k == "fixme" ||
	       k == "FIXME" || 
	       k == "todo" ||
	       k == "TODO" ||
	       strncmp(k.c_str(),"AND",3) == 0 ||
	       strncmp(k.c_str(),"source",6) == 0 ||
	       strncmp(k.c_str(),"3dshapes",8) == 0 ||
	       strncmp(k.c_str(),"note",4) == 0 ||
	       strncasecmp(k.c_str(),"opengeo",7) == 0)) {
	  _con->createTag(lastid,_type,k,v);
	}
      } else if (name == "member") {
	_con->createMember(lastid,memcnt++,ref,type,role);
      } else if (name == "nd" ) {
	_con->createNd(lastid,ndcnt++,ref);
      } else {
	throw "Onbekend element "+name;
      }
    }
    break;
  }
}

void MySaxParser::on_end_element(const Glib::ustring& name) {
  //  std::cout << "on_end_element()" << std::endl;
  if (depth == 2) {
    lastid = 0;
  }
  depth--;
  counter++;
  if ((counter%10000) == 0) {
    _con->commit();
	printCounts();
    _con->begin();
  }
}

void MySaxParser::on_characters(const Glib::ustring& text) {
  //  std::cout << "on_characters(): " << text << std::endl;
}

void MySaxParser::on_comment(const Glib::ustring& text) {
  //  std::cout << "on_comment(): " << text << std::endl;
}

void MySaxParser::on_warning(const Glib::ustring& text) {
  //  std::cout << "on_warning(): " << text << std::endl;
}

void MySaxParser::on_error(const Glib::ustring& text) {
  //  std::cout << "on_error(): " << text << std::endl;
}

void MySaxParser::on_fatal_error(const Glib::ustring& text) {
  //  std::cout << "on_fatal_error(): " << text << std::endl;
}

void MySaxParser::setDBconn(osm_db::database *con) {
  _con = con;
}
