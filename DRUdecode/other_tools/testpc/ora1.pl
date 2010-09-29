#! /usr/bin/perl -w

use strict;
use DBI;

my $dbh=DBI->connect("dbi:Oracle:host=176.176.176.1;port=1521;sid=vis",'vis/vis') or die $!;
my @tables=$dbh->tables();

print join("\n", @tables),"\n";
