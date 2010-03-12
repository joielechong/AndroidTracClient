#! /bin/sh

echo "Content-Type: text/plain"
echo ""


PATH=$PATH:/usr/local/bin:/usr/bin:/bin:/usr/local/pgsql/bin
cd /home/mfvl/src/poi

perl poiedit.pl
unzip  -d pois-Blitzer -qo pois-Blitzer
unzip -d . -qo radarsfixes.zip

perl ov2toasc.pl

sleep 5

psql -d mfvl -c "truncate pois.temp"

find . -name "*.asc" -mtime 0 -maxdepth 1 -exec ./loadpoifile.sh '{}' \;
#find . -name "Flitsservice.asc" -mtime 0 -maxdepth 1 -exec ./loadpoifile.sh '{}' \;
cd pois-Blitzer
find . -name "*.asc" -maxdepth 2 -exec ../loadpoifile.sh '{}' \;
cd ..

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

insert into pois.posten (lat,lon,file,commentaar,naam,updated,changed) select distinct lat,lon,filename,descr,naam,true,true from pois.temp;
delete from pois.temp as t using pois.posten as p where p.lat=t.lat and p.lon = t.lon and p.commentaar = t.descr and p.file=t.filename;
update pois.posten set updated=true where file='Flitsservice.asc' and (update_date > (now()- interval '1 day'));
EOF
