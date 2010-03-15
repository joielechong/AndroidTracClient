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
		
		void addTag(string k,string v);

	protected:
		long _id;
		int	_version;
		vector<Tag> _tags;
}

class Way : Element {
}

class Relation : Elelement {
}

class Node : Element {
}

#endif