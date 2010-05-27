#include "osm.h"
#include "osm_db.h"
#include <sstream>

namespace osm {
  using namespace std;
  
  // Node
  
  string Node::output () {
    stringstream s;
    s << "Node: Id = " << _id << " version = " << _version << " lat,lon = " << _lat << " , " << _lon << " x,y = " << _x << "," << _y<< endl;
    s << printTags();
    return s.str();
  }
   
  Node::Node(long id,osm_db::database &con) {
    _id = id;
    con.getNode(id,_version,_lat,_lon,_x,_y);
    getTags(id,"node",con);
  }

}
