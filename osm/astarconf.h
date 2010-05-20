#ifndef _ASTARCONF_H
#define _ASTARCONF_H

#include "config.h"
#include <string>
#include "osm_db.h"
#include <iostream>
#include <cstdlib>
#include <map>

namespace osm {
  
  using namespace std;
  class Highway {
  public:
    inline Highway() : _extracost(0),_speed(0) {}
    inline unsigned int speed() const {return _speed;}
    inline void speed(const unsigned int s) {_speed=s;}
    inline unsigned int extracost() const {return _extracost;}
    inline void extracost(const unsigned int e) {_extracost=e;}

    inline void output(ostream &out) {out << "Speed = " << _speed << " extracost = " << _extracost;}

  private:
    int _extracost;
    unsigned int _speed;
  };

  class Profile {
  public:
    inline Profile() : _maxspeed(0),_avgspeed(0),_ignore_oneway(false) {}

    inline unsigned int maxspeed() const {return _maxspeed;}
    inline void maxspeed(const unsigned int s) {_maxspeed=s;}
    inline unsigned int avgspeed() const {return _avgspeed;}
    inline void avgspeed(const unsigned int s) {_avgspeed=s;}
    inline bool ignore_oneway() const {return _ignore_oneway;}
    inline void set_ignore_oneway() {_ignore_oneway = true;}
    inline unsigned int allowed(const string h) {return _allowed[h];}
    inline void allowed(string h,const unsigned int e) {_allowed[h]=e;}
    inline unsigned int traffic_calming(const string t) {return _traffic_calming[t];}
    inline void traffic_calming(const string t,const unsigned int e) {_traffic_calming[t]=e;}
    inline unsigned int barrier(const string b) {return _barrier[b];}
    inline void barrier(const string b,const unsigned int e) {_barrier[b]=e;}
    inline void output(ostream &out) {out << "maxspeed = " << _maxspeed << "  avgspeed = " << _avgspeed << " ignore_oneway = " << _ignore_oneway;}

  private:
    unsigned int _maxspeed;
    unsigned int _avgspeed;
    bool _ignore_oneway;
    map<string,unsigned int> _allowed;
    map<string,unsigned int> _traffic_calming; 
    map<string,unsigned int> _barrier;
  };
}

#endif
