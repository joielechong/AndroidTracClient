/* 
**		file:		d_putrec.c
**		purpose:	routine to update a record in a dbiii file.
**		ussage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					d_getrec(d,(long)recordno);
**					... modify record ...
**					d_putrec(d,(long)recordno);
**					d_close(d);
**					free(d);
**		notes:	the data for the record is contained in memory at the location
**					pointed to by DBF.record_ptr.
**					compile with "tcc -c d_putrec".	include this file in dbf.li
**					see dbf.h for structure of DBF
**		returns:	0					if successfull
**					RECNO_TOO_BIG	if record is not in database
**		author:	Mark Sadler
**		revised:	6/6/87
*/ 

#include <stdio.h>
#include "dbf.h"

int d_putrec(struct DBF *d,unsigned long int r)
{
  if(r > d->records)
    return(RECNO_TOO_BIG);
  if(r > 0L)
  {
    fseek(d->file_ptr,((long)d->header_length + ((r - 1) * d->record_length)),SEEK_SET);
    fwrite(d->record_ptr,d->record_length,1,d->file_ptr);
    d->status = updated;
    d->current_record = 0;
    fseek(d->file_ptr,0,SEEK_SET);
  }
  return(0);
}
