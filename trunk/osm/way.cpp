#include "osm.h"
#include "osm_db.h"
#include <sstream>

namespace osm {
  using namespace std;
  
  //Way
  
  string Way::output () {
    stringstream s;
    unsigned int i;
    
    s << "Way: Id = " << _id << " version = " << _version << endl;
    
    for(i=0;i<_nds.size();i++) 
      s << "   Node[" << i << "] : " << _nds[i] << endl;  
    
    s << printTags();
    return s.str();
  }
  
  Way::Way(long id,osm_db::database &con) {
    _id = id;
    con.getWay(id,_version);
    getTags(id,"way",con);
    con.getNds(id,_nds);
  }
}
