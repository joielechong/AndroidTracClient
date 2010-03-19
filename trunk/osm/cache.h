// 
//    This  template class implements a cache for memory objects
//	  It is a kind of stl map with its size controlled.
//
//
//    While the cache is not full, all elements are memorised, when the cache is full older elements are released
//
//
//    Typical use :
//
//		The example below shows a cache of strings indexed by an int
//
//		cache<int,string> mycache;
//      string *pText = mycache[23];  // find element 23
//		if(!mycache.iscached(pText))
//		{
//			FillTextFromDisk(23,pText); // fills with a pText= new string...
//			mycache.push_back(23,pText);
//		}
//      ... here *pText is available
//
//	  Description :		
//
//	  cache<K, V>;
//			Constructor of the cache K is the key, and V is the object stored. See too "Size of the cache".
//	  K is the key, maybe string, int or any objects that fits in a stl map
//	  V is the class that you whant to cache. 
//			V must support a size() method and a correct ~V destructor, which are used by cache.
//	  push_back(K k,V* p);
//			Insert p element in the cache at position k. p must point to a V element or be NULL
//          This element have been created with some new V and will be deleted by cache soon or later;
//			This element may be NULL, on the condition that NULL->size() gives back 0 and delete NULL works.
//
// 	  V* operator[](K& k);
//          This allows to access to an object indexed by k, and gives back a pointer on this object
//			If the object is not cached  (*V)(-1) is returned.
//	  iscached(V* p);
//			Simply gives back true when p is not -1
//
//
//
//	  Size of the cache
//
//	  You can control the number of elements of the queue and the total size of the queue
//	  A more complete declaration of the cache is
//	  cache<K, V, limsize>;  // example  	cache<int,string,10000> mycache;
//			when the cache grows till limsize, older elments are removed
//			limsize represents the maximum size of the cache expressed in the unit of V::size()
//				(example characters for string::size())
//	  A more complete declaration of the cache is:
//	  cache<K, V, limsize, BigQ, SmallQ>;
//          It allows to control the size of the queue. The size of the cache had been defined above.
//			The size of the queue is the maximum number of V elements alwoed in the queue.
//			When the size of the cache is greater than limsize, SmallQ is used else BigQ s used.
//
//	  Hints :
//          - Do not forget that cache WILL delete V elements
//			- It is a choice to allow or not NULL pointer, but respect the conditions on V.
//					V::size() { if(!this) return 0; else...

#ifndef __CACHE_FOR_MEMORY_OBJECTS_PCG907
#define __CACHE_FOR_MEMORY_OBJECTS_PCG907

#include <map>
#include <list>

class listitem;
typedef list<listitem>::iterator listiter;
typedef map< K,listiter>::iterator mapiter;

template < class K, class V, int limsize = 1000, int BiqQ = 0x7fffffff, int SmallQ=0 > class cache {
 public:
 cache() { m_size =0;}
 V* operator[](K& k) {
   mapiter imap = m_map.find(k);
   if(imap== m_map.end()) return ((V*) -1);
   listiter ilist = (*imap).second;
   V* p= (*ilist).m_p;
   m_list.erase(ilist);
   push_back(p, imap);
   return p;
 }
 
 void push_back(const K &k,V* p)  {
   int size = p->size();
   bool Remove;
   listiter ilist =NULL;
   do
     {
       Remove = m_size>limsize;
       
       if(Remove)
	 Remove = m_list.size()> SmallQ;
       else
	 Remove = m_list.size()> BiqQ;
       if(Remove) {
	 ilist = m_list.end();
	 ilist--;
	 m_size -= ((*ilist).m_p)->size();
	 delete (*ilist).m_p;
	 m_map.erase((*ilist).m_mapiter);
	    m_list.erase(ilist);
       }
     } while(Remove);
   mapiter imap;
   ASSERT(m_map.find(k)==m_map.end()); // No, you are not allowed to insert twice the same elment
   // begin bad writing : The 2 next lines are bad writing, but I do not know  how to avoid it.. please help
   ASSERT(m_map.size() == m_list.size());
   TRACE(" %d \n",m_map.size());
   m_map[k]=ilist;
   TRACE(" %d \n",m_map.size());
   ASSERT(m_map.size() == m_list.size()+1);
   imap = m_map.find(k); 
   //  They should be replaced by some :
   //      imap = m_map.insert(k,ilist); 
   // end bad writing : please help me to fix that 
   ASSERT(m_map[k]==ilist);
   push_back(p, imap);
   m_size += p->size();
 }

 bool iscached(V* p) {
   return (p != (V*) -1);
 }
 
 ~cache() {
   for(listiter ilist = m_list.begin(); ilist != m_list.end(); ilist++)
     delete (*ilist).m_p;  
 }

#ifdef _DEBUG
 int dbggetmapsize() {
   ASSERT(m_map.size() == m_list.size());
   TRACE(" dbggetmapsize %d (m_size %d) order : ",m_map.size(),m_size);
   for(listiter ilist = m_list.begin(); ilist != m_list.end(); ilist++)
     TRACE(" %d ", (*((*ilist).m_mapiter)).first);
   TRACE("\n");
   return m_map.size();
 }
 
 int dbggetcachesize() {
   return m_size;
 }
  
 V* dbgcheck(K &k) {
   mapiter imap = m_map.find(k);
   if(imap== m_map.end()) return ((V*) -1);
   listiter ilist = (*imap).second;
   return  (*ilist).m_p;
  }
#endif //_DEBUG

 private:
 class listitem
 {
   friend class cache<  K,  V,  limsize,  BiqQ,  SmallQ > ;
 private:
   mapiter  m_mapiter;
   V* m_p;
 };
 list<listitem> m_list;
 map< K,listiter> m_map;
 int m_size;
 void push_back(V* p, mapiter &imap)
 {
   ASSERT(m_map.size() == m_list.size()+1);
   m_list.push_front();
   ASSERT(m_map.size() == m_list.size());
   listiter ilist = m_list.begin();
   (*ilist).m_mapiter = imap;
   (*ilist).m_p = p;
   (*imap).second = ilist;
   ASSERT(m_map.size() == m_list.size());
 }
};


#ifdef _DEBUG
#ifdef TEST_CACHE

#include <string>

void main () //test_cache()
{
  
  cache<int, string ,25, 3, 2> tcache;
  
  int		one =12341;
  int		two =12342;
  int		three =12343;
  int		four =12344;
  int		five =12345;
  int		six =12446;
  int		seven =12447;
  int		eight =12448;
  tcache.push_back(one,new string(11,'1'));
  ASSERT(*(tcache.dbgcheck(one))== string(11,'1'));
  ASSERT(tcache.dbggetmapsize()==1 && tcache.dbggetcachesize()==11);
  tcache.push_back(two,new string(4,'2'));
  ASSERT(tcache.dbggetmapsize()==2 && tcache.dbggetcachesize()==15);
  ASSERT(*(tcache.dbgcheck(one))== string(11,'1'));
  tcache.push_back(three,new string(3,'3'));
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==18);
  tcache.push_back(four,new string(2,'4'));
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==20);
  ASSERT(*(tcache.dbgcheck(one))== string(11,'1'));
  ASSERT(*(tcache.dbgcheck(four))== string(2,'4'));
  tcache.push_back(five,new string(8,'5'));
  ASSERT(tcache.dbgcheck(one) == (string*) -1);
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==17);
  tcache.push_back(six,new string(3,'6'));
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==16);
  ASSERT(tcache.dbgcheck(two) == (string*) -1);
  // order 6,5,4,3
  ASSERT(tcache[two] == (string*) -1);
  ASSERT(*(tcache[four])== string(2,'4'));
  // order 4,6,5,3
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==16);
  ASSERT(tcache[one] == (string*) -1);
  ASSERT(*(tcache[three])== string(3,'3'));
  // order 3,4,6,5
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==16);
  tcache.push_back(seven,new string(12,'7'));
  // order 7,3,4,6
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==20);
  tcache.push_back(one,new string(11,'1'));
  // order 1,7,3,4
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==28);
  tcache.push_back(eight,new string(17,'8'));
  ASSERT(tcache[four] == (string*) -1);
  // order 8,1,7
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==40);
  ASSERT(*tcache[seven] == string(12,'7'));
  // order 7,8,1
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==40);
  ASSERT(*tcache[one] == string(11,'1'));
  // order 1,7,8
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==40);
  tcache.push_back(six,new string(3,'6'));
  // order 6,1,7
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==26);
  tcache.push_back(two,new string(4,'2'));
  // order 2,6,1
  ASSERT(tcache.dbggetmapsize()==3 && tcache.dbggetcachesize()==18);
  // order 8,2,6,1
  tcache.push_back(eight,new string(17,'8'));
  ASSERT(tcache.dbggetmapsize()==4 && tcache.dbggetcachesize()==35);
  
}

#endif //TEST_CACHE

#endif //_DEBUG

#endif  //__CACHE_FOR_MEMORY_OBJECTS_PCG907
