BEGIN {FS="|";}

{ split($2,d,"-");
  dagnr=(31*(d[2]-1))+d[1]-1;
  jaar=d[3]+dagnr/372;
  printf "%s %s %.3f %7.2f %7.2f %7.2f %7.2f\n",$2,datum,jaar,$4,$5,$6,$7;
}
