#include <assert.h>
#include <math.h>
#include "grootcirkel.h"

static void osmdistance(sqlite3_context *sc,int n,sqlite3_value **values) {
  double result,lat1,lon1,lat2,lon2;
  
  assert(n==4);
  lat1 = sqlite3_value_double(values[0]);
  lon1 = sqlite3_value_double(values[1]);
  lat2 = sqlite3_value_double(values[2]);
  lon2 = sqlite3_value_double(values[3]);
  //    result = (RADIUS-DRAD*(sin((lat1+lat2)*PI/360)))*2*asin(sqrt((pow(sin((lat2-lat1)*PI/360),2)+cos(lat1*PI/180)*cos(lat2*PI/180)*pow(sin((lon2-lon1)*PI/360),2))));
  result = grootcirkel(lat1,lon1,lat2,lon2);
  sqlite3_result_double(sc, result);
}

static void osmint(sqlite3_context *sc,int n,sqlite3_value **values) {
  long num;
  
  assert(n==1);
  num = atol( (const char *)sqlite3_value_text(values[0]));
  sqlite3_result_int(sc,num);
}

static void osmcalc_x(sqlite3_context *sc,int n,sqlite3_value **values) {
  double lon;
  int result;
 
  assert(n==1);
  lon = sqlite3_value_double(values[0]);
  result = round((lon+90.0)*20.0);
  sqlite3_result_int(sc,result);
}

static void osmcalc_y(sqlite3_context *sc,int n,sqlite3_value **values) {
  double lat;
  int result;

  assert(n==1);
  lat = sqlite3_value_double(values[0]);
  result = round((lat+180.0)*20.0);
  sqlite3_result_int(sc,result);
}
