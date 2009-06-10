BEGIN {FS="|";fonds="";somaantal=0;sombedr=0;somkost=0;}

{
  if (fonds != $1 ) {
    if (fonds != "") {
      printf "      %-20s ========== ========== ========\n"," ";
      printf "      %-20s %10.4f %10.2f %8.2f\n\n"," ",somaantal,sombedr,somkost;
    }
    printf "%s\n",$1;
    fonds=$1;
    somaantal=0;sombedr=0;somkost=0;
  }
  printf "      %-20s %10.4f %10.2f %8.2f\n",$2,$3,$4,$5;
  somaantal += $3;
  sombedr += $4;
  somkost += $5;
}

END {
  if (fonds != "") {
    printf "      %-20s ========== ========== ========\n"," ";
    printf "      %-20s %10.4f %10.2f %8.2f\n"," ",somaantal,sombedr,somkost;
  }
}
