extern int nfields;

extern char *fieldnames[10];

extern struct result
{
  char *veld;
  double waarde[10];
} *results;

long getResults(const char *dbName, const char *query);
