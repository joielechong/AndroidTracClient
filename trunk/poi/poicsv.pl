#! /usr/bin/perl -w

use Geo::Distance;
use Data::Dumper;
use DBI;

#my $dbh = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=radar.mdb");
#my $dbh = DBI->connect("dbi:SQLite2:dbname=radar");
my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $posten = $dbh->selectall_arrayref("SELECT lat,lon,posten.id,file,commentaar,type from posten,inputfiles where file=naam order by lat,lon;");

my $maxrow=$#$posten;

for my $i (0..$maxrow) {
    my ($lat1,$lon1,$id1,$file1,$rem1,$type1)=@{$$posten[$i]};

       print "$lat1;$lon1\;\"$file1\"\;\"$rem1\"\n";
}
