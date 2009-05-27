/* 
**		file:		dbcopy.c
**		purpose:	program to copy structure and records to another file
**		usage:	dbcopy fromfile tofile
**		notes:	dos copy does this faster, but this program exercizes most
**					of the dbf functions
**		author:	Mark Sadler
**		revised:	6/19/87
*/


#include <stdio.h>
#include <alloc.h>
#include <string.h>
#include "dbf.h"

void db_error(int errornum,char *filename)
{
  printf("\nError opening file: ");
  switch (errornum)
  {
  case OUT_OF_MEM:
    printf("Not enough memory.\n");
    break;
  case NO_FILE:
    printf("Can not open file %s.\n",filename);
    break;
  case BAD_FORMAT:
    printf("File %s is not a dBASE III file.\n",filename);
    break;
  }
}

void main(int argc,char *argv[])
{
  struct DBF in,out;
  int errornum,fld;
  unsigned long rec;
  char *buff;

  if(argc < 2)
  {
    printf("Usage DBCOPY infile outfile\n");
    exit(1);
  }

  /* open input file */
  strcpy(in.filename,argv[1]);
  if(!strchr(in.filename,'.'))			/* default to .dbf file	 */
    strcat(in.filename,".DBF");
  if((errornum = d_open(&in))!=0)		/* open file				 */
  {
    db_error(errornum,in.filename);
    exit(1);
  }

  /* copy input file structure to outputfile */
  strcpy(out.filename,argv[2]);
  if(!strchr(out.filename,'.'))					/* default to .dbf file	 */
    strcat(out.filename,".DBF");
  if((errornum = d_cpystr(&in,&out))!=0)		/* open file				 */
  {
    db_error(errornum,out.filename);
    exit(1);
  }

  for (rec=1L;rec <= in.records;rec++)			/* records loop */
  {
    d_getrec(&in,rec);

    /* 
		**		note: this loop could be replaced by just copying the record with
		**				using memcopy to copy to the out file record.  faster still
		**				by changing the out file structure record pointer to point
		**				to the record in the in file structure.
		*/ 
    if ((buff=(char *)malloc(MAX_RECORD))==NULL)
    {
      db_error(OUT_OF_MEM,"filename error");
      exit(1);
    }
    for (fld=1;fld <= in.num_fields;fld++)		/* fields loop  */
    {
      d_getfld(&in,fld,buff);
      d_putfld(&out,fld,buff);
    }	/* fields loop  */

    *out.record_ptr = *in.record_ptr;		/* copy deleted file marker */
    d_addrec(&out);
  }	/* records loop */
  free(buff);
  d_close(&in);
  d_close(&out);
}

