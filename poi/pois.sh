#! /bin/sh

PATH=$PATH:/usr/local/bin:/usr/bin:/bin

cd ~mfvl/src/poi

./updatepois.sh
perl poidist.pl 2>/dev/null
#perl poiwrite.pl
