/*
**		file:		d_close.c
**		purpose:	routine to close a dbaseiii file after access by the other routines
**					in dbf.lib.	updates header and places eof marker at end of file.
**		usage:	d = (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(d->filename,"filename.dbf");
**					d_open(d);
**					... access file with other routines ...
**					d_close(d);
**					free(d);
**		notes:	compile with "tcc -c d_close".	include this file in dbf.lib
**					see dbf.h for structure of DBF.	ALWAYS close a file that has
**					been opened, otherwise records may be lost.
**		author:	Mark Sadler
**		revised:	6/18/87
*/ 		
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "dbf.h"

void d_write_header(struct DBF *d)
{
/* position at start of file */
  rewind(d->file_ptr);

  putc(d->dbf_version,d->file_ptr);
  putc(d->update_yr,d->file_ptr);
  putc(d->update_mo,d->file_ptr);
  putc(d->update_day,d->file_ptr);
  putlong(d->records,d->file_ptr);
  putword(d->header_length,d->file_ptr);
  putword(d->record_length,d->file_ptr);
}

void d_close(struct DBF *d)
{
  struct tm *tt;
  time_t t;
  char c;

  if(d->status == updated)
  {
    /* update date data */
    time(&t);
    tt = localtime(&t);
    d->update_day = tt->tm_mday;
    d->update_mo  = tt->tm_mon+1;
    d->update_yr  = tt->tm_year;

    d_write_header(d);

    /* position at end of file	 */
    fseek(d->file_ptr,1L,SEEK_END);
    fread(&c,1,1,d->file_ptr);
    /* write eof */
    if (c != '\x1a')
      fwrite("\x1a",1,1,d->file_ptr);
  }
  /* free fields array and record */
  free(d->fields_ptr);
  free(d->record_ptr);
  fclose(d->file_ptr);
}
