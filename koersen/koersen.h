enum types {normaal, optie_fl, optie_rel_fl,optie_eur,optie_rel_eur,laatste};

#define MAXTABEL 200

struct tabel
{
  char *tt_nam;
  char *db_nam;
  double koers;
  enum types type;
  double conv_koers;
};

extern struct tabel tabel[MAXTABEL];

extern int tabcnt;

