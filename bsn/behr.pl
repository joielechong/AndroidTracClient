#! /usr/bin/perl -w

use strict;

my @maand=("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC");
my $naam = shift;

my $file= shift;

open FILE,"<$file" or die "Kan $file niet openen\n";

while (<FILE>) {
    chomp;
    my ($datum,$koers) = split(": ");
    $datum =~ m/(\d\d)(\d\d)(\d\d)/;
    my $jaar  = $1;
    my $maand = $maand[$2 - 1];
    my $dag   = $3;
    print "0/1/$dag-$maand-$jaar\n9|30|".uc($naam)."|0|$koers|$koers|$koers|$koers\n";
}

close FILE;
