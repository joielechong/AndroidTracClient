#ifndef _OSM_MYPARSER_H
#define _OSM_MYPARSER_H

#include <libxml++/libxml++.h>

#include "osm.h"
#include "osm_db.h"

class MySaxParser : public xmlpp::SaxParser
{
 public:
  MySaxParser();
  virtual ~MySaxParser();
  
  int getDepth();
  void setDBconn(osm_db::osm_db *con);
 
 protected:
  //overrides:
  virtual void on_start_document();
  virtual void on_end_document();
  virtual void on_start_element(const Glib::ustring& name,
                                const AttributeList& properties);
  virtual void on_end_element(const Glib::ustring& name);
  virtual void on_characters(const Glib::ustring& characters);
  virtual void on_comment(const Glib::ustring& text);
  virtual void on_warning(const Glib::ustring& text);
  virtual void on_error(const Glib::ustring& text);
  virtual void on_fatal_error(const Glib::ustring& text);
  
  
 private:
  short depth;
  osm::Element *elem;
  osm_db::osm_db *_con;
};

#endif //_MYPARSER_H
