#! /bin/sh 

PATH=$PATH:/usr/local/bin:/usr/bin:/bin:/usr/local/pgsql/bin

file=`echo $1| sed -e "s/^..//;"`

echo $file

fromdos < "$file" | 
     sed '/^$/d' | 
     iconv -f iso-8859-1  -t utf-8 | 
     grep -v "^;" | 
     sed -e "s:$:,\"$file\":g;" -e "s/^ *//; -e "s/ ,','g;" -e "s/, */, /g;" | 
psql -d mfvl -c "copy pois.temp (lon,lat,descr,filename) from stdin with csv"
