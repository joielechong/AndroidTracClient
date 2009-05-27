#ifndef _DBFILE_H
#define _DBFILE_H 1.0

#include <stdio.h>
#include <string.h>
#include <dbf.h>
#include <typeinfo>
#include <iostream.h>
#include <stdexcept>

template <class T> class DBFile
{
public:
  DBFile(const char *naam);
  ~DBFile() { d_close(&d); }

  T& operator[](const long int r);
  int find_field(const char *field);
  long getLong(const char *field) const;
  int  getInt(const char *field) const;
  double getDouble(const char *field) const;
  unsigned long records(void) const { return d.records;}

private:
  struct DBF d;
  T t;
};


template <class T>
DBFile<T>::DBFile(const char *naam) {
  strcpy(d.filename,naam);
  if (d_open(&d) != 0) {
    static char buffer[512];
    sprintf(buffer,"Cannot open %s",naam);
//    throw(invalid_argument(buffer));
    throw (buffer);
  }
}

template <class T>
T& DBFile<T>::operator[](const long int r){
  static char buffer[512];

  if (d_getrec(&d,r) != 0) {
    sprintf(buffer,"%s: Illegal record requested %ld",d.filename,r);
    throw(buffer);
  }
  for(int i=0;i<d.num_fields;i++) {
    d_getfld(&d,i+1,buffer);
    if (*buffer == 0) {
      sprintf(buffer,"%s:Illegal field requested %d",d.filename,i+1);
      throw(buffer);
    }
    fprintf(stderr,"%s\n",d.fields_ptr[i].name);
    t.field_fill(d.fields_ptr[i].name,buffer);
  }
  return t;
}

#endif
