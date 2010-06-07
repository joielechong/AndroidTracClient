#include "osm.h"
#include "osm_db.h"
#include "grootcirkel.h"
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
	  Glib::ustring name,maxspeed,avgspeed,ignore_oneway;
          
          getAttribute(nodeElement,"name",name);
          getAttribute(nodeElement,"maxspeed",maxspeed);
          getAttribute(nodeElement,"avgspeed",avgspeed);
          getAttribute(nodeElement,"ignore_oneway",ignore_oneway);

	  _profiles[name] = Profile();
	  if (maxspeed.length() > 0)
	    _profiles[name].maxspeed(atol(maxspeed.c_str()));
	  if (avgspeed.length() > 0)
	    _profiles[name].avgspeed(atol(avgspeed.c_str()));
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
		else if (nodename == "traffic_calming")
		  process_trafficcalming(*iterp,_profiles[name]);
		else
		  throw domain_error("Foutief configuratiefile: allowed, barrier of traffic_calming verwacht. Naam = "+nodename);
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
  
  Map::Map(osm_db::database *sql,const unsigned long cacheSize,string conffile) :
    _con(sql),
    _cacheSize(cacheSize),
    _vehicle(""),
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
      process_conf(pNode);

      /*
      for (highway_type::iterator i=_highways.begin();i != _highways.end(); i++) {
	cout << "  " << (*i).first << "  ";
	((*i).second).output(cout);
	cout << endl;
      }
      
      for (profile_type::iterator i=_profiles.begin();i != _profiles.end(); i++) {
	cout << "  " << (*i).first << "  ";
	((*i).second).output(cout);
	cout << endl;
      }
      */

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

  void Map::findAdmin(const string querystring,vector<string> &naam,vector<int> &level) {
    vector<long> nodeids;
    vector<double> nodelats;
    vector<double> nodelons;
    vector<long> relids;
    vector<string> relname;
    vector<int> admlevel;
    vector<long> admid;
    
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

  void Map::findLocation(const long id,vector<long> &adminlist) {
    vector<long> admins;
    
    adminlist.clear();
    _con->adminNode(id,admins);
    Node &n = _nodes[id];
    for (vector<long>::iterator i=admins.begin();i != admins.end();i++) {
      Relation &r = _relations[*i];
      if (r.isInside(_con,n.lat(),n.lon()))
        adminlist.push_back(*i);
    }
  }

  long Map::findCoor(const double lat,const double lon,const string vehicle) {
    throw runtime_error("findCoor : Not yet implemented");
  }
  
  double Map::distance(const Node &n1,const Node &n2) const {
    return grootcirkel(n1.lat(),n1.lon(),n2.lat(),n2.lon());
  }
  
  double Map::direction(const long n1, const long n2) {
    double dx = _nodes[n2].lon() - _nodes[n1].lon();
    double dy = _nodes[n2].lat() - _nodes[n1].lat();
    return 180.0*atan2(dx,dy)/PI;  // PI is gedefinieerd in grootcirkel.h
  }
}
