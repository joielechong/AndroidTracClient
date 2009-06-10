#! /bin/sh

psql koersdata -qAtc "select id,naam from rekening order by naam;" | (
  while [ "1" = "1" ] ; do
    IFS="|"
    read id naam
    if [ -z "$id" ] ; then
      break;
    fi
    if [ -z "$naam" ] ; then
      continue;
    fi
    plot.sh "$naam"
  done
)
