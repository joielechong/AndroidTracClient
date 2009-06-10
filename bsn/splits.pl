#! /usr/bin/perl -w

use Pg;
# use strict;

my @row;
my $id;

my $fonds  = shift;
my $factor = shift;
my $enddat = shift;

my $conn=Pg::connectdb("dbname=koersdata ") or die "Cannot connect to database: $!\n";

die "Verbinding met databse mislukt.\n".$conn->errorMessage unless $conn->status == PGRES_CONNECTION_OK;

my $result=$conn->exec("select id from rekening where naam = '$fonds'");

die "ID bepaling mislukt\n".$conn->errorMessage unless $result->ntuples == 1;

$id= $result->getvalue(0,0);

unless ($enddat) {
    $result=$conn->exec("select max(datum) from koersid where id=$id");
    die "Datum bepaling mislukt\n".$conn->errorMessage unless $result->ntuples == 1;

    $enddat = $result->getvalue(0,0);
}

$result=$conn->exec("select volume,open,hoog,laag,slot from koersid where id=$id and datum='$enddat'");
die "Slot bepaling mislukt\n".$conn->errorMessage unless $result->ntuples == 1;
$volume=$result->getvalue(0,0);
$open  =$result->getvalue(0,1);
$hoog  =$result->getvalue(0,2);
$laag  =$result->getvalue(0,3);
$slot  =$result->getvalue(0,4);

print "Koersgegevens voor splitsing van $fonds op $enddat: $volume, $open, $hoog, $laag, $slot\n";

$result=$conn->exec("update koersid set volume=volume*$factor, open=open/$factor, hoog=hoog/$factor, laag=laag/$factor, slot=slot/$factor where id=$id and datum <= '$enddat'");

print $result->cmdStatus,"\n";

$result=$conn->exec("select volume,open,hoog,laag,slot from koersid where id=$id and datum='$enddat'");
die "Slot bepaling mislukt\n".$conn->errorMessage unless $result->ntuples == 1;
$volume=$result->getvalue(0,0);
$open  =$result->getvalue(0,1);
$hoog  =$result->getvalue(0,2);
$laag  =$result->getvalue(0,3);
$slot  =$result->getvalue(0,4);

print "Koersgegevens na splitsing van $fonds op $enddat: $volume, $open, $hoog, $laag, $slot\n";

