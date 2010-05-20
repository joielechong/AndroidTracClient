#include "osm.h"
#include "osm_db.h"
#include "astarconf.h"
#include <string>
#include <stdbool.h>
#include <libxml++/libxml++.h>
#include <iostream>
#include <stdbool.h>
#include <map>
#include <vector>

namespace osm {
  using namespace std;

  void Profile::output(ostream &out) {
    out << "maxspeed = " << _maxspeed << "  avgspeed = " << _avgspeed << " ignore_oneway = " << _ignore_oneway;

    map<string,unsigned int>::iterator iter;

    for(iter=_allowed.begin();iter != _allowed.end();iter++) 
      out << endl << "  Allowed : " << (*iter).first << " extracost = " << (*iter).second;
    for(iter=_barrier.begin();iter != _barrier.end();iter++) 
      out << endl << "  Barrier : " << (*iter).first << " extracost = " << (*iter).second;
    for(iter=_traffic_calming.begin();iter != _traffic_calming.end();iter++) 
      out << endl << "  Traffic Calming : " << (*iter).first << " extracost = " << (*iter).second;
  }

  map<string,Highway> highways;
  map<string,Profile> profiles;
  
  static void print_indentation(unsigned int indentation) {
    for(unsigned int i = 0; i < indentation; ++i)
      std::cout << " ";
  }
  
  static void getAttribute(const xmlpp::Element *nodeElement,const string attr_name,Glib::ustring &attribute) {
    const xmlpp::Attribute *a = nodeElement->get_attribute(attr_name);
    if (a)
      attribute=a->get_value();
  }
  
  static void process_allowed(const xmlpp::Node *node,Profile &profile) {
    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
    if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
      if(!nodeText && !nodeComment) { //Let's not say "name: text".
        const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node);
	Glib::ustring name,extracost;
        
        getAttribute(nodeElement,"highway",name);
        getAttribute(nodeElement,"extracost",extracost);
        profile.allowed(name,atol(extracost.c_str()));
      }
    }
  }
  
  static void process_barrier(const xmlpp::Node *node,Profile &profile) {
    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
    if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
      if(!nodeText && !nodeComment) { //Let's not say "name: text".
        const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node);
	Glib::ustring name,extracost;
        
        getAttribute(nodeElement,"type",name);
        getAttribute(nodeElement,"extracost",extracost);
        profile.barrier(name,atol(extracost.c_str()));
      }
    }
  }
  
  static void process_trafficcalming(const xmlpp::Node *node,Profile &profile) {
    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
    if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
      if(!nodeText && !nodeComment) { //Let's not say "name: text".
        const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node);
	Glib::ustring name,extracost;
        
        getAttribute(nodeElement,"type",name);
        getAttribute(nodeElement,"extracost",extracost);
        profile.traffic_calming(name,atol(extracost.c_str()));
      }
    }
  }
  
  static void process_profiles(const xmlpp::Node* node) {
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iter);
      const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iter);
      
      if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
	if(!nodeText && !nodeComment) { //Let's not say "name: text".
	  const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(*iter);
	  Glib::ustring name,maxspeed,avgspeed,ignore_oneway;
          
          getAttribute(nodeElement,"name",name);
          getAttribute(nodeElement,"maxspeed",maxspeed);
          getAttribute(nodeElement,"avgspeed",avgspeed);
          getAttribute(nodeElement,"ignore_oneway",ignore_oneway);

	  profiles[name] = Profile();
	  if (maxspeed.length() > 0)
	    profiles[name].maxspeed(atol(maxspeed.c_str()));
	  if (avgspeed.length() > 0)
	    profiles[name].avgspeed(atol(avgspeed.c_str()));
	  if (atol(ignore_oneway.c_str()) != 0)
	    profiles[name].set_ignore_oneway();

	  xmlpp::Node::NodeList list = (*iter)->get_children();
	  for(xmlpp::Node::NodeList::iterator iterp = list.begin(); iterp != list.end(); ++iterp) {
	    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iterp);
	    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iterp);
	    Glib::ustring nodename = (*iterp)->get_name();
	    
	    if(!(nodeText && nodeText->is_white_space())) {//Let's ignore the indenting - you don't always want to do this.    
	      nodename = (*iterp)->get_name();
	      if(!nodeText && !nodeComment && !nodename.empty()) { //Let's not say "name: text".
		if (nodename == "allowed") 
		  process_allowed(*iterp,profiles[name]);
		else if (nodename == "barrier")
		  process_barrier(*iterp,profiles[name]);
		else if (nodename == "traffic_calming")
		  process_trafficcalming(*iterp,profiles[name]);
		else
		  throw domain_error("Foutief configuratiefile: allowed, barrier of traffic_calming verwacht. Naam = "+nodename);
	      }
	    }
	  }      
	}
      }
    }
  }
  
  static void process_highways(const xmlpp::Node* node) {
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iter);
      const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iter);
      
      if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
	if(!nodeText && !nodeComment) { //Let's not say "name: text".
	  const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(*iter);
	  Glib::ustring name,speed,extracost;
          getAttribute(nodeElement,"name",name);
          getAttribute(nodeElement,"speed",speed);
          getAttribute(nodeElement,"extracost",extracost);

	  highways[name] = Highway();
	  if (speed.length() > 0)
	    highways[name].speed(atol(speed.c_str()));
	  if (extracost.length() > 0)
	    highways[name].extracost(atol(extracost.c_str()));
	}
      }
    }
  }
  
  static void process_conf(const xmlpp::Node* node) {
    Glib::ustring nodename = node->get_name();
    if (nodename != "astar")
      throw domain_error("Foutief configuratiefile");
    
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iter);
      const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iter);
      
      if(!(nodeText && nodeText->is_white_space())) {//Let's ignore the indenting - you don't always want to do this.    
	nodename = (*iter)->get_name();
	if(!nodeText && !nodeComment && !nodename.empty()) { //Let's not say "name: text".
	  if (nodename == "highways") 
	    process_highways(*iter);
	  else if (nodename == "profiles") 
	    process_profiles(*iter);
	  else
	    throw domain_error("Foutiefconfiguratiefile: highways of profiles verwacht. Naam = "+nodename);
	}
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
      //      print_node(pNode);
      process_conf(pNode);
      
      for (map<string,Highway>::iterator i=highways.begin();i != highways.end(); i++) {
	cout << "  " << (*i).first << "  ";
	((*i).second).output(cout);
	cout << endl;
      }
      
      for (map<string,Profile>::iterator i=profiles.begin();i != profiles.end(); i++) {
	cout << "  " << (*i).first << "  ";
	((*i).second).output(cout);
	cout << endl;
      }
    } else
      throw runtime_error("Kan file "+_conffile+" niet parsen");
  }
  
  bool Map::insideRelation(const long relationid,const long nodeid) {
    double lat,lon;
    vector<double> lats;
    vector<double> lons;


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
}
