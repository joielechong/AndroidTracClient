void scale(int *num,int *ndiv)
{
  int factor;
  int n;
  int nd;

  if(*num <= 0)
  {
    *num = 1;
    return;
  }

  n = *num;
  factor = 1;
  while(n >= 10)
  {
    n /= 10;
    factor *= 10;
  }

  if(n<2) {
    n  = 2;
    nd = 10;
  }
  else if(n<3) {
    n  = 3;
    nd = 6;
  }
  else if(n<5) {
    n  = 5;
    nd = 10;
  }
  else {
    n  = 10;
    nd = 10;
  }

  *num = n * factor;
  if (nd > *num)
    nd = *num;
  *ndiv = nd;
}

