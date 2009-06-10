BEGIN {FS=" ";
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
     }

/^[12]/ { d[3] = substr($1,1,4);
	  d[1] = substr($1,5,2);
	  d[2] = substr($1,7,2);
	  printf "0/1/%d-%s-%2.2d\r\n",d[2],maand[d[1]],d[3]%100;
	  printf "9|30|%s|0|%.2f|%.2f|%.2f|%.2f\r\n",$3,$2,$2,$2,$2; 
	}
