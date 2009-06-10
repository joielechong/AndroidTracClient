#! /usr/bin/perl -w

use strict;

open PIJP, "psql -d koersdata -qAtc \"select naam from rekening where naam like '% (US)' and private='f';\"|" or die "kan geen pijp openen\n";

while (<PIJP>) {
    print ;
    chomp;
    system("del_fonds \"$_\"");
}
close PIJP;
