#! /bin/sh

PATH=$PATH:/usr/local/bin:/usr/bin:/bin:/usr/local/pgsql/bin
cd /home/mfvl/src/poi

perl poiedit.pl 3
#ssh  mfvl@xs4all.nl "GET http://flitsservice.com/flitsservice.ov2" >Flitsservice.ov2

perl ov2toasc.pl

sleep 5

psql -d mfvl -c "truncate pois.temp"

find . -maxdepth 1 -mtime 0 -name "Flitsservice*.asc" -exec ./loadpoifile.sh '{}' \;

psql -d mfvl -f - <<EOF 
insert into pois.inputfiles (naam) select distinct filename from pois.temp where not filename in (select naam from pois.inputfiles);
update pois.temp set descr=trim(descr);
update pois.posten set updated='no' where file in (select distinct filename from pois.temp);

delete from pois.temp as t using pois.inputfiles as i where t.filename=i.naam and i.type is null;

update pois.posten as p set updated='yes' from pois.temp as t where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;

update pois.posten as p set lat=t.lat,lon=t.lon,file=t.filename,commentaar=t.descr,updated='yes' from pois.temp as t where t.naam=p.naam;
delete from pois.temp as t using pois.posten as p where p.naam=t.naam;

update pois.posten as p set changed='yes', file = t.filename, updated='yes' from pois.temp as t where t.lat=p.lat and p.lon=t.lon and p.commentaar = t.descr;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file != t.filename;

update pois.posten as p set changed='yes', commentaar = t.descr, naam=t.naam, updated='yes' from pois.temp as t where t.lat=p.lat and p.lon=t.lon and p.file = t.filename;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;

update pois.posten as p set changed='yes', lat=t.lat, lon=t.lon, file=t.filename, commentaar=t.descr, updated='yes' from pois.temp as t where t.naam=p.naam;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;

insert into pois.posten (lat,lon,file,commentaar,naam,updated,changed) select lat,lon,filename,descr,naam,'yes','yes' from pois.temp;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;
update pois.posten set updated=true where file='Flitsservice.asc' and (update_date > (now()- interval '1 day'));
EOF

psql -d mfvl -c "copy (select lon,lat,pois.mtcstring(type,id,snelheid,richting,bidirectioneel) from pois.flitspalen) to STDOUT with csv quote as ' ';" | todos >uitvoer/SCFormat.asc

