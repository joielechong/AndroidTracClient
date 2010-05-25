#include "osm.h"
#include "astarconf.h"
#include <map>

namespace osm {
  using namespace std;
  
  typedef typename map<long,bool> set_type;
  typedef typename map<long,double> score_type;
  typedef typename map<long,long> route_type; 
  
  static void AstarHelper(int set,long goal,set_type &openset,set_type &closedset,score_type &f,score_type &g,score_type &h,score_type &d,route_type &to) {
  }

  double Map::Astar(const long n1,const long n2) {
  set_type startset,goalset,closedset;
  score_type gs_score,hs_score,fs_score,ds_score, gg_score,hg_score,fg_score,dg_score;
  route_type came_from,goes_to;

  startset[n1] = true;
  goalset[n2] = true;
  gs_score[n1] = 0;
  dg_score[n2] = ds_score[n1] = distance(n1,n2);
  while (!startset.empty() || !goalset.empty()) {
    AstarHelper(1,n1,startset,closedset,fs_score,gs_score,hs_score,ds_score,came_from);
    AstarHelper(2,n2,goalset,closedset,fg_score,gg_score,hg_score,dg_score,goes_to);
  }
  
    return 0;
  }

  double Map::Astar(const long n1,const double lat2,const double lon2) {
    return 0;
  }

  double Map::Astar(const double lat1,const double lon1,const long n2) {
    return 0;
  }

  double Map::Astar(const double lat1,const double lon1,const double lat2,const double lon2) {
    return 0;
  }
}
