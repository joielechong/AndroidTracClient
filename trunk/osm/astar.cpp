#include "osm.h"
#include <map>
#include <cstdlib>
#include <stdbool.h>
#include "grootcirkel.h"

#define EXTRACOST_FACTOR (100.0)

namespace osm {
  using namespace std;
  
  void Map::initRoute(const string &vehicle) {
    if (vehicle != "" && _profiles.find(vehicle) == _profiles.end())
        throw domain_error("Onbekend voertuig : "+vehicle);
    _vehicle = vehicle;
  }

  bool Map::wrong_direction(Node &nodex,Node &nodey,Way &ww,string onew) const {

    int dir = _con->getDirection(nodex.id(),nodey.id(),ww.id());
    if (dir == 0)
      throw runtime_error("geen directe verbinding tussen nodes");

    if (onew=="rev")
      return (dir == -1);
    else
      return (dir == 1);
  }
  
  long Map::curvecost(const long x,const long y,const long p) {
    if (p == 0)
      return 0;
    Node &ndx=nodes(x);
    Node &ndy=nodes(y);
    Node &ndp=nodes(p);
    
    double dx1 = ndy.lon()-ndx.lon();
    double dy1 = ndy.lat()-ndx.lat();
    double dx2 = ndx.lon()-ndp.lon();
    double dy2 = ndx.lat()-ndp.lat();
    
    double h1 = 180.0 * atan2(dy1,dx1) / PI;
    double h2 = 180.0 * atan2(dy2,dx2) / PI;
    double dh = fabs(h2-h1);
    if (dh > 180) 
      dh = 360 - dh;

    if (dh < 90)
      return 3 * (dh/90);
    else if (dh < 120) 
      return 3 + 4*(dh-90)/30;
    else if (dh < 150)
      return 5 + 5*(dh-120)/30;

    return 10 + 10 * (dh-150)/30;
  }
  
  double Map::cost(const long x,const long y,const long prevnode,const bool ignoreExtra) { 
    _costcounter++;
    double dist = distance(x,y);
    if (_vehicle == "") return dist;

    long extracost = 0; 
    double speed = _profiles[_vehicle].maxspeed();
    long w = getConnectingWay(x,y);
    if (w == 0)
      throw range_error("Kan weg niet vinden");
    osm::Way &ww = ways(w);
    string hw;
    
    long cnt = ww.getNodesCount() - 1;
    if (cnt < 1) cnt = 1;
    
// type weg

    try {
      hw = ww["highway"];
    } catch (range_error &ex) {
      try {
	if (ww["route"] == "ferry") {
	  hw = "unclassified";
	  speed = 12;    // vehicle speed is niet belangrijk
 	  extracost= 600 / cnt; // 10 minuten vertraging
	}
      } catch (range_error &ex) {
	return INFINITY;
      }
    }

// bepaal maximum snelheid
// eerst uit de weg halen anders de standaard snelheid voor zo'n weg
    
    try {
      double maxspeed = atol(ww["maxspeed"].c_str());
      if (maxspeed < speed)
	speed = maxspeed;
    } catch (range_error &ex) {
      double defspeed = _highways[hw].speed();
      if (defspeed < speed)
	speed = defspeed;
    }

    if (speed == 0)
      return INFINITY;

// basis kost is de tijd die je er op nominale snelheid over doet

    double kost = dist *3.6 / speed;

// beperkingen aflopen

    string access;
    string oneway;
    try { access = ww["access"];} catch (range_error &ex) {access="yes";}
    try { oneway = ww["oneway"];} catch (range_error &ex) {oneway="";}
    try {
      if (ww["junction"] == "roundabout" && oneway == "")
	oneway = "yes";
    } catch (range_error &ex) {}

    extracost += _highways[hw].extracost()*dist/EXTRACOST_FACTOR;   // extracost gaat per kilometer als ze op een weg slaan    
    Node &nodey = nodes(y);                               // node waar we naar toe gaan

    if (_vehicle == "foot") {
      string fa;
      try { fa = ww["foot"];} catch (range_error &ex) {fa="";}
      if (fa == "") {
	try {
	  if (ww["motorroad"] == "yes") {
	    fa="no";
	  }
	} catch (range_error &ex) {}	
      }
      if ((fa == "no") || (access == "no" && fa != "yes"))
	return INFINITY;
      try { extracost += _profiles[_vehicle].allowed(hw)*dist/EXTRACOST_FACTOR;} catch (range_error &ex) {return INFINITY;}
      try { extracost += _highways[nodey["highway"]].extracost();} catch (range_error &ex) {};
    } else if (_vehicle == "bicycle") {
      string cw;
      try { cw = ww["cycleway"];} catch (range_error &ex) {cw="";}
      if (cw == "opposite_lane" || cw == "opposite_track" || cw == "both")
	cw = "opposite";
      string ca;
      try { ca = ww["bicycle"];} catch (range_error &ex) {ca="";}
      if (ca == "") {
	try {
	  if (ww["motorroad"] == "yes") {
	    ca="no";
	  }
	} catch (range_error &ex) {}	
      }

      if (ca != "yes") {  // als expliciet toegestaan dan geen extracost meenemen (dus extracost voor dit type weg is dan 0 ongeacht profiel waarde)
	try { 
	  extracost += _profiles[_vehicle].allowed(hw)*dist/EXTRACOST_FACTOR;
	} catch (range_error &ex) {
	  if (cw == "") 
	    return INFINITY;
	}
      }
      if (cw != "")
	extracost = 0; // prefer cycle track or lane
      else {
        if ((ca == "no") || (access == "no" && ca != "yes"))
	  return INFINITY;
      }
      if (oneway != "" && cw != "opposite") {
	if (wrong_direction(nodes(x),nodey,ww,oneway))
	  return INFINITY;
     }
      try {extracost += _highways[nodey["highway"]].extracost();} catch (range_error &ex) {};
    } else if (_vehicle == "car") {
      string ma;
      try { ma = ww["motorcar"];} catch (range_error &ex) {ma="";}
      if ((ma == "no") || (access == "no" && ma != "yes"))
	return INFINITY;
      try { extracost += _profiles[_vehicle].allowed(hw)*dist/EXTRACOST_FACTOR;} catch (range_error &ex) {return INFINITY;}
      if (oneway != "") {
	if (wrong_direction(nodes(x),nodey,ww,oneway))
	  return INFINITY;
      }
      try { extracost += _highways[nodey["highway"]].extracost();} catch (range_error &ex) {};
    }
    
//  als nog steeds access = no dan mag het echt niet

    if (access == "no")
      return INFINITY;

    extracost += curvecost(x,y,prevnode) * _profiles[_vehicle].curvefactor();
// nog wat beperkingen  (als waarde * dan voor alle niet expliciet gespecificeerde in profiel)
      
    try { 
      extracost += _profiles[_vehicle].traffic_calming(nodey["traffic_calming"]);
    } catch (range_error &ex) {
      try { extracost += _profiles[_vehicle].traffic_calming("*");} catch (range_error &ex1) {};
    };
    try { 
      extracost += _profiles[_vehicle].barrier(nodey["barrier"]);
    } catch (range_error &ex) {
      try { extracost += _profiles[_vehicle].barrier("*");} catch (range_error &ex1) {};
    };
    try { 
      extracost += _profiles[_vehicle].barrier(nodey["highway"]);
    } catch (range_error &ex) {
      try { extracost += _profiles[_vehicle].highway("*");} catch (range_error &ex1) {};
    };
    
    if (extracost >= INFINITY)
      return INFINITY;

    if (!ignoreExtra)
      kost += extracost;
    if (kost > INFINITY)
      kost = INFINITY;
    
    return kost;
  }
  
  double Map::calc_h_score(const long n1,const long n2) {
    double dist = distance(n1,n2);
    if (_vehicle == "") return dist;

    return dist * 1.75 * 3.6 / _profiles[_vehicle].avgspeed();
  }

  long bestpoints[3];
  double initialdistance;
  double maxperc = 0;
  
  long Map::AstarHelper(int set,long goal,set_type &openset,set_type &closedset,score_type &f,score_type &g,score_type &h,score_type &d,route_type &to,const bool ignoreExtra) {
    long xs = 0;
    set_type::iterator k;
    
    for (k=openset.begin();k!=openset.end();k++) {
      if (xs == 0) 
	xs = k->first;
      if (f[k->first] < f[xs])
	xs = k->first;
    }
    if (xs == 0)  // no more nodes to process should not happen in our situation 
      throw runtime_error("xs = 0");
      
//    cerr << "xs = " << xs << endl;
    
    bestpoints[set] = xs;
    double newdistance = distance(bestpoints[1],bestpoints[2]);
    double curperc = 100.0*(initialdistance - newdistance)/initialdistance;
    maxperc = max(maxperc,curperc);

    k = closedset.find(xs);
    if (k != closedset.end() && k->second != set) {
      cout << "S set = " << set << " xs match = " << xs << endl;
      return xs;
    }
    openset.erase(xs);
    closedset[xs]=set;

    long prevnode = 0;
    route_type::iterator pn;
    pn = to.find(xs);
    if (pn != to.end())
      prevnode = pn->second;

    vector<long> neighbours;
    getNeighbours(xs,neighbours);

    for (vector<long>::iterator yi=neighbours.begin();yi != neighbours.end();yi++) {
      long y = *yi;
      
//      cerr << "y = " << y << endl;

      if (closedset.find(y) == closedset.end()) { // && g[xs] != INFINITY) {
//        cerr << "not closed"<<endl;
	double tentative_g_score = g[xs] + (set==1?cost(xs,y,prevnode,ignoreExtra):cost(y,xs,prevnode,ignoreExtra));
	if (tentative_g_score >INFINITY)
	  tentative_g_score = INFINITY;
	bool tentative_is_better = false;

	if (openset.find(y) == openset.end()) {
	  openset[y] = 1;
//          cerr << "added to openset" << endl;
        }
        if (g.find(y) != g.end()) {
          if (tentative_g_score < g[y] || g[y] == 0) {
            tentative_is_better = true;
	        cout << "verbetering van " << y << endl << "oud = " << g[y] << " nieuw = " << tentative_g_score << endl;
//
// alle nodes die vanaf y bereikbaar zijn moeten opnieuw worden berekend (behalve xs1)
// dus als ze al in closedset staan daar weer uit verwijderen
//	  
            vector<long> ynb;
            getNeighbours(y,ynb);
            for (vector<long>::iterator y1i=ynb.begin();y1i != ynb.end();y1i++) {
              long y1=*y1i;
              if (y1 != xs) {
                k = closedset.find(y1);
                if (k != closedset.end()) {
                  if (k->second == set) {
                    cout << "   opnieuw open " << y1 << endl;
//                    closedset.erase(y1);
                  }
                }
              }
            }
          }
        } else { 
          tentative_is_better = true;
//          cerr << "bestond nog niet" << endl;
	}
        
	  string name;
	  string ref;
	  long w = getConnectingWay(xs,y);
	  osm::Way &ww = ways(w);
	  try { name = ww["name"];} catch (range_error &ex) {name="";}
	  try { ref = ww["ref"];} catch (range_error &ex) {ref="";}
	if (tentative_is_better) {
	  to[y] = xs;
	  g[y] = tentative_g_score;
	  h[y] = calc_h_score(y,goal);
	  d[y] = distance(y,goal);
	  f[y] = g[y] + h[y];
	  if (f[y] > INFINITY)
	    f[y] = INFINITY;
	  string name;
	  string ref;
	  long w = getConnectingWay(xs,y);
	  osm::Way &ww = ways(w);
	  try { name = ww["name"];} catch (range_error &ex) {name="";}
	  try { ref = ww["ref"];} catch (range_error &ex) {ref="";}
	  cout << set << " " << maxperc << " " <<  curperc ;
	  if (set == 1) 
	    cout << " " << xs << " " << y << " " << f[xs] << " " << g[y] << " " << h[y] << " " << f[y] << " "+name+" "+ref <<endl;
	  else
	    cout << " " << y << " " << xs << " " << f[xs] << " " << g[y] << " " << h[y] << " " << f[y] << " "+name+" "+ref <<endl;
	} else {
	  cout << set << " XXXXXX";
	  if (set == 1) 
	    cout << " " << xs << " " << y << " " << f[xs] << " " << g[y] << "/" << tentative_g_score << " " << h[y] << " " << f[y] << " "+name+" "+ref <<endl;
	  else
	    cout << " " << y << " " << xs << " " << f[xs] << " " << g[y] << "/" << tentative_g_score << " " << h[y] << " " << f[y] << " "+name+" "+ref <<endl;
        }
      }
    }
    return 0;
  }
  
  double Map::Astar(const long n1,const long n2,const string &vehicle,list<long> &route,const bool ignoreExtra) {
    set_type startset,goalset,closedset;
    score_type gs_score,hs_score,fs_score,ds_score, gg_score,hg_score,fg_score,dg_score;
    route_type came_from,goes_to;
    long xs = 0;

    initRoute(vehicle);
    startset[n1] = 1;
    goalset[n2] = 1;
    gs_score[n1] = 0;
    initialdistance = dg_score[n2] = ds_score[n1] = distance(n1,n2);
    bestpoints[1] = n1;
    bestpoints[2] = n2;
    
    while (xs == 0 && (!startset.empty() || !goalset.empty())) {
      xs = AstarHelper(1,n2,startset,closedset,fs_score,gs_score,hs_score,ds_score,came_from,ignoreExtra);
      if (xs == 0)
        xs = AstarHelper(2,n1,goalset,closedset,fg_score,gg_score,hg_score,dg_score,goes_to,ignoreExtra);
    }

    route.clear();
    if (xs == 0)
        throw range_error("Niet mogelijk om een route te berekenen");
    route.push_back(xs);
    route_type::iterator rp;

    long x = xs;
    while ((rp = came_from.find(x)) != came_from.end()) {
      x = rp->second;
      route.push_front(x);
    }
    x = xs;
    while ((rp = goes_to.find(x)) != goes_to.end()) {
      x = rp->second;
      route.push_back(x);
    }

    return 0;
  }
  
  double Map::Astar(const long n1,const double lat2,const double lon2,const string &vehicle,list<long> &route,const bool ignoreExtra) {
    return 0;
  }
  
  double Map::Astar(const double lat1,const double lon1,const long n2,const string &vehicle,list<long> &route,const bool ignoreExtra) {
    return 0;
  }
  
  double Map::Astar(const double lat1,const double lon1,const double lat2,const double lon2,const string &vehicle,list<long> &route,const bool ignoreExtra) {
    return 0;
  }
}
