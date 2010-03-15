#include "osm.h"

using namespace osm;

Element::Element(long id,int version) :_id(id),_version(version) {}

void Element::addTag(string k,string v) {
  _tags[k] = v;
}

