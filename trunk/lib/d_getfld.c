/* 
**		file:		d_getfld.c
**		purpose:	routine to fill a buffer with field data from a dbiii file
**					opened with d_open and accessed with d_getrec.	the data from the
**					record is copied into the buffer and terminated with '\0'
**		usage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					d_getrec(d,(long)recordno);
**					d_getfld(d,fieldno,buffer);
**					... use field data ...
**					d_close(d);
**					free(d);
**		notes:	compile with "tcc -c d_getfld".	include this file in dbf.lib
**					see dbf.h for structure of DBF
**		returns:	field type  if successful
**					null if not
**		author:	Mark Sadler
**		revised:	6/14/87
*/ 

#include <stdio.h>
#include <string.h>
#include "dbf.h"

char d_getfld(struct DBF *d,int f,char *buff)
{
  struct FIELD_RECORD *fp;
  if(f > 0 && f <= d->num_fields)
  {
    fp = d->fields_ptr + (f - 1);
    memcpy(buff,fp->field_data_address,fp->len);
    buff[fp->len] = '\0';
    return(fp->typ);
  }
  else
  {
    buff[0]='\0';
    return('\0');
  }
}

