#include <math.h>
#include <stdlib.h>

#define PI (3.1415926535897932384626433)
#define RADIUS (6378137)
#define DRAD (21835)

inline double grootcirkel(const double lat1, const double lon1, const double lat2, const double lon2) {
    return (RADIUS-DRAD*(sin((lat1+lat2)*PI/360)))*2*asin(sqrt((pow(sin((lat2-lat1)*PI/360),2)+cos(lat1*PI/180)*cos(lat2*PI/180)*pow(sin((lon2-lon1)*PI/360),2))));
}

