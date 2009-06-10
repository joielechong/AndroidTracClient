#! /bin/sh

PATH=$PATH:/usr/local/pgsql/bin

if [ -z "$1" ] ; then
  echo "Wel een jaartal opgeven (4 cijfers)"
  exit 1
fi

(
  psql koersdata -qetAc "select * from koers where datum>='1-jan-$1' and datum<='31-mar-$1' order by datum,naam;" | awk -f pg.bsn.awk;
  psql koersdata -qetAc "select * from koers where datum>='1-apr-$1' and datum<='30-jun-$1' order by datum,naam;" | awk -f pg.bsn.awk;
  psql koersdata -qetAc "select * from koers where datum>='1-jul-$1' and datum<='30-sep-$1' order by datum,naam;" | awk -f pg.bsn.awk;
  psql koersdata -qetAc "select * from koers where datum>='1-oct-$1' and datum<='31-dec-$1' order by datum,naam;" | awk -f pg.bsn.awk
) >/temp/bsn-files/$1.bsn


