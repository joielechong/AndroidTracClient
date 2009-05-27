/*
**		file:		d_putfld.c
**		purpose:	routine to fill a field with data from buffer.
**		ussage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					d_getrec(d,(long)recordno);
**					d_getfld(d,fieldno,buffer);
**					... modify field data ...
**					d_putfld(d,fieldno,buffer);
**					d_putrec(d,(long)recordno);
**					d_close(d);
**					free(d);
**		notes:	compile with "tcc -c d_putfld".	include this file in dbf.lib
**					see dbf.h for structure of DBF.	this routine places the data into
**					a record in memory.	d_addrec or d_putrec must be called to write
**					the data to the file on disk.
**		returns:	length of the field if successful. 0 if not.
**		author:	Mark Sadler
**		revised:	6/14/87
*/

#include <stdio.h>
#include <string.h>
#include "dbf.h"

int d_putfld(struct DBF *d,int f,char *buff)
{
  struct FIELD_RECORD *fp;

  if(f > 0 && f <= d->num_fields)
  {
    fp = d->fields_ptr + (f - 1);
    memcpy(fp->field_data_address,buff,fp->len);
    return(fp->len);
  }
  else
    return(0);
}

