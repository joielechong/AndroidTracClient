#! /usr/local/bin/perl -w

use strict;
use XML::Simple;
use Data::Dumper;

my $ref=XMLin("09-SystemTest.xml",KeyAttr => { EAScenario => "+xmi.id" });

print Dumper($ref);

