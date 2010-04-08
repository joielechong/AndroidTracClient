#include "eisparser.h"
#include <iostream>
#include <string>
#include <glibmm/ustring.h>
#include <glibmm/stringutils.h>
#include <cstdlib>
#include <cstring>
#include <stdexcept>

namespace eisparser {
  using namespace std;

  void EisenParser::on_start_document() {
    //     std::cout << "on_start_document()" << std::endl;
    _depth = 0;
    _state = 0;
    cout << "<TABLE>" << endl;
  }
  
  void EisenParser::on_end_document() {
    //    std::cout << "on_end_document()" << std::endl;
    cout << "</TABLE>"<<endl;
  }
  void EisenParser::on_comment(const Glib::ustring& text)
  {
    std::cout << "on_comment(): " << text << std::endl;
  }

  void EisenParser::on_warning(const Glib::ustring& text)
  {
    std::cout << "on_warning(): " << text << std::endl;
  }

  void EisenParser::on_error(const Glib::ustring& text)
  {
    std::cout << "on_error(): " << text << std::endl;
  }

  void EisenParser::on_fatal_error(const Glib::ustring& text)
  {
    std::cout << "on_fatal_error(): " << text << std::endl;
  }


  void EisenParser::on_characters(const Glib::ustring &text) {
    //    std::cout << "on_characters(): " << text.c_str() << "  ("<<_depth<<","<<_state<<")" << std::endl;
    switch(_state) {

    case 9:
      display_eis();  // fall through to state 3
    case 3:
      {
	_eis += text;
	string drie = _eis.substr(0,3);
	if (drie == "ctt" || drie == "VMS" || drie == "NZK" || drie == "NZk" || drie == "CTT" || drie == "Ctt")
	  _state = 2;
	else {
	  _eis = "";
	  _state = 7;
	}
      }
      break;
      
    case 6:
      if (_eis == "") 
	_state = 7;
      else {
	_eistitel += text;
	_state--;
      }
      break;

    case 10:
    case 11:
      _eistext += text;
      _state = 10;
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
      else if (tag == "w:tc") {
	if (_eis.length() >5 &&(_eis.substr(1,4)=="ttOA" || _eis.substr(1,4)=="ttMS"))
	  _state = 10;
	else
	  _state = 5;  // Naar eistitel
      }
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
      else if (tag == "w:tr")
	_state = 8;
      break;

    case 10:
      if (tag == "w:t")  // Eis tekst
	_state++;
      else if (tag == "w:tc") 
	_state = 12;
      else if (tag == "w:tr")
	_state = 8;
      else if (tag == "w:binData")
	_state = 14;
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
    } else if (_state == 14 && tag == "w:binData") {
	_state = 10;
    } else if (tag == "w:tbl"){
      display_eis();
      _state = 0;
    }
    _depth--;
    
  }
  
  void EisenParser::display_eis() {
    if (_eis != "" && _eis != "EisPagina") {
      /*
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
      */
      string t = Glib::strescape(_eistext);
      cout <<"<TR><TD>" << _eis.raw() << "</TD><TD>" << _eistitel.raw() << "</TD><TD> " << t <<"</TD><TD>" << _eistrack <<"</TD></TR>" <<endl;
      _eis = "";
      _eistitel = "";
      _eistext = "";
      _eistrack = "";
    }
  }
}
