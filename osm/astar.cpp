#include "osm.h"
#include <map>

namespace osm {
  using namespace std;
  
  void Map::initRoute(const string &vehicle) {
    if (vehicle != "" && _profiles.find(vehicle) == _profiles.end())
        throw domain_error("Onbekend voertuig : "+vehicle);
    _vehicle = vehicle;
  }

  double Map::cost(const long n1,const long n2,const long prevnode) { 
    double dist = distance(n1,n2);
    if (_vehicle == "") return dist;
    return dist;
  }

  double Map::calc_h_score(const long n1,const long n2) {
    double dist = distance(n1,n2);
    if (_vehicle == "") return dist;
    return dist;
  }
  
  long Map::AstarHelper(int set,long goal,set_type &openset,set_type &closedset,score_type &f,score_type &g,score_type &h,score_type &d,route_type &to) {
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

    k = closedset.find(xs);
    if (k != closedset.end() && k->second != set) {
      cout << "set = " << set << " xs match = " << xs << endl;
      return xs;
    }
    openset.erase(xs);
    closedset[xs]=set;
    vector<long> neighbours;
    vector<long>::iterator y;

    getNeighbours(xs,neighbours);
    for (y=neighbours.begin();y != neighbours.end();y++) {
      long xs1=xs;
      long prevnode = 0;
      route_type::iterator pn;
      pn = to.find(xs1);
      if (pn != to.end())
	prevnode = pn->second;
      k = closedset.find(*y);
      if (k == closedset.end()) {
	double tentative_g_score = g[xs1] + (set==1?cost(xs1,*y,prevnode):cost(*y,xs1,prevnode));
	bool tentative_is_better = false;
	k = openset.find(*y);
	if (k == openset.end()) {
	  openset[*y] = 1;
	  tentative_is_better = true;
	} else if (tentative_g_score < g[*y]) 
	  tentative_is_better = true;
	if (tentative_is_better) {
	  to[*y] = xs1;
	  g[*y] = tentative_g_score;
	  h[*y] = calc_h_score(*y,goal);
	  d[*y] = distance(*y,goal);
	  f[*y] = g[*y] + h[*y];
	  if (set == 1) 
	    cout << set << " " << xs << " " << *y << " " << g[*y] << " " << h[*y] << " " << f[*y] << endl;
	  else
	    cout << set << " " << *y << " " << xs << " " << g[*y] << " " << h[*y] << " " << f[*y] << endl;
	}
      }
    }
    return 0;
  }
  
  double Map::Astar(const long n1,const long n2,const string &vehicle,list<long> &route) {
    set_type startset,goalset,closedset;
    score_type gs_score,hs_score,fs_score,ds_score, gg_score,hg_score,fg_score,dg_score;
    route_type came_from,goes_to;
    long xs = 0;

    initRoute(vehicle);
    startset[n1] = 1;
    goalset[n2] = 1;
    gs_score[n1] = 0;
    dg_score[n2] = ds_score[n1] = distance(n1,n2);
    while (xs == 0 && (!startset.empty() || !goalset.empty())) {
      xs = AstarHelper(1,n2,startset,closedset,fs_score,gs_score,hs_score,ds_score,came_from);
      if (xs == 0)
        xs = AstarHelper(2,n1,goalset,closedset,fg_score,gg_score,hg_score,dg_score,goes_to);
    }

    route.clear();
    if (xs == 0)
        throw range_error("Niet mogelijk om een royute te berekenen");
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
  
  double Map::Astar(const long n1,const double lat2,const double lon2,const string &vehicle,list<long> &route) {
    return 0;
  }
  
  double Map::Astar(const double lat1,const double lon1,const long n2,const string &vehicle,list<long> &route) {
    return 0;
  }
  
  double Map::Astar(const double lat1,const double lon1,const double lat2,const double lon2,const string &vehicle,list<long> &route) {
    return 0;
  }
}
