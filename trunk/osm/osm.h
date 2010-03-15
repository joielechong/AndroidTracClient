#ifndef _OSM_H
#define _OSM_H

#include <string>
#include <vector>

using namespace osm;

class Tag {
}

class Nd {
}

class Member {
}

class Element {
	public:
		Element(long id,int version);
		~Element();
		
		void addTag(std::string k,std::string v);

	protected:
		long _id;
		int	_version;
		std::vector<Tag> _tags;
}

class Way : Element {
	public:
		Way(long id,int version);
		~Way();
	
	private:
		std::vector<Nd> _nds;
}

class Relation : Element {
	public:
		Relation(long id,int version);
		~Relation();
	
	private:
		std::vector<Member> _members;
}

class Node : Element {
	public:
		Node(long id,int version, double lat,double lon);
		~Node();
		
	private:
		double _lat;
		double _lon
		int x;
		int y;
}

#endif