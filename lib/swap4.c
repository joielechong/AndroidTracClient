
#ifdef OS2
void swap4(unsigned char b[4])
{
  unsigned char c;

  c = b[0];
  b[0] = b[3];
  b[3] = c;
  c = b[2];
  b[2] = b[1];
  b[1] = c;
}
#endif


