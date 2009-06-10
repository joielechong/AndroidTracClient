#! /bin/sh


psql koersdata -qAtc "select naam,actietab.actie,sum(aantal),sum(aantal*koers),sum(kosten) from aandelen,rekening,actietab  where aandelen.id=rekening.id and actietab.id=aandelen.actie  group by naam,actietab.actie order by naam,actietab.actie;" | sed -e "s/|/|+/g;"| sed -e "s/|+/|/;" -e "s/+-/-/g;" | awk -f x.awk
