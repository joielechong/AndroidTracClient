#include "myparser.h"
#include <iostream>
#include "osm.h"

MySaxParser::MySaxParser() : xmlpp::SaxParser() {
  depth = 0;
  *elem = NULL;
}

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
  std::cout << "node: name=" << name << "(" << depth << ")" << std::endl;
	
  if (depth == 2) {
	if (name == "node") {
	  elem = new osm::Node();
	} else if (name == "way") {
	  elem = new osm::Way();
	} else if (name == "relation") {
	  elem = new osm::Relation();
	} else {
	// throw an exception
	}
  }
  
  // Print attributes:
  for(xmlpp::SaxParser::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter)
    {
      std::cout << "  Attribute: " << iter->name << " = " << iter->value << std::endl;
	  if (depth == 2) {
	    if (iter->name == "id") 
		  elem->setId(iter->value);
		else if (iter->name == "version") 
		  elem->setVersion(iter->value);
	  }
    }
}

void MySaxParser::on_end_element(const Glib::ustring& name) {
  std::cout << "on_end_element()" << std::endl;
  if (depth == 2 && elem != NULL) {
     elem->store(_con);
	 delete [] *elem;
	 *elem = NULL;
  }
  depth--;
  
}

void MySaxParser::on_characters(const Glib::ustring& text) {
  std::cout << "on_characters(): " << text << std::endl;
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

void MySaxParser::setDBconn(sqlite3x::sqlite3_connection *con) {
  _con = con;
}
