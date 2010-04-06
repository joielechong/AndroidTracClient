#include "osm_db.h"

namespace osm_db {  
  void database::postprocess() {
    executenonquery("UPDATE tag SET v='associatedStreet' WHERE type='relation' AND k='type' AND v='relatedStreet'");
    executenonquery("DELETE FROM relation WHERE id in (SELECT id FROM relationtag WHERE k='type' AND NOT v in ('boundary','restriction','multipolygon','associatedStreet'))");
    executenonquery("DELETE FROM way WHERE NOT id in (SELECT id FROM waytag WHERE k in ('highway','boundary','route','natural') OR k like 'addr:%' OR k like 'is_in%' UNION SELECT ref FROM member WHERE type = 'way')");
    executenonquery("DELETE FROM way WHERE id in (SELECT id FROM waytag as tag WHERE ((k='route' AND NOT v like 'ferry%') OR ( k='natural' AND NOT v like 'coastline%')))");
    executenonquery("DELETE FROM node WHERE NOT id IN (SELECT id FROM nodetag UNION SELECT ref FROM nd UNION SELECT ref FROM member WHERE type='node')");
    executenonquery("UPDATE tag SET v='yes' WHERE k IN ('bridge','oneway','tunnel') AND v IN ('1','YES','true','Yes')");
    executenonquery("DELETE FROM tag WHERE k IN ('bridge','oneway','tunnel') AND v IN ('NO','FALSE','No','False','no','ny','false')");
	
    executenonquery("UPDATE node SET x=round((lon+90)*20),y=round((lat+180)*20) WHERE id in (SELECT ref FROM usable_way as u,nd WHERE u.id=nd.id)");
    executenonquery("INSERT OR REPLACE INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp");
    //    executenonquery("INSERT OR REPLACE INTO neighbor (way,id1,id2,distance) SELECT DISTINCT way,id1,id2,osmdistance(nd1.lat,nd1.lon,nd2.lat,nd2.lon) FROM nb,node as nd1,node as nd2 WHERE id1=nd1.id AND id2=nd2.id");
    executenonquery("INSERT INTO adressen SELECT id,'node' AS type,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:country') AS country,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:city') AS city,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:street') AS street,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM nodetag WHERE id=node.id AND k='addr:postcode') AS postcode FROM node WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL");
    executenonquery("INSERT INTO adressen SELECT id,'way' AS type,(SELECT v FROM waytag WHERE id=way.id AND k='addr:country') AS country,(SELECT v FROM waytag WHERE id=way.id AND k='addr:city') AS city,(SELECT v FROM waytag WHERE id=way.id AND k='addr:street') AS street,(SELECT v FROM waytag WHERE id=way.id AND k='addr:housenumber') AS housenumber,(SELECT v FROM waytag WHERE id=way.id AND k='addr:postcode') AS postcode FROM way WHERE NOT coalesce(country,city,street,housenumber,postcode) IS NULL");
  }  
}
