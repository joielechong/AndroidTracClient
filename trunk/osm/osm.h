#ifndef _OSM_H
#define _OSM_H

#include <string>
#include <vector>

class Tag {
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
}

class Relation : Element {
}

class Node : Element {
}

#endif