#include "osm.h"
#include "astarconf.h"
#include <map>

namespace osm {
  using namespace std;

  double Map::Astar(const Node &n1,const Node &n2) {
  map<long,bool> startset,goalset,closedset;
  map<long,double> gs_score,hs_score,fs_score,ds_score, gg_score,hg_score,fg_score,dg_score;
  map<long,long> came_from,goes_to;

  startset[n1.id()] = true;
  goalset[n2.id()] = true;
  gs_score[n1.id()] = 0;
  dg_score[n2.id()] = ds_score[n1.id()] = distance(n1,n2);
  
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
