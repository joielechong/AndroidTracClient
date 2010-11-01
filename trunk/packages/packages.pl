#! /usr/bin/perl -w

use strict;
use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=mfvl user=mfvl");
