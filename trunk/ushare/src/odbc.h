#ifndef _USHARE_ODBC_H_
#define _USHARE_ODBC_H_

#include "metadata.h"

int init_odbc(const char *);
void odbc_finish(int);
long entry_stored(int,char *);
int store_entry(int,struct upnp_entry_t *);

#endif
