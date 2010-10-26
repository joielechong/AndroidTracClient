#include "osm.h"
#include "osm_db.h"
#include <string>
#include <stdbool.h>
#include <libxml++/libxml++.h>
#include <iostream>
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
    for(iter=_highway.begin();iter != _highway.end();iter++) 
      out << endl << "  Highway : " << (*iter).first << " extracost = " << (*iter).second;
    for(iter=_traffic_calming.begin();iter != _traffic_calming.end();iter++) 
      out << endl << "  Traffic Calming : " << (*iter).first << " extracost = " << (*iter).second;
  }

  unsigned int Profile::allowed(const string h) {
    if (_allowed.find(h) == _allowed.end())
      throw range_error(h+" not allowed");
    else 
      return _allowed[h];
  }
 
   unsigned int Profile::traffic_calming(const string t) {
     if (_traffic_calming.find(t) == _traffic_calming.end())
       return 0;
     else
       return _traffic_calming[t];
  }
  
  unsigned int Profile::barrier(const string b) {
    if (_barrier.find(b) == _barrier.end())
      return  0;
    else
     return _barrier[b];
  }
  
  unsigned int Profile::highway(const string h) {
    if (_highway.find(h) == _highway.end())
      return  0;
    else
     return _highway[h];
  }
  
  static void getAttribute(const xmlpp::Element *nodeElement,const string attr_name,Glib::ustring &attribute) {
    const xmlpp::Attribute *a = nodeElement->get_attribute(attr_name);
    if (a)
      attribute=a->get_value();
  }
  
  static double conv_extracost(Glib::ustring extracost) {
    if (extracost == "INFINITY")
      return INFINITY;
    else
      return  atol(extracost.c_str());
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
        profile.allowed(name,conv_extracost(extracost.c_str()));
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
        profile.barrier(name,conv_extracost(extracost.c_str()));
      }
    }
  }
  
  static void process_highway(const xmlpp::Node *node,Profile &profile) {
    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(node);
    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(node);
    
    if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
      if(!nodeText && !nodeComment) { //Let's not say "name: text".
        const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(node);
	Glib::ustring name,extracost;
        
        getAttribute(nodeElement,"name",name);
        getAttribute(nodeElement,"extracost",extracost);
        profile.highway(name,conv_extracost(extracost.c_str()));
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
        profile.traffic_calming(name,conv_extracost(extracost.c_str()));
      }
    }
  }
  
  void Map::process_profiles(const xmlpp::Node* node) {
    xmlpp::Node::NodeList list = node->get_children();
    for(xmlpp::Node::NodeList::iterator iter = list.begin(); iter != list.end(); ++iter) {
      const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iter);
      const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iter);
      
      if(!(nodeText && nodeText->is_white_space())) { //Let's ignore the indenting - you don't always want to do this.    
	if(!nodeText && !nodeComment) { //Let's not say "name: text".
	  const xmlpp::Element* nodeElement = dynamic_cast<const xmlpp::Element*>(*iter);
	  Glib::ustring name,maxspeed,avgspeed,ignore_oneway,curvefactor;
          
          getAttribute(nodeElement,"name",name);
          getAttribute(nodeElement,"maxspeed",maxspeed);
          getAttribute(nodeElement,"avgspeed",avgspeed);
          getAttribute(nodeElement,"curvefactor",curvefactor);
          getAttribute(nodeElement,"ignore_oneway",ignore_oneway);

	  _profiles[name] = Profile();
	  if (maxspeed.length() > 0)
	    _profiles[name].maxspeed(atol(maxspeed.c_str()));
	  if (avgspeed.length() > 0)
	    _profiles[name].avgspeed(atol(avgspeed.c_str()));
	  if (curvefactor.length() > 0)
	    _profiles[name].curvefactor(atod(curvefactor.c_str()));
	  if (atol(ignore_oneway.c_str()) != 0)
	    _profiles[name].set_ignore_oneway();

	  xmlpp::Node::NodeList list = (*iter)->get_children();
	  for(xmlpp::Node::NodeList::iterator iterp = list.begin(); iterp != list.end(); ++iterp) {
	    const xmlpp::TextNode* nodeText = dynamic_cast<const xmlpp::TextNode*>(*iterp);
	    const xmlpp::CommentNode* nodeComment = dynamic_cast<const xmlpp::CommentNode*>(*iterp);
	    Glib::ustring nodename = (*iterp)->get_name();
	    
	    if(!(nodeText && nodeText->is_white_space())) {//Let's ignore the indenting - you don't always want to do this.    
	      nodename = (*iterp)->get_name();
	      if(!nodeText && !nodeComment && !nodename.empty()) { //Let's not say "name: text".
		if (nodename == "allowed") 
		  process_allowed(*iterp,_profiles[name]);
		else if (nodename == "barrier")
		  process_barrier(*iterp,_profiles[name]);
		else if (nodename == "highway")
		  process_highway(*iterp,_profiles[name]);
		else if (nodename == "traffic_calming")
		  process_trafficcalming(*iterp,_profiles[name]);
		else
		  throw domain_error("Foutief configuratiefile: allowed, barrier, highway of traffic_calming verwacht. Naam = "+nodename);
	      }
	    }
	  }      
	}
      }
    }
  }
  
  void Map::process_highways(const xmlpp::Node* node) {
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

	  _highways[name] = Highway();
	  if (speed.length() > 0)
	    _highways[name].speed(atol(speed.c_str()));
	  else
	    _highways[name].speed(0);
	  if (extracost.length() > 0)
	    _highways[name].extracost(conv_extracost(extracost.c_str()));
	  else
	    _highways[name].extracost(0);
	}
      }
    }
  }
  
  void Map::process_conf(const xmlpp::Node* node) {
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
  
  void Map::load_conf() {
	
    xmlpp::DomParser parser;
    //parser.set_validate();
    parser.set_substitute_entities(); //We just want the text to be resolved/unescaped automatically.
    parser.parse_file(_conffile);
    if(parser) {
      //Walk the tree:
      const xmlpp::Node* pNode = parser.get_document()->get_root_node(); //deleted by DomParser.
      process_conf(pNode);

    } else
      throw runtime_error("Kan file "+_conffile+" niet parsen");
    
  }
}