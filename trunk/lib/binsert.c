#include <string.h>
#include <mfl.h>

void *binsert(void *key,void *base,size_t *nmemb,const size_t size,int (*compar)())
{
	size_t low,high,mid,n=*nmemb;
	int m;

	if (n == 0)
	{
		memmove(base,key,size);
		(*nmemb)++;
		return base;
	}
	low = 0;
	high = n-1;
	while (low != high)
	{
		mid = (low+high)/2;
		m = ((int (*)(void *,void *))compar)(key,((char *)base)+mid*size);
		if (m == 0)
			return ((char *)base)+mid*size;
		else if (m < 0)
			high = mid;
		else
			low = mid+1;
	}
	m = ((int (*)(void *,void *))compar)(key,((char *)base)+low*size);
	if (m == 0)
		return ((char *)base)+low*size;
	else if (m < 0)
	{
		memmove(((char *)base)+(low+1)*size,((char *)base)+low*size,size*(n-low));
		memmove(((char *)base)+low*size,key,size);
		(*nmemb)++;
		return ((char *)base)+low*size;
	}
	else
	{
		low++;
		memmove(((char *)base)+(low+1)*size,((char *)base)+low*size,size*(n-low));
		memmove(((char *)base)+low*size,key,size);
		(*nmemb)++;
		return ((char *)base)+low*size;
	}
}
