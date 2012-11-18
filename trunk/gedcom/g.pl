#! /usr/bin/perl -w

use strict;

use Gedcom;
use Data::Dumper;

my $geda=Gedcom->new('vanloon.ged');
my $gedm=Gedcom->new('vanloon-mundia.ged');

my @indm = $gedm->individuals();
my @famm = $gedm->families();

for my $i (@indm) {
    my $j = $geda->get_individual($i->name);
    if (defined($j)) {
	print $i->name,' ', $j->name,"\n";
    } else {
	print $i->name, " is er niet\n";
    }
}
