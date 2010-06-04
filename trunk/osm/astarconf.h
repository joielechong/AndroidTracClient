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
    inline double extracost() const {return _extracost;}
    inline void extracost(const double e) {_extracost=e;}

    inline void output(ostream &out) {out << "Speed = " << _speed << " extracost = " << _extracost;}

  private:
    double _extracost;
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
    double allowed(const string h);
    inline void allowed(string h,const double e) {_allowed[h]=e;}
    double traffic_calming(const string t);
    inline void traffic_calming(const string t,const double e) {_traffic_calming[t]=e;}
    double barrier(const string b);
    inline void barrier(const string b,const double e) {_barrier[b]=e;}
    void output(ostream &out);

  private:
    unsigned int _maxspeed;
    unsigned int _avgspeed;
    bool _ignore_oneway;
    map<string,unsigned double> _allowed;
    map<string,unsigned double> _traffic_calming; 
    map<string,unsigned double> _barrier;
  };
}

#endif
