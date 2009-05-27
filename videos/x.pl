#! /usr/bin/perl -w

use strict;

use Data::Dumper;
use XML::Simple;

my $xml=XMLin('/mnt/sdb1/dvd34.xml');

print Dumper($xml);

open X,">/tmp/x.xml";
print X XMLout($xml,RootName=>"dvdauthor");
close X;
