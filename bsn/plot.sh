#! /bin/sh

if [ -z "$1" ] ; then
  echo "Geef een fondsnaam op"
  exit 1
fi

PLOTFILE=/tmp/plotpg_$$
LOADFILE=/tmp/loadpg_$$

echo "Loading data from database"

naam=$1;

alias=`psql koersdata -qAtc "select naam2 from vertaal where naam1='$naam';"`

if [ -n "$alias" ] ; then
  naam=$alias;
fi

psql koersdata -qAtc "select * from koers where naam='$naam' order by datum;"|awk -f y.awk >$PLOTFILE

dest=$2

if [ "$dest" = "PS" ] ; then

cat >$LOADFILE <<EOF1
set term postscript landscape color
set output "|lpr"
EOF1

elif [ "$dest" = "JPEG" ] ; then

f=`echo $naam| sed -e "s/ /_/g;"`

cat >$LOADFILE <<EOF2
set term postscript landscape color
set output "|gs -sDEVICE=jpeg -q -dBATCH -sOutputFile=$f.jpg -"
#set output "x.ps"
EOF2

else

dest="";
cat >$LOADFILE <<EOF

EOF

fi

cat >>$LOADFILE <<EOF2
#set multiplot
set title '$naam'
set grid 

# set logscale y

#set origin 0,0
#set size 1,0.5

#plot [1997:1998] '$PLOTFILE' using 1:5 with lines, '' using 1:3 with lines  , '' using 1:4 with lines, '' using 1:2 with lines
plot '$PLOTFILE' using 1:5 with lines, '' using 1:3 with lines  , '' using 1:4 with lines, '' using 1:2 with lines

#set origin 0,0.5
#set size 1,0.5

#plot '$PLOTFILE' using 1:5:4:3 with yerrorbars
EOF2

if [ -z "$dest" ] ; then

cat >>$LOADFILE <<EOF3
#set nomultiplot
pause -1 "Hit Return to continue
EOF3

fi

echo "Calling gnuplot"
gnuplot $LOADFILE

rm $LOADFILE
rm $PLOTFILE