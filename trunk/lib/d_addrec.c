/*
**		file:		d_addrec.c
**		purpose:	routine to append a record to a open dbaseiii file.
**		usage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					... put desired data into memory at location pointed to by
**							d->record_ptr ...
**					d_addrec(d);
**					d_close(d);
**					free(d);
**		notes:	compile with "tcc -c d_addrec".	include this file in dbf.lib
**					see dbf.h for structure of DBF
**		author:	Mark Sadler
**		revised:	6/6/87
*/

#include <stdio.h>
#include "dbf.h"

int d_addrec(struct DBF *d)
{
  fseek(d->file_ptr,((long)d->header_length + (d->records * d->record_length)),0);
  fwrite(d->record_ptr,d->record_length,1,d->file_ptr);
  d->current_record = ++d->records;
  d->status = updated;
  return(0);
}
