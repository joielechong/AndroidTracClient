#include <time.h>
#include <ctype.h>
#include <string.h>
#include <dbf.h>
#include <mfl.h>

int d_create_init(struct DBF *d)
{
  time_t t;
  int i;
  struct tm *tp;

  d->status = creating;
  if((d->file_ptr = fopen(d->filename,"w+b")) == NULL)
    return(NO_FILE);

  d->dbf_version = DB3FILE;
  time(&t);
  tp = localtime(&t);
  d->update_yr = tp->tm_year;
  d->update_mo = tp->tm_mon + 1;
  d->update_day= tp->tm_mday;
  d->records = 0;
  d->num_fields = 0;
  d->header_length = HEADER_PROLOG + 1;
  d->record_length = 1;
  d->fields_ptr = NULL;
  d->record_ptr = NULL;
  d->current_record = 0;
  rewind(d->file_ptr);

  putc(d->dbf_version,d->file_ptr);
  putc(d->update_yr,d->file_ptr);
  putc(d->update_mo,d->file_ptr);
  putc(d->update_day,d->file_ptr);
  putlong(d->records,d->file_ptr);
  putword(d->header_length,d->file_ptr);
  putword(d->record_length,d->file_ptr);

  for(i=12;i<HEADER_PROLOG;i++)
    putc(0,d->file_ptr);

  return 0;
}

int d_create_end(struct DBF *d)
{
  putc(0xd,d->file_ptr);

  rewind(d->file_ptr);

  putc(d->dbf_version,d->file_ptr);
  putc(d->update_yr,d->file_ptr);
  putc(d->update_mo,d->file_ptr);
  putc(d->update_day,d->file_ptr);
  putlong(d->records,d->file_ptr);
  putword(d->header_length,d->file_ptr);
  putword(d->record_length,d->file_ptr);

  fclose(d->file_ptr);
  return d_open(d);
}

int d_create_field(struct DBF *d,char *name,char type,unsigned short int len,unsigned short int dec)
{
  struct FIELD_RECORD fd;

  memset(&fd,0,sizeof(fd));
  strncpy(fd.name,name,sizeof(fd.name)-1);
  strupr(fd.name);
  fd.typ = toupper(type);
  if(strchr("CNLDM",fd.typ) == NULL)
    return -1;
  if(fd.typ == 'M')
    d->dbf_version = 0x83;
  fd.len = (fd.typ == 'D' ? 8:len);
  fd.dec = dec;
  d->num_fields++;
  d->header_length += FIELD_REC_LEN;
  d->record_length += len;

  fwrite(&fd,sizeof(fd),1,d->file_ptr);

  return 0;
}

#ifdef DEBUG_MAIN
int main(void)
{
  struct DBF d;

  strcpy(d.filename,"test.dbf");
  d_create_init(&d);
  d_create_field(&d,"NAAM",'C',15,0);
  d_create_field(&d,"DATUM",'D',8,0);
  d_create_end(&d);
  d_close(&d);
  return 0;
}

#endif
