#include "osm_db.h"
#include "osmparser.h"
#include <iostream>
#include <string>
#include <glibmm/ustring.h>
#include <cstdlib>
#include <cstring>
#include <stdexcept>
#include "config.h"

#if  defined(HAVE_STRNCASECMP) 
#define STRNCASECMP strncasecmp
#elif defined(HAVE_STRNICMP)
#defined STRNCASECMP strnicmp
#else
#error No strncasecmp or strnicmp function available
#endif

namespace osmparser {
  
  void GenParser::printCounts() { 
    long nodes,ways,rels,bounds,tags,nds,mems; 
    _con->getCounts(nodes,ways,rels,bounds,tags,nds,mems); 
    std::cout << _counter << ": " << nodes << " nodes " << ways << " ways " << rels << " relations " << tags << " tags " << nds << " nds " << mems << " members " << std::endl; 
  }
  
  void GenParser::on_start_document() {
    //  std::cout << "on_start_document()" << std::endl;
    _con->begin();
  }
  
  void GenParser::on_end_document() {
    //  std::cout << "on_end_document()" << std::endl;
    _con->commit();
    printCounts();
  }
  
  void OSCParser::on_start_document() {
    _con->getBound(_minlat,_minlon,_maxlat,_maxlon);
    _con->begin();
  }
  
  void OSCParser::on_start_element(const Glib::ustring& name,const AttributeList& attributes) {
    _depth++;
    //  std::cout << "node: name=" << name.c_str() << "(" << depth << ")" << std::endl;
    
    // Store attributes:
    long id=-1,ref=-1;
    int version=-1;
    double lat=-999,lon=-999;
    std::string type,role,k,v;
    
    if (_depth == 1 && name != "osmChange")
      throw std::runtime_error("Incorrect osmChange file");
    
    if (_depth == 2)
      _mode = name;
    
    for(xmlpp::SaxParser::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter) {
      //    std::cout << "  Attribute: " << iter->name << " = " << iter->value.c_str() << std::endl;
      switch (_depth) {
      case 1:
	if (iter->name == "version" && iter->value != "0.6")
	  throw std::runtime_error("osmChange file uses wrong api version: "+iter->value);
	break;
	
      case 3:
	if (iter->name == "id") {
	  id = atol(iter->value.c_str());
	  _lastid = id;
	  _memcnt = 0;
	  _ndcnt = 0;
	} else if (iter->name == "version") {
	  version = atol(iter->value.c_str());
	} else if (iter->name == "lat") {
	  lat = atof(iter->value.c_str());
	} else if (iter->name == "lon") {
	  lon = atof(iter->value.c_str());
	}
	break;
      case 4:
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

    if (_mode == "create") {
      
      switch (_depth) {
      case 3:
	_type = name;
	if (name == "node") {
	  _con->createNode(id,version,lat,lon);
	} else if (name == "way") {
	  _con->createWay(id,version);
	} else if (name == "relation") {
	  _con->createRelation(id,version);
	} else if (name == "bounds" || name == "bound") {
	} else {
	  throw std::range_error("Onbekend element "+name);
	}
	break;
      case 4:
	if (_lastid != 0) {
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
	      _con->createTag(_lastid,_type,k,v);
	    }
	  } else if (name == "member") {
	    try {
	      _con->createMember(_lastid,_memcnt++,ref,type,role);
	    } catch (const std::exception &ex) {
	      std::cerr << "createMember mislukt: "<<ex.what() << std::endl;
	      std::cerr << "  errmsg = " << _con->errmsg() << std::endl;
	      std::cerr << "lastid = " << _lastid << " memcnt = " << _memcnt-1 << " ref = "<< ref << " type = "<<type<<" role = "<<role << std::endl;
	    }
	  } else if (name == "nd" ) {
	    try {
	      int seq = _ndcnt++;
	      _con->createNd(_lastid,seq,ref);
	    } catch (const std::exception &ex) {
	      std::cerr << "createNd mislukt: "<<ex.what() << std::endl;
	      std::cerr << "  errmsg = " << _con->errmsg() << std::endl;
	      std::cerr << "lastid = " << _lastid << " ndcnt = " << _ndcnt-1 << " ref = "<< ref << std::endl;
	    }
	  } else {
	    throw std::range_error("Onbekend element "+name);
	  }
	}
	break;
      }
    } else if (_mode == "modify") {
    } else if (_mode == "delete") {
    } else 
      throw std::runtime_error("Onbekende methode in osc file: "+_mode);
  }
  
  void OSMParser::on_start_element(const Glib::ustring& name,const AttributeList& attributes) {
    _depth++;
    //  std::cout << "node: name=" << name.c_str() << "(" << depth << ")" << std::endl;
    
    // Store attributes:
    long id=-1,ref=-1;
    int version=-1;
    double lat=-999,lon=-999;
    std::string type,role,k,v;
    
    if (_depth == 1 && name != "osm")
      throw std::runtime_error("Incorrect planet file");
    
    for(xmlpp::SaxParser::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter) {
      //    std::cout << "  Attribute: " << iter->name << " = " << iter->value.c_str() << std::endl;
      switch (_depth) {
      case 1:
	if (iter->name == "version" && iter->value != "0.6")
	  throw std::runtime_error("Planetfile uses wrong api version: "+iter->value);
	break;

      case 2:
	if (iter->name == "id") {
	  id = atol(iter->value.c_str());
	  _lastid = id;
	  _memcnt = 0;
	  _ndcnt = 0;
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
    
    switch (_depth) {
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
	throw std::range_error("Onbekend element "+name);
      }
      break;
    case 3:
      if (_lastid != 0) {
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
	    _con->createTag(_lastid,_type,k,v);
	  }
	} else if (name == "member") {
	  try {
	  _con->createMember(_lastid,_memcnt++,ref,type,role);
	  } catch (const std::exception &ex) {
	    std::cerr << "createMember mislukt: "<<ex.what() << std::endl;
	    std::cerr << "  errmsg = " << _con->errmsg() << std::endl;
	    std::cerr << "lastid = " << _lastid << " memcnt = " << _memcnt-1 << " ref = "<< ref << " type = "<<type<<" role = "<<role << std::endl;
	  }
	} else if (name == "nd" ) {
	  try {
            int seq = _ndcnt++;
	    _con->createNd(_lastid,seq,ref);
	  } catch (const std::exception &ex) {
	    std::cerr << "createNd mislukt: "<<ex.what() << std::endl;
	    std::cerr << "  errmsg = " << _con->errmsg() << std::endl;
	    std::cerr << "lastid = " << _lastid << " ndcnt = " << _ndcnt-1 << " ref = "<< ref << std::endl;
	  }
	} else {
	  throw std::range_error("Onbekend element "+name);
	}
      }
      break;
    }
  }
  
  void OSMParser::on_end_element(const Glib::ustring& name) {
    //  std::cout << "on_end_element()" << std::endl;
    if (_depth == 2)
      _lastid = 0;
    _depth--;
    _counter++;
    if ((_counter%10000) == 0) {
      _con->commit();
      printCounts();
      _con->begin();
    }
  }

  void OSCParser::on_end_element(const Glib::ustring& name) {
    //  std::cout << "on_end_element()" << std::endl;
    if (_depth == 2) 
      _mode = "";
    else if (_depth == 3)
      _lastid = 0;
    _depth--;
    _counter++;
    if ((_counter%10000) == 0) {
      _con->commit();
      printCounts();
      _con->begin();
    }
  }
}