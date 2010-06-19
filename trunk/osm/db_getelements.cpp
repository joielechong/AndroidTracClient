#include "osm_db.h"
#include <sqlite3x.hpp>
#include <string>
#include <iostream>
#include <cstring>
#include <vector>
#include <stdexcept>

namespace osm_db {  
  using namespace std;
  using namespace sqlite3x;
  
  void database::getCounts(long &nodes,long &ways,long &rels, long &bounds, long &tags,long &nds, long &mems) {
    if (_getCounts == NULL) 
      _getCounts = new sqlite3_command(*_sql,"SELECT * FROM counts");
    sqlite3_cursor cur(_getCounts->executecursor());
    cur.step();
    nodes = cur.getint64(0);
    ways = cur.getint64(1);
    rels = cur.getint64(2);
    bounds = cur.getint64(3);
    tags = cur.getint64(4);
    nds = cur.getint64(5);
    mems = cur.getint64(6);
  }
  
  void database::getNode(long id,int &version,double &lat,double &lon, int&x, int &y) {
    //    std::cout << "getNode("<<id<<")"<<std::endl;
    try {
      if (_getNode == NULL) 
        _getNode = new sqlite3_command(*_sql,"SELECT version,lat,lon,x,y FROM node  WHERE id = ?");
      _getNode->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getNode->executecursor());
      if (cur.step()) {
	version = cur.getint(0);
	lat = cur.getdouble(1);
	lon = cur.getdouble(2);
	x = cur.getint(3);
	y = cur.getint(4);
      } else
        throw osm_db_error("Node does not exist: %ld",id);
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cerr << "Exception in sqlite: " << ex.what() <<endl;
      cerr << "  errmsg = " << this->errmsg() << endl;
      throw osm_db_error("Node %d returned database error",id);
    }
  }
  
  void database::getWay(long id,int &version) {
    //    cout << "getWay("<<id<<")"<<endl;
    try {
      if (_getWay == NULL) 
        _getWay = new sqlite3_command(*_sql,"SELECT version FROM node  WHERE id = ?");
      _getWay->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getWay->executecursor());
      if (cur.step())
	version = cur.getint(0);
      else
	version = -1;
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cerr << "Exception in sqlite: " << ex.what() <<endl;
      cerr << "  errmsg = " << this->errmsg() << endl;
      version = -1;
    }
  }
  
  void database::getRelation(long id,int &version) {
    //    cout << "getRelation("<<id<<")"<<endl;
    try {
      if (_getRelation == NULL) 
        _getRelation = new sqlite3_command(*_sql,"SELECT version FROM relation  WHERE id = ?");

      _getRelation->bind(1,(sqlite3x::int64_t)id);
      sqlite3_cursor cur(_getRelation->executecursor());
      if (cur.step())
	version = cur.getint(0);
      else
	version = -1;
      cur.close();
    } catch (const sqlite3x::database_error& ex) {
      cerr << "Exception in sqlite: " << ex.what() <<endl;
      cerr << "  errmsg = " << this->errmsg() << endl;
      version = -1;
    }
  }

  void database::getTags(const long id,string type,vector<string> &k,vector<string> &v) {
    if (_getTags == NULL) 
      _getTags = new sqlite3_command(*_sql,"SELECT k,v FROM  tag WHERE id=? and type = ?");

    k.clear();
    v.clear();
    _getTags->bind(1,(sqlite3x::int64_t)id);
    _getTags->bind(2,type);
    sqlite3_cursor cur(_getTags->executecursor());
    while(cur.step()) {
      k.push_back(cur.getstring(0));
      v.push_back(cur.getstring(1));
    }
  }

  void database::getNds(long id,vector<long> &ref) {
    if (_getNds == NULL) 
      _getNds = new sqlite3_command(*_sql,"SELECT seq,ref FROM nd WHERE id=?");

    ref.clear();
    _getNds->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_getNds->executecursor());
    while(cur.step()) {
      unsigned int seq = cur.getint(0);
      long r = cur.getint64(1);
      while (ref.size() < seq)
	ref.push_back(-1);
      if (seq < ref.size())
	ref[seq] = r;
      else
	ref.push_back(r);
    }
  }

  void database::getWays(const long nodeid,vector<long> &ways) {
    if (_getWays == NULL) 
      _getWays = new sqlite3_command(*_sql,"SELECT way FROM nb WHERE id1=?");

    ways.clear();
    _getWays->bind(1,(sqlite3x::int64_t)nodeid);
    sqlite3_cursor cur(_getWays->executecursor());
    while(cur.step())
      ways.push_back(cur.getint64(0));
  }

  long database::getConnectingWay(const long node1,const long node2) {
    if (_getConn == NULL)
      _getConn = new sqlite3_command(*_sql,"SELECT way FROM nb WHERE id1=? and id2=?");
    _getConn->bind(1,(sqlite3x::int64_t)node1);
    _getConn->bind(2,(sqlite3x::int64_t)node2);
    sqlite3_cursor cur(_getConn->executecursor());
    if (cur.step())
      return cur.getint64(0);
    else
      return 0;
  }

  void database::getMembers(long id,vector<string> &type,vector<string> &role,vector<long> &ref) {
    if (_getMembers == NULL) 
      _getMembers = new sqlite3_command(*_sql,"SELECT seq,type,role,ref FROM member WHERE id=?");

    type.clear();
    role.clear();
    ref.clear();
    _getMembers->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_getMembers->executecursor());
    while(cur.step()) {
      unsigned int seq = cur.getint(0);
      while (ref.size() < seq) {
	ref.push_back(-1);
	type.push_back("");
	role.push_back("");
      }
      if (seq < ref.size()) {
	type[seq] = cur.getstring(1);
	role[seq] = cur.getstring(2);
	ref[seq] = cur.getint64(3);
      } else {
	type.push_back(cur.getstring(1));
	role.push_back(cur.getstring(2));
	ref.push_back(cur.getint64(3));
      }
    }
  }

  void database::findCoor(const double lat,const double lon,vector<long> &ways,vector<long> &nodes,vector<double> &distances) {
    if (_findCoor == NULL) 
      _findCoor = new sqlite3_command(*_sql,"SELECT DISTINCT osmdistance(inp.lat,inp.lon,n.lat,n.lon),n.id,wt.id FROM (SELECT ? as lon,? as lat) as inp,node as n,nb,waytag as wt WHERE n.x=osmcalc_x(inp.lon) and n.y=osmcalc_y(inp.lat) and abs(inp.lat-n.lat)<0.001 and abs(inp.lon-n.lon) <0.001 and n.id=nb.id1 and nb.way=wt.id and osmdistance(inp.lat,inp.lon,n.lat,n.lon)<50 order by 1");
    ways.clear();
    nodes.clear();
    distances.clear();
    _findCoor->bind(1,lon);
    _findCoor->bind(2,lat);
    sqlite3_cursor cur(_findCoor->executecursor());
    while (cur.step()) {
      ways.push_back(cur.getint64(2));
      nodes.push_back(cur.getint64(1));
      distances.push_back(cur.getdouble(0));
    }
  }

  void database::findNode(const double latinp,const double loninp,const double diff,vector<long> &id,vector<double> &lat,vector<double> &lon,vector<double> &distance) {
    if (_findNode == NULL)
      _findNode = new sqlite3_command(*_sql,"SELECT node.id,node.lat,node.lon,osmdistance(node.lat,node.lon,inp.lat,inp.lon) from node,(select ? as lat,? as lon,? as diff) as inp WHERE node.x >= osmcalc_x(inp.lon-inp.diff*2) AND node.x <= osm_calc_x(inp.lon+inp.diff*2) and node.y >= osmcalc_y(inp.lat-inp.diff*2) AND node.y <=smcalc_y(inp.lat+inp.diff*2) AND abs(node.lat-inp.lat) < inp.diff and abs(node.lon-inp.lon) < inp.diff order by 4");

    id.clear();
    lat.clear();
    lon.clear();
    distance.clear();
    _findNode->bind(1,latinp);
    _findNode->bind(2,loninp);
    _findNode->bind(3,diff);
    sqlite3_cursor cur(_findNode->executecursor());
    while (cur.step()) {
      id.push_back(cur.getint64(0));
      lat.push_back(cur.getdouble(1));
      lon.push_back(cur.getdouble(2));
      distance.push_back(cur.getdouble(3));
    }
  }
  //  select nd.id,nd.seq,nd.ref from member,nd where member.id=162577 and member.type='way' and member.ref=nd.id and (nd.seq=0 or nd.seq = (select max(seq) from nd where id=member.ref));

  void database::getRelCoords(long relationid, vector<double> &lat,vector<double> &lon) {
    if (_getRelWays == NULL)
      _getRelWays = new sqlite3_command(*_sql,"SELECT member.ref,(SELECT ref FROM nd WHERE id=member.ref ORDER BY seq LIMIT 1) AS first,(SELECT ref FROM nd WHERE id=member.ref ORDER BY seq DESC LIMIT 1) AS last FROM member WHERE id=? AND type='way' ");
    if (_getWayAsc == NULL)
      _getWayAsc = new sqlite3_command(*_sql,"SELECT lat,lon FROM nd,node WHERE nd.id=? AND nd.ref=node.id ORDER BY seq ASC");
    if (_getWayDesc == NULL)
      _getWayDesc = new sqlite3_command(*_sql,"SELECT lat,lon FROM nd,node WHERE nd.id=? AND nd.ref=node.id ORDER BY seq DESC");

    lat.clear();
    lon.clear();
    vector<long> ways;
    vector<long> firstnode;
    vector<long> lastnode;
    vector<bool> used;

    { // destroys cur after execution
      _getRelWays->bind(1,(sqlite3x::int64_t)relationid);
      sqlite3_cursor cur(_getRelWays->executecursor());
      while (cur.step()) {
	//	cout << "rel " << ways.size() << " " << cur.getint64(0) << " " <<cur.getint64(1) << " " << cur.getint64(2) << endl; 
	ways.push_back(cur.getint64(0));
	firstnode.push_back(cur.getint64(1));
	lastnode.push_back(cur.getint64(2));
	used.push_back(false);
      }
    }    
    long first = -1;
    long next = firstnode[0];
    
    int curway = 0;
    bool asc = true;
    while (first != next) {
      bool found = false;
      for (unsigned int i=0;!found && i<firstnode.size();i++) {
	if (!used[i]) {
	  if (firstnode[i] == next) {
	    found = true;
	    asc = true;
	    curway = i;
	    next = lastnode[i];
	  } else if (lastnode[i] == next) {
	    found = true;
	    asc = false;
	    curway = i;
	    next = firstnode[i];
	  }
	}
      }

      if (!found) 
	throw osm_db_error("relatie %d niet goed: next = %d",relationid,next);

      if (first == -1)
	first = firstnode[curway];

      sqlite3_command *q;
      if (asc) 
	q = _getWayAsc;
      else
	q = _getWayDesc;
      q->bind(1,(sqlite3x::int64_t)ways[curway]);
      sqlite3_cursor cur(q->executecursor());
      while (cur.step()) {
	lat.push_back(cur.getdouble(0));
	lon.push_back(cur.getdouble(1));
      }
      used[curway] = true;
    }
  }

  void database::findAdmin(const double lat,const double lon,vector<long> &ids,vector<string> &names, vector<int> & admlevel) {
    if (_findAdmin == NULL) 
      _findAdmin = new sqlite3_command(*_sql,"SELECT admin.id,admin.name,admin.level FROM admin WHERE  ? BETWEEN minlat AND maxlat AND ? BETWEEN minlon AND maxlon ORDER BY admin.level");

    ids.clear();
    names.clear();
    admlevel.clear();

    _findAdmin->bind(1,lat);
    _findAdmin->bind(2,lon);
    sqlite3_cursor cur(_findAdmin->executecursor());
    while (cur.step()) {
      ids.push_back(cur.getint64(0));
      names.push_back(cur.getstring(1));
      admlevel.push_back(cur.getint(2));
    }
  }
  void database::ndAddress(const string querystring,vector<long> &ways,vector<long> &nodes,vector<double> &distances,vector<long> &intpolways,vector<long> &assocways) {
    sqlite3_command fa(*_sql,"SELECT DISTINCT osmdistance(inp.lat,inp.lon,n.lat,n.lon),n.id,wt.id,intpolway,assocway FROM (SELECT lat,lon,intpolway,assocway FROM adressen,node WHERE "+querystring+" AND adressen.type='node' AND adressen.id=node.id UNION SELECT lat,lon,intpolway,assocway FROM adressen,nd,node WHERE "+querystring+" AND adressen.type='way' AND adressen.id=nd.id AND nd.ref=node.id) as inp,node as n,nb,waytag as wt WHERE n.x=osmcalc_x(inp.lon) and n.y=osmcalc_y(inp.lat) and abs(inp.lat-n.lat)<0.001 and abs(inp.lon-n.lon) <0.001 and n.id=nb.id1 and nb.way=wt.id and osmdistance(inp.lat,inp.lon,n.lat,n.lon)<50 order by 1");
    ways.clear();
    nodes.clear();
    distances.clear();
    intpolways.clear();
    assocways.clear();
    sqlite3_cursor cur(fa.executecursor());
    while (cur.step()) {
      ways.push_back(cur.getint64(2));
      nodes.push_back(cur.getint64(1));
      distances.push_back(cur.getdouble(0));
      intpolways.push_back(cur.getint64(3));
      assocways.push_back(cur.getint64(4));
    }
  }

  void database::findAddress(string querystring,vector<long> &nodeids,vector<double> &nodelats,vector<double> &nodelons){
     sqlite3_command fa(*_sql,"SELECT node.id,lat,lon FROM adressen,node WHERE "+querystring+" AND adressen.type='node' AND adressen.id=node.id UNION SELECT node.id,lat,lon FROM adressen,nd,node WHERE "+querystring+" AND adressen.type='way' AND adressen.id=nd.id AND nd.ref=node.id ");
    sqlite3_cursor cur(fa.executecursor());
    while (cur.step()) {
      nodeids.push_back(cur.getint64(0));
      nodelats.push_back(cur.getdouble(1));
      nodelons.push_back(cur.getdouble(2));
    }
  }

  void database::findHouses(long id,vector<long> &ids,vector<double> &nodelats,vector<double> &nodelons,vector<string> &countries,vector<string> &cities,vector<string> &streets,vector<string> &housenumbers,vector<string> &postcodes) {
    if (_findHouses == NULL) 
      _findHouses = new sqlite3_command(*_sql,"SELECT node.id,node.lat,node.lon,country,city,street,housenumber,postcode FROM nd,node,adressen WHERE nd.id=? AND nd.ref=node.id and node.id=adressen.id ORDER by nd.seq");

    ids.clear();
    nodelats.clear();
    nodelons.clear();
    countries.clear();
    cities.clear();
    streets.clear();
    housenumbers.clear();
    postcodes.clear();

    _findHouses->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_findHouses->executecursor());
    while (cur.step()) {
      ids.push_back(cur.getint64(0));
      nodelats.push_back(cur.getdouble(1));
      nodelons.push_back(cur.getdouble(2));
      countries.push_back(cur.getstring(3));
      cities.push_back(cur.getstring(4));
      streets.push_back(cur.getstring(5));
      housenumbers.push_back(cur.getstring(6));
      postcodes.push_back(cur.getstring(7));
    }
  }

  void database::getInterpolationWays(vector<long> &ids) {
    sqlite3_command s(*_sql,"SELECT id FROM waytag WHERE k='addr:interpolation'");
    sqlite3_cursor cur(s.executecursor());
    while (cur.step()) {
      ids.push_back(cur.getint64(0));
    } 
  }

  void database::getids(string &sqlcmd,vector<long> &ids) {
    sqlite3_command getids(*_sql,sqlcmd);
    sqlite3_cursor cur(getids.executecursor());

    ids.clear();
    while (cur.step())
      ids.push_back(cur.getint64(0));
    
  }

  void database::getNeighbours(const long id,vector<long> &ids) {
    if (_getNeighbours == NULL)
      _getNeighbours = new sqlite3_command(*_sql,"SELECT id2 FROM nb WHERE id1=?");
    _getNeighbours->bind(1,(sqlite3x::int64_t)id);
    sqlite3_cursor cur(_getNeighbours->executecursor());

    ids.clear();
    while (cur.step())
      ids.push_back(cur.getint64(0));
  
  }

  int database::getDirection(const long x,const long y,const long w) {
    if (_getDirection == NULL)
      _getDirection = new sqlite3_command(*_sql,"SELECT (x.seq-y.seq) FROM nd AS x,nd AS y  WHERE x.id=? AND x.ref=? AND y.id=x.id AND y.ref=? AND abs(y.seq-x.seq)==1");
    _getDirection->bind(1,(sqlite3x::int64_t)w);
    _getDirection->bind(2,(sqlite3x::int64_t)x);
    _getDirection->bind(3,(sqlite3x::int64_t)y);
    sqlite3_cursor cur(_getDirection->executecursor());
    if (cur.step())
      return cur.getint64(0);
    return 0;
  }
  
  void database::adminNode(const long nodeid,vector<long> &admins) {
    if (_adminNode == NULL)
      _adminNode = new sqlite3_command(*_sql,"SELECT admin.id FROM admin,node WHERE node.id=? and lat >= minlat and lat <= maxlat and lon >= minlon and lon <= maxlon ORDER BY level DESC,name");
    _adminNode->bind(1,(sqlite3x::int64_t)nodeid);
    sqlite3_cursor cur(_adminNode->executecursor());
    admins.clear();
    while (cur.step())
      admins.push_back(cur.getint64(0));
  }
  
}
