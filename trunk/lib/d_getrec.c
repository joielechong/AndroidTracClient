/*
**		file:		d_getrec.c
**		purpose:	routine to get a record from a dbiii file and place in memory at
**					location pointed to by DBF.record_prt.
**		ussage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					d_getrec(d,(long)record);
**					... access record as desired ...
**					d_close(d);
**					free(d);
**		notes:	compile with "tcc -c d_getrec".	include this file in dbf.lib
**					see dbf.h for structure of DBF
**		returns:	0					if successfull
**					RECNO_TOO_BIG	if record is not in database
**		author:	Mark Sadler
**		revised:	6/6/87
*/ 
#include <stdio.h>
#include "dbf.h"

int d_getrec(struct DBF *d,unsigned long int r)
{
  if(r > d->records)
    return(RECNO_TOO_BIG);
  if (r > 0L)
  {
    if (r != (d->current_record+1) || r == 1)
      fseek(d->file_ptr,((long)d->header_length + ((r - 1L) * d->record_length)),0);
    fread(d->record_ptr,d->record_length,1,d->file_ptr);
    d->current_record = r;
    return(0);
  }
  return(RECNO_TOO_BIG);
}

