#include "myparser.h"
#include <iostream>
#include "osm.h"
#include <string>
#include <map>

MySaxParser::MySaxParser() : depth(0), elem(NULL), xmlpp::SaxParser() {}

MySaxParser::~MySaxParser() {
}

void MySaxParser::on_start_document() {
  std::cout << "on_start_document()" << std::endl;
}

void MySaxParser::on_end_document() {
  std::cout << "on_end_document()" << std::endl;
}

void MySaxParser::on_start_element(const Glib::ustring& name,const AttributeList& attributes) {
  depth++;
//  std::cout << "node: name=" << name << "(" << depth << ")" << std::endl;
  
  // Store attributes:
  std::map<std::string,std::string> attr;
  for(xmlpp::SaxParser::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter) {
//    std::cout << "  Attribute: " << iter->name << " = " << iter->value << std::endl;
	attr[iter->name]=iter->value;
  }
  switch (depth) {
	case 2:
    if (name == "node") {
      elem = new osm::Node(attr["id"],attr["version"],attr["lat"],attr["lon"]);
    } else if (name == "way") {
      elem = new osm::Way(attr["id"],attr["version"]);
    } else if (name == "relation") {
      elem = new osm::Relation(attr["id"],attr["version"]);
	} else if (name == "bounds") {
    } else {
      throw(("Onbekend element "+name).c_str());
    }
	break;
	
	case 3:
	if (elem != NULL) {
      if (name == "tag") {
        elem->addTag(attr["k"],attr["v"]);
      } else if (name == "member") {
        elem->addMember(attr["ref"],attr["type"],attr["role"]);
	  } else if (name == "nd" ) {
	    elem->addNd(attr["ref"]);
	  } else {
	    throw(("Onbekend element "+name).c_str());
	  }
    }
	break;
	
  }
}

void MySaxParser::on_end_element(const Glib::ustring& name) {
//  std::cout << "on_end_element()" << std::endl;
  if (depth == 2 && elem != NULL) {
	std::cout << *elem;
    *_con << *elem;
    delete elem;
    elem = NULL;
  }
  depth--;
  
}

void MySaxParser::on_characters(const Glib::ustring& text) {
//  std::cout << "on_characters(): " << text << std::endl;
}

void MySaxParser::on_comment(const Glib::ustring& text) {
  std::cout << "on_comment(): " << text << std::endl;
}

void MySaxParser::on_warning(const Glib::ustring& text) {
  std::cout << "on_warning(): " << text << std::endl;
}

void MySaxParser::on_error(const Glib::ustring& text) {
  std::cout << "on_error(): " << text << std::endl;
}

void MySaxParser::on_fatal_error(const Glib::ustring& text) {
  std::cout << "on_fatal_error(): " << text << std::endl;
}

void MySaxParser::setDBconn(osm_db::database *con) {
  _con = con;
}
