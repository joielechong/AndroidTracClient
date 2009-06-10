#! /bin/sh

pk -qAetc "select naam,id from rekening order by naam;" | 
(
  while [ "1" = "1" ] ; do 
    IFS="|"; 
    read naam id; 
    if [ -z "$id" ] ; then 
      break; 
    fi; 
    do_it "$naam"; 
  done; 
)
