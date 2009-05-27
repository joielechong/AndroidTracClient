/* 
**		file:		d_cpystr.c
**		purpose:	routine to create a new dbiii file useing the structure of an
**					existing file.
**		ussage:	source = (struct DBF *)malloc(sizeof(struct DBF));
**					dest	= (struct DBF *)malloc(sizeof(struct DBF));
**					strcpy(source->filename,"source.dbf");
**					strcpy(dest->filename,"dest.dbf");
**					d_open(source);
**					d_cpystr(source,dest);
**					d_close(source);
**					d_close(dest);
**					free(source);
**					free(dest);
**		notes:	compile with "tcc -c d_cpystr".	include this file in dbf.lib
**					this code fragment does the same thing as:
**					USE SOURCE
**					COPY STRUCTURE TO DEST
**		returns:	0				if successful with structure filled in
**					NO_FILE		if unable to find file
**					OUT_OF_MEM	if not enough memory
**		author:	Mark Sadler
**		revised:	6/19/87
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "dbf.h"

int d_cpystr(struct DBF *s,struct DBF *d)
{
  struct FIELD_RECORD *f;
  int i;
  int n;

  /* make sure source file is open */
  if(s->status != not_updated && s->status != updated)
    return(NO_FILE);

  d->status = not_open;						/* in case can not open */

  if((d->file_ptr = fopen(d->filename,"w+b")) == NULL)
    return(NO_FILE);

  if((d->fields_ptr = (struct FIELD_RECORD *)malloc(s->num_fields * FIELD_REC_LEN))==NULL)
    return(OUT_OF_MEM);

  memcpy(d->fields_ptr,s->fields_ptr,(s->num_fields * FIELD_REC_LEN));
  memcpy(&d->dbf_version,&s->dbf_version,12);
  d->records = 0L;
  d->current_record = 0;
  d->num_fields = s->num_fields;
  /* the following line forces the copied dbf structure to be in dbIII+ format */
  /* dbIII+ files can be read without problems with dbIII. */
  d->header_length=HEADER_PROLOG+(d->num_fields * FIELD_REC_LEN)+1;
  if((d->record_ptr = (char *)malloc(d->record_length))==NULL)
    return(OUT_OF_MEM);
  /* initialize pointers to fields in record */
  for(i=0,n=1;i<d->num_fields;i++)
  {
    f = d->fields_ptr + i;
    f->field_data_address = d->record_ptr + n;
    n += f->len;
  }

  putc(d->dbf_version,d->file_ptr);
  putc(d->update_yr,d->file_ptr);
  putc(d->update_mo,d->file_ptr);
  putc(d->update_day,d->file_ptr);
  putlong(d->records,d->file_ptr);
  putword(d->header_length,d->file_ptr);
  putword(d->record_length,d->file_ptr);

  for(i=1;i<=20;i++)
    fwrite("\0",1,1,d->file_ptr);

  fwrite(d->fields_ptr,FIELD_REC_LEN,d->num_fields,d->file_ptr);

  fwrite("\x0d",1,1,d->file_ptr);

  d->status = updated;						/* open successfull */
  return(0);
}
