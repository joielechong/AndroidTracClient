#include "osm.h"
#include "osm_db.h"
#include <string>
#include <sstream>
#include <stdbool.h>

namespace osm {
  using namespace std;
  
  void Relation::addMember(long ref,string type,string role) {
    Member *mem = new Member(ref,type,role);
    _members.push_back(*mem);
    delete mem;
  }
  
  string Relation::output () {
    stringstream s;
    unsigned int i;
    
    s << "Relation: Id = " << _id << " version = " << _version << endl;;
    
    for(i=0;i<_members.size();i++) 
      s << "   Member[" << i << "] : " << _members[i].ref() << "," << _members[i].type() << "," << _members[i].role() << endl;  
    
    s << printTags();
    return s.str();
  }
  
  Member::Member(long ref,string type, string role) : _ref(ref),_role(role),_type(type) {}
  
  Relation::Relation(long id,osm_db::database &con) {
    _id = id;
    _coordsLoaded=false;
    con.getRelation(id,_version);
    getTags("relation",con);
    std::vector<long> ref;
    std::vector<std::string> type;
    std::vector<std::string> role;
    con.getMembers(id,type,role,ref);
    for (unsigned int i=0;i<ref.size();i++) 
      _members.push_back(Member(ref[i],type[i],role[i]));
  }

  bool Relation::isInside(osm_db::database *con,const double lat,const double lon) {
    try {
      if (!_coordsLoaded) {
	con->getRelCoords(_id,_lats,_lons);
	_coordsLoaded = true;
      }
    } catch  (const osm_db::osm_db_error &ex) {
      cerr <<ex.what()<<endl;
      cerr << "  errmsg = " << con->errmsg() << endl;
      return false;
    }
    int nvert = _lats.size();
    bool c=false;
    //    cout << nvert << " nodes"<<endl;
    int j=nvert - 1;
    for (int i=0;i<nvert;j=i++) {
      if ((_lons[i] > lon ) != (_lons[j] > lon)) {
	double l2 = (_lats[j]-_lats[i])*(lon-_lons[i])/(_lons[j]-_lons[i]) + _lats[i];
	//	cout << "l2 = " << l2 << endl;
	if (lat < l2)
	  c = !c;
      }
      //      cout << i << " " << lon << " " << _lons[j] << " " << _lons[i] << " : " << lat << " " << _lats[j] << " " << _lats[i] << " = " << c << endl;
    }
    return c;
  }
}
