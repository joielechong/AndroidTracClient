#ifndef _EISPARSER_H
#define _EISPARSER_H

#include <libxml++/libxml++.h>
#include <string>

namespace eisparser {
  using namespace std;

  class EisenParser : public xmlpp::SaxParser {
  public:
    inline EisenParser() : xmlpp::SaxParser(), _depth(0),_state(0) {}
    inline virtual ~EisenParser() {};
    
    inline int getDepth() {return _depth;}
    
  protected:
    //overrides:
    virtual void on_start_document();
    virtual void on_end_document();
    virtual void on_start_element(const Glib::ustring& name, const AttributeList& properties);
    virtual void on_end_element(const Glib::ustring& name);
    virtual void on_characters(const Glib::ustring& text);
    virtual void on_comment(const Glib::ustring& text);
    virtual void on_warning(const Glib::ustring& text);
    virtual void on_error(const Glib::ustring& text);
    virtual void on_fatal_error(const Glib::ustring& text);
    
  private:
    
    void printCounts();
    void display_eis();

    short _depth;
    short _state;
    Glib::ustring _eis;
    Glib::ustring _eistitel;
    Glib::ustring _eistext;
    Glib::ustring _eistrack;
  };
}
#endif //_EISPARSER_H
