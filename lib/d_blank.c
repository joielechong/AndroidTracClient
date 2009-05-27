/* 
**		file:		d_blank.c
**		purpose:	routine to fill a record in memory with blanks.
**		usage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					d_blank(d);
**					d_addrec(d);
**					d_close(d);
**					free(d);
**		notes:	the above code fragment append a blank record to the end of the
**					dbiii file.
**					compile with "tcc -c d_blank".  include this file in dbf.lib
**					see dbf.h for structure of DBF
**		author:	Mark Sadler
**		revised:	6/6/87
*/ 
#include <stdio.h>
#include <string.h>
#include "dbf.h"

int d_blank(struct DBF *d)
{
  memset(d->record_ptr,'\x20',d->record_length);
  return(0);
}

