#ifndef _OSM_MYPARSER_H
#define _OSM_MYPARSER_H

#include <libxml++/libxml++.h>
#include <string>
#include "osm_db.h"

namespace osmparser {
  class MySaxParser : public xmlpp::SaxParser {
  public:
    inline MySaxParser() : xmlpp::SaxParser(), _depth(0),_counter(0), _lastid(0), _memcnt(0), _ndcnt(0) {}
    inline virtual ~MySaxParser() {};
    
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
    int _memcnt,_ndcnt;
  };
}
#endif //_MYPARSER_H
