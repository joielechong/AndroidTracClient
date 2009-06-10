BEGIN {FS="|";
       maand["01"]="JAN";
       maand["02"]="FEB";
       maand["03"]="MAR";
       maand["04"]="APR";
       maand["05"]="MAY";
       maand["06"]="JUN";
       maand["07"]="JUL";
       maand["08"]="AUG";
       maand["09"]="SEP";
       maand["10"]="OCT";
       maand["11"]="NOV";
       maand["12"]="DEC";
       olddat="";
     }

{ 
  if (olddat!=$2) { 
    split($2,d,"-");
    printf "0/1/%s-%s-%2.2d\r\n",d[3],maand[d[2]],d[1]%100;
    olddat=$2;
  }
  printf "9|30|%s|%d|%.2f|%.2f|%.2f|%.2f\r\n",$1,$3,$4,$5,$6,$7; 
}
