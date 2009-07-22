#! /bin/sh

PATH=$PATH:/usr/local/bin:/usr/bin:/bin:/usr/local/pgsql/bin
cd /home/mfvl/src/poi


psql -d mfvl -c "copy (select lon,lat,pois.mtcstring(type,id,snelheid,richting,bidirectioneel) from pois.flitspalen where not (land is null and lat>=45.9332 and lat <=54.786 and lon >=6.03111 and lon <= 17.082377) and not land in ('D','A','CH') ) to STDOUT with csv quote as ' ';" | todos >uitvoer/SCFormat-ADCH.asc
