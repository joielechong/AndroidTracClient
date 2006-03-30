#! /usr/bin/perl -w

use strict;
use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=httpd", "mfvl", "gotect03");
my $saldos = $dbh->selectall_arrayref("SELECT girorekening,max(datum) from saldo where rekening='Girorekening' group by girorekening;");

my $maxrow=$#$saldos;

my $sth1 = $dbh->prepare("SELECT saldo from saldo where girorekening=? and datum=? and rekening='Girorekening'");

my $sth2 = $dbh->prepare("SELECT datum,sum(bedrag) FROM giro WHERE girorekening=? and datum >?  group by datum order by datum");

my $sth3 = $dbh->prepare("insert into saldo values (?,?,'Girorekening',?)");
my @row;

for my $i (0..$maxrow) {
    my ($rek,$datum)=@{$$saldos[$i]};

    $sth1->execute($rek,$datum);
    @row = $sth1->fetchrow_array;
    my $saldo=$row[0];
    $sth2->execute($rek,$datum);
    
     while ( my @row = $sth2->fetchrow_array ) {
	 $saldo += $row[1];
	 print join(",",@row),",",$saldo,"\n";
	 $sth3->execute($row[0],$rek,$saldo);
    }
}
