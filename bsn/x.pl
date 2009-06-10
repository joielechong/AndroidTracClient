#! /usr/bin/perl -w

use strict;

my $cmd="psql koersdata -tc \"select naam from rekening where naam like '%/WARR' order by naam;\"";

open PSQL,"$cmd |" or die "Mis 1, $!";

while (<PSQL>) {
    chomp;
    s/[ ]*$//;
    $cmd = "ins_vert \"$_\" -";
    print $cmd,"\n";
#    system($cmd);
}

close PSQL;
