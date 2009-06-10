#! /user/bin/perl -w

use strict;
use Pg;

my $conn=Pg::connectdb("dbname=koersdata") or die "Cannot connect to database: $!\n";

my $result=$conn->exec("select naam,id from rekening where naam like 'ASN G%'");

my @row;
my %fondsen;
while (@row = $result->fetchrow) {
    $fondsen{$row[0]}=$row[1];
}

my $minverh=999999;
my $maxverh=-1;
my $count=0;
my $tuples=0;
my $euro=2.20371;

my $fonds;
foreach $fonds (sort keys %fondsen) {
    my $id=$fondsen{$fonds};
    $result=$conn->exec("select max(datum) from koersid where id=$id and datum <\'1-1-1999\';");
    next if $result->getisnull(0,0);
    my $lowdate=$result->getvalue(0,0);
    $result=$conn->exec("select min(datum) from koersid where id=$id and datum >=\'1-1-1999\';");
    next if $result->getisnull(0,0);
    my $hidate=$result->getvalue(0,0);
    $result=$conn->exec("select slot from koersid where id=$id and datum =\'$lowdate';");
    my $lowkoers=$result->getvalue(0,0);
    $result=$conn->exec("select slot from koersid where id=$id and datum =\'$hidate';");
    my $hikoers=$result->getvalue(0,0);
    my $verhouding=$lowkoers/$hikoers;
    next if $verhouding < 1.5;
    $maxverh=$verhouding if $verhouding > $maxverh;
    $minverh=$verhouding if $verhouding < $minverh;
    $count++;
    print "$count $fonds $id $lowdate $lowkoers $hidate $hikoers $verhouding\n";
    $result=$conn->exec("update koersid set open=open/$euro, hoog=hoog/$euro, laag=laag/$euro, slot=slot/$euro where datum <'1-1-1999' and id=$id;");
    print "Updated ",$result->cmdTuples," tuples\n";
    $tuples += $result->cmdTuples;
}

print "$count fondsen, $tuples records gewijzigd\n";
