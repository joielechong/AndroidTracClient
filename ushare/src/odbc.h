#ifndef _USHARE_ODBC_H_
#define _USHARE_ODBC_H_

#include "metadata.h"

int init_odbc(const char *);
void odbc_finish(int);
long entry_stored(int,char *);
int store_entry(int,struct upnp_entry_t *,int);
struct upnp_entry_t *fetch_entry(int,int);
struct upnp_entry_t **fetch_children(int,struct upnp_entry_t *);
long get_last_entry(int);
char *get_next(int,long,long *);
void del_entry(int,char *);
#endif
