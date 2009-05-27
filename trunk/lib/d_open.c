/*
**		file:			d_open.c
**		purpose:		routine to open a dbaseiii file for access by the other routines
**						in dbf.lib.
**		usage:		d = (struct DBF *)malloc(sizeof(struct DBF));
**						strcpy(dbf->filename,"filename.dbf");
**						d_open(d);
**						... access file with other routines ...
**						d_close(d);
**						free(d);
**		notes:		compile with "tcc -c d_open".  include this file in dbf.lib
**						see dbf.h for structure of DBF.  copy filename into structure
**						before calling d_open.
**		returns:		0			 if successful with structure filled in
**						NO_FILE	 if unable to find file
**						OUT_OF_MEM if not enough memory
**						BAD_FORMAT if not dBASE file
**		author:	 	Mark Sadler
**		revised:		6/18/87
*/

#include <stdio.h>
#include <stdlib.h>
#include "dbf.h"

short int getword(FILE *fid)
{
  unsigned char b[2];

  b[0] = getc(fid);
  b[1] = getc(fid);
  return b[1] *256 + b[0];
}

long int getlong(FILE *fid)
{
  unsigned char b[3];

  b[0] = getc(fid);
  b[1] = getc(fid);
  b[2] = getc(fid);
  b[3] = getc(fid);
  return ((b[3]*256L + b[2])*256L +b[1]) *256L + b[0];
}

void putword(short int w,FILE *fid)
{
  putc(w,fid);
  putc(w>>8,fid);
}

void putlong(long int w,FILE *fid)
{
  putc(w,fid);
  putc(w>>8,fid);
  putc(w>>16,fid);
  putc(w>>24,fid);
}

int d_open(struct DBF *dbf)
{
  int i;
  int n;

  dbf->status = not_open;							 /* in case can not open       */
  if((dbf->file_ptr = fopen(dbf->filename,"r+b")) == NULL)
    return(NO_FILE);

  rewind(dbf->file_ptr);

/* read prolog */

  dbf->dbf_version    = getc(dbf->file_ptr);
  dbf->update_yr  = getc(dbf->file_ptr);
  dbf->update_mo  = getc(dbf->file_ptr);
  dbf->update_day = getc(dbf->file_ptr);
  dbf->records        = getlong(dbf->file_ptr);
  dbf->header_length  = getword(dbf->file_ptr);
  dbf->record_length  = getword(dbf->file_ptr);

/*  fread(&dbf->dbf_version,1,12,dbf->file_ptr);*/

  if (dbf->dbf_version != DB3FILE					/* check for dbiii file marker */
    && (dbf->dbf_version != DB3WITHMEMO
    || dbf->update_mo == 0))
  {
    fclose(dbf->file_ptr);
    return(BAD_FORMAT);
  }

  dbf->current_record = 0L;
  dbf->num_fields = ((dbf->header_length - (HEADER_PROLOG+1)) / FIELD_REC_LEN);

  if((dbf->fields_ptr = (struct FIELD_RECORD *)calloc(dbf->num_fields,FIELD_REC_LEN))==NULL)
    return(OUT_OF_MEM);

  /* position at field descriptions */
  fseek(dbf->file_ptr,(long)HEADER_PROLOG,0);

  /* read into field description array */
  fread((void *)dbf->fields_ptr,sizeof *dbf->fields_ptr,(unsigned)dbf->num_fields,dbf->file_ptr);

  if((dbf->record_ptr = (char *)malloc(dbf->record_length))==NULL)
    return(OUT_OF_MEM);

  /* initialize pointers to fields in record. */
  for(i=0,n=1;i<dbf->num_fields;i++)		/* n is offset from start of rec     */
  { 											/* @ n=0 is the deleted record flag  */
    dbf->fields_ptr[i].field_data_address = dbf->record_ptr + n;
    n += dbf->fields_ptr[i].len;
  }

  dbf->status = not_updated;						 /* open successfull */
  return(0);
}

#ifdef DEBUG_MAIN		/* test program */
#include <string.h>

int main(int argc,char **argv)
{
  struct DBF d;
  int errornum;
  int i;

  if (argc != 2)
  {
    printf("Usage d_open filename");
    return 1;
  }

  strcpy(d.filename,argv[1]);
  if(!strchr(d.filename,'.'))										 /* default to .dbf file	 */
    strcat(d.filename,".DBF");

  if((errornum = d_open(&d))!=0)												 /* open file							*/
  {
    printf("Error opening file: ");
    switch (errornum)
    {
    case OUT_OF_MEM:
      printf("Not enough memory.\n");
      break;
    case NO_FILE:
      printf("Can not open file %s.\n",d.filename);
      break;
    case BAD_FORMAT:
      printf("File %s is not a dBASE III file.\n",d.filename);
      break;
    }
    return 1;
  }

  printf("\nFile Name: %s",d.filename);
  printf("\nCurrent Record: %ld",d.current_record);
  printf("\nDBF signature: %d",d.dbf_version);
  if (d.status==not_updated)
    printf("\nStatus Not Updated");
  else
    printf("\nStatus Not Open");
  printf("\nNumber of Fields: %d",d.num_fields);
  printf("\nUpdate Year: %d",d.update_yr);
  printf("\nUpdate Month: %d",d.update_mo);
  printf("\nUpdate Day: %d",d.update_day);
  printf("\nNumber of Records: %ld",d.records);
  printf("\nRecord Length: %d",d.record_length);
  for (i=0;i<d.num_fields;i++)
    printf("\nField #%d: %s",i,d.fields_ptr[i].name);

  return 0;
}
#endif
