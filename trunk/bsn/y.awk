BEGIN {FS="|";}

{ split($2,d,"-");
  maand=d[2];
  if (d[2]=="08") maand=8;
  if (d[2]=="09") maand=9;
  dag=d[3];
  if (d[1]=="08") dag=8;
  if (d[1]=="09") dag=9;

  dagnr=31*(maand-1)+dag-1;
  jaar=d[1]+dagnr/372;
  printf "%.3f %7.2f %7.2f %7.2f %7.2f\n",jaar,$4,$5,$6,$7;
}
