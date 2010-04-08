#include "eisparser.h"
#include <iostream>
#include <string>
#include <glibmm/ustring.h>
#include <cstdlib>
#include <cstring>
#include <stdexcept>

namespace eisparser {
  using namespace std;

  void EisenParser::on_start_document() {
    //    std::cout << "on_start_document()" << std::endl;
    _depth = 0;
    _state = 0;
    cout << "<TABLE>" << endl;
  }
  
  void EisenParser::on_end_document() {
    //    std::cout << "on_end_document()" << std::endl;
    cout << "</TABLE>"<<endl;
  }
  
  void EisenParser::on_characters(const Glib::ustring& text)
  {
    string t=text.c_str();
    //    std::cout << "on_characters(): " << t << "("<<_depth<<","<<_state<<"(" << std::endl;
    switch(_state) {
    case 3:
      _eis += text;
      _state--;
      break;
      
    case 6:
      _eistitel += text;
      _state--;
      break;

    case 11:
      _eistext += text;
      _state--;
      break;

    case 13:
      if (_eistrack != "")
	_eistrack += ", ";
      _eistrack += text;
      _state--;
      break;
    }      
  }
  
  void EisenParser::on_start_element(const Glib::ustring& name,const AttributeList& attributes) {
    _depth++;
    string tag=name.c_str();
    //    std::cout << "node: name=" << tag << "(" << _depth << "," << _state << ")" << std::endl;
    switch (_state) {
    case 0:
      if (tag == "w:tbl") {
	_state++;
	_eis = "";
	_eistitel = "";
	_eistext = "";
	_eistrack="";
      } else if (tag == "w:hdr" || tag == "w:ftr") 
	_state = 99;
      break;
     
    case 1:
      if (tag == "w:tc")
	_state++;
      break;
      
    case 2:
      if (tag == "w:t")
	_state++;  // Eis identificatie staat hier
      else if (tag == "w:tc")
	_state = 5;  // Naar eistitel
      break;

    case 5:
      if (tag == "w:t")
	_state++;
      else if (tag == "w:tc") 
	_state=7;
      else if (tag == "w:tr")
	_state = 8;
      break;
      
    case 7:
      if (tag == "w:tr")
	_state++;
      break;

    case 8:
      if (tag == "w:tc")  // Lege cel (onder eis identificatie)
	_state++;
      break;

    case 9:
      if (tag == "w:tc") 
	_state++;
      break;

    case 10:
      if (tag == "w:t")  // Eis tekst
	_state++;
      else if (tag == "w:tc") 
	_state = 12;
      else if (tag == "w:tr")
	_state = 8;
      break;

    case 12:
      if (tag== "w:t")  // Eis tracking
	_state++;
      else if (tag == "w:tr")
	_state = 8;
      break;
    }
      
  }
  
  void EisenParser::on_end_element(const Glib::ustring& name) {
    string tag=name.c_str();
    //    std::cout << "on_end_element() " << tag << std::endl;
    if (_state == 99) {
      if (tag == "w:hdr" || tag == "w:ftr")
	_state = 0;
    } else if (tag == "w:tbl"){
      if (_eis != "" && _eis != "EisPagina") {
	size_t c = _eistitel.find("&");
	while (c != string::npos) {
	  _eistitel.replace(c,1,"&amp;");
	  c = _eistitel.find("&",c+4);
	}
	c = _eistext.find("&");
	while (c != string::npos) {
	  _eistext.replace(c,1,"&amp;");
	  c = _eistext.find("&",c+4);
	}
	cout <<"<TR><TD>" << _eis << "</TD><TD>" << _eistitel << "</TD><TD> " << _eistext <<"</TD><TD>" << _eistrack <<"</TD></TR>" <<endl;
	_eis = "";
      }
      _state = 0;
    }
    _depth--;

  }
}
