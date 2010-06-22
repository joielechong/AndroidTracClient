#ifndef _OSM_PARSER_H
#define _OSM_PARSER_H

#include <libxml++/libxml++.h>
#include <string>
#include <stdbool.h>
#include <vector>
#include "osm_db.h"

namespace osmparser {
  class OSMParser : public xmlpp::SaxParser {
  public:
    inline OSMParser() : xmlpp::SaxParser(), _depth(0),_counter(0), _lastid(0), _memcnt(0), _ndcnt(0) {}
    inline virtual ~OSMParser() {};
    
    inline int getDepth() {return _depth;}
    inline void setDBconn(osm_db::database *con) {_con=con;};
    
  protected:
    //overrides:
    virtual void on_start_document();
    virtual void on_end_document();
    virtual void on_start_element(const Glib::ustring& name, const AttributeList& properties);
    virtual void on_end_element(const Glib::ustring& name);
    
  private:
    
    void printCounts();
    
    short _depth;
    long _counter;
    osm_db::database *_con;
    long _lastid;
    std::string _type;
    int _memcnt;
    int _ndcnt;
  };
}
#endif //_OSMPARSER_H
