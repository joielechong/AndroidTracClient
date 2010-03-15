#include "osm.h"

using namespace osm;

Element::Element(long id,int version) :_id(id),_version(version) {}

void Element::addTag(string k,string v) {
  _tags[k] = v;
}

Way::Way(long id,int version) : Element(id,version) {}
Relation::Relation(long id,int version) : Element(id,version) {}
Node::Node(long id,int version,double lat,double lon) : Element(id,version), _lat(lat), _lon(lon) {}

void Way::addNd(long ref) {
    Nd nd(ref);
    _nds.pushback(nd);
}

void Relation::addMember(long ref,string type,string role) {
	Member mem(ref,type,string);
	-member.pushback(mem);
}