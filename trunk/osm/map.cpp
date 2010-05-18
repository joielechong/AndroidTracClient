#include "osm.h"
#include "osm_db.h"
#include <string>
#include <stdbool.h>
#include <libxml++/libxml++.h>
#include <iostream>
#include <stdbool.h>

namespace osm {
  using namespace std;

  class Highway {
  public:
    inline Highway() : _extracost(0),_speed(0) {}
    inline unsigned int speed() const {return _speed;}
    inline void speed(const unsigned int s) {_speed=s;}
    inline unsigned int extracost() const {return _extracost;}
    inline void extracost(const unsigned int e) {_extracost=e;}

  private:
    int _extracost;
    unsigned int _speed;
  };

  map<string,Highway> highways;
  
  class Profile {
  public:
    inline Profile() : _maxspeed(0),_avgspeed(0),_ignore_oneway(false) {}

    inline unsigned int maxspeed() const {return _maxspeed;}
    inline void maxspeed(const unsigned int s) {_maxspeed=s;}
    inline unsigned int avgspeed() const {return _avgspeed;}
    inline void avgspeed(const unsigned int s) {_avgspeed=s;}
    inline bool ignore_oneway() const {return _ignore_oneway;}
    inline void set_ignore_oneway() {_ignore_oneway = true;}
    inline unsigned int allowed(const string h) {return _allowed[h];}
    inline void allowed(string h,const unsigned int e) {_allowed[h]=e;}
    inline unsigned int traffic_calming(const string t) {return _traffic_calming[t];}
    inline void traffic_calming(const string t,const unsigned int e) {_traffic_calming[t]=e;}
    inline unsigned int barrier(const string b) {return _barrier[b];}
    inline void barrier(const string b,const unsigned int e) {_barrier[b]=e;}

  private:
    unsigned int _maxspeed;
    unsigned int _avgspeed;
    bool _ignore_oneway;
    map<string,unsigned int> _allowed;
    map<string,unsigned int> _traffic_calming; 
    map<string, unsigned int> _barrier;
  };

  map<string,Profile> profiles;
  
  static void print_indentation(unsigned int indentation) {
    for(unsigned int i = 0; i < indentation; ++i)
      std::cout << " ";
  }

  static void process_profiles(const xmlpp::Node* node) {
  }

  static void process_highways(const xmlpp::Node* node) {
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node);
      const xmlpp::Attribute* attribute = nodeElement->get_attribute("name");
      Glib::ustring name,speed,extracost;
      if (attribute)
	name=attribute->get_value();
      else
	throw domain_error("highway requires name");
      
      attribute = nodeElement->get_attribute("speed");
      if (attribute)
	speed=attribute->get_value();
      attribute = nodeElement->get_attribute("extracost");
      if (attribute)
	extracost=attribute->get_value();
      highways[name] = Highway();
      if (speed.length() > 0)
	highways[name].speed(atol(speed.c_str()));
      if (extracost.length() > 0)
	highways[name].extracost(atol(extracost.c_str()));
    }
  }
  
  static void process_conf(const xmlpp::Node* node) {
    Glib::ustring nodename = node->get_name();
    if (nodename != "astar")
      throw domain_error("Foutief configuratiefile");
    
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::ContentNode* nodeContent = dynamic_cast<const xmlpp::ContentNode*>(node);
      const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
      const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
      if(!(nodeText && nodeText->is_white_space())) {//Let's ignore the indenting - you don't always want to do this.    
	nodename = (*iter)->get_name();
	if (nodename == "highways") 
	  process_highways(*iter);
	else if (nodename == "profiles") 
	  process_profiles(*iter);
	else
	  throw domain_error("Foutiefconfiguratiefile: highways of profiles verwacht. Naam = "+nodename);
      }
    }      
  }
  
  static void print_node(const xmlpp::Node* node, unsigned int indentation = 0) {
    std::cout << std::endl; //Separate nodes by an empty line.
    
    const xmlpp::ContentNode* nodeContent = dynamic_cast<const xmlpp::ContentNode*>(node);
    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
    if(nodeText && nodeText->is_white_space()) //Let's ignore the indenting - you don't always want to do this.
      return;
    
    const Glib::ustring nodename = node->get_name();
    
    if(!nodeText && !nodeComment && !nodename.empty()) { //Let's not say "name: text".
      print_indentation(indentation);
      
      const Glib::ustring namespace_prefix = node->get_namespace_prefix();
      if(namespace_prefix.empty())
	std::cout << "Node name = " << nodename << std::endl;
      else
	std::cout << "Node name = " << namespace_prefix << ":" << nodename << std::endl;
    } else if(nodeText) { //Let's say when it's text. - e.g. let's say what that white space is.
      print_indentation(indentation);
      std::cout << "Text Node" << std::endl;
    }
    
    //Treat the various node types differently: 
    if(nodeText) {
      print_indentation(indentation);
      std::cout << "text = \"" << nodeText->get_content() << "\"" << std::endl;
    } else if(nodeComment) {
      print_indentation(indentation);
      std::cout << "comment = " << nodeComment->get_content() << std::endl;
    } else if(nodeContent) {
      print_indentation(indentation);
      std::cout << "content = " << nodeContent->get_content() << std::endl;
    } else if(const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node)) {
      //A normal Element node:
      
      //line() works only for ElementNodes.
      print_indentation(indentation);
      std::cout << "     line = " << node->get_line() << std::endl;
      
      //Print attributes:
      const xmlpp::Element::AttributeList& attributes = nodeElement->get_attributes();
      for(xmlpp::Element::AttributeList::const_iterator iter = attributes.begin(); iter != attributes.end(); ++iter) {
	const xmlpp::Attribute* attribute = *iter;
	print_indentation(indentation);
	
	const Glib::ustring namespace_prefix = attribute->get_namespace_prefix();
	if(namespace_prefix.empty())
	  std::cout << "  Attribute " << attribute->get_name() << " = " << attribute->get_value() << std::endl; 
	else
	  std::cout << "  Attribute " << namespace_prefix  << ":" << attribute->get_name() << " = " << attribute->get_value() << std::endl;
      }
      
      const xmlpp::Attribute* attribute = nodeElement->get_attribute("title");
      if(attribute) {
	std::cout << "title found: =" << attribute->get_value() << std::endl;
      }
    }
    
    if(!nodeContent) {
      //Recurse through child nodes:
      xmlpp::Node::NodeList list = node->get_children();
      for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
	print_node(*iter, indentation + 2); //recursive
      }
    }
  }
  
  Map::Map(osm_db::database *sql,const unsigned long cacheSize,string conffile) :
    _con(sql),
    _cacheSize(cacheSize),
    _nodes(_con,_cacheSize),
    _ways(_con,_cacheSize),
    _relations(_con,_cacheSize),
    _conffile(conffile) {
    xmlpp::DomParser parser;
    //parser.set_validate();
    parser.set_substitute_entities(); //We just want the text to be resolved/unescaped automatically.
    parser.parse_file(_conffile);
    if(parser) {
      //Walk the tree:
      const xmlpp::Node* pNode = parser.get_document()->get_root_node(); //deleted by DomParser.
      print_node(pNode);
      process_conf(pNode);
    } else
      throw runtime_error("Kan file "+_conffile+" niet parsen");
  }
  
  bool Map::insideRelation(const long relationid,const long nodeid) {
    double lat,lon;
    std::vector<double> lats;
    std::vector<double> lons;


    lat = _nodes[nodeid].lat();
    lon = _nodes[nodeid].lon();

    _con->getRelCoords(relationid,lats,lons);

    int nvert = lats.size();
    bool c=false;
    //    cout << nvert << " nodes"<<endl;
    int j=nvert - 1;
    for (int i=0;i<nvert;j=i++) {
      if ((lons[i] > lon ) != (lons[j] > lon)) {
	double l2 = (lats[j]-lats[i])*(lon-lons[i])/(lons[j]-lons[i]) + lats[i];
	//	cout << "l2 = " << l2 << endl;
	if (lat < l2)
	  c = !c;
      }
      //      cout << i << " " << lon << " " << lons[j] << " " << lons[i] << " : " << lat << " " << lats[j] << " " << lats[i] << " = " << c << endl;
    }
    return c;
  }

  void Map::findAdmin(const string querystring,std::vector<string> &naam,std::vector<int> &level) {
    std::vector<long> nodeids;
    std::vector<double> nodelats;
    std::vector<double> nodelons;
    std::vector<long> relids;
    std::vector<string> relname;
    std::vector<int> admlevel;
    std::vector<long> admid;
    
    _con->findAddress(querystring,nodeids,nodelats,nodelons);
    //    cout << nodeids.size() << " nodes gevonden bij " << querystring << endl;
    for (unsigned int j=0;j<nodeids.size();j++) {
      cout << "Node: " << j << " " << nodelats[j] << "," << nodelons[j] << " " <<nodeids[j] << endl;
      _con->findAdmin(nodelats[j],nodelons[j],relids,relname,admlevel);
      cout << relids.size() << " admins gevonden" << endl;
      for (unsigned int i=0;i<relids.size();i++) {
	int found=-1;
	//	cout << relname[i] << endl;
	for (unsigned int k=0;found<0 && k<admid.size();k++) {
	  if (admid[k]==relids[i])
	    found=k;
	}
	if (found == -1) {
	  //	  cout << "  Admin: "<< i << " " << relids[i] << " " <<nodeids[j] << endl;
	  Node& n=_nodes[nodeids[j]];
	  if (_relations[relids[i]].isInside(_con,n.lat(),n.lon())) {
	    //	    cout << "    Omvattend"<<endl;
	    admid.push_back(relids[i]);
	    naam.push_back(relname[i]);
	    level.push_back(admlevel[i]);
	  }
	}
      }
    }
  }

  double Map::Astar(const Node &n1,const Node &n2) {
    return 0;
  }

  double Map::Astar(const Node &n1,const double lat2,const double lon2) {
    return 0;
  }

  double Map::Astar(const double lat1,const double lon1,const Node &n2) {
    return 0;
  }

  double Map::Astar(const double lat1,const double lon1,const double lat2,const double lon2) {
    return 0;
  }
}
