#! /usr/bin/perl -w

use strict;
use DBI;

my $dsnnew="Provider=Microsoft.Jet.OLEDB.4.0;Data Source=I:\\B&B BR\\Werkruimte\\Specificatie\\specificatie_gegevens.mdb";
my $dsnold="Provider=Microsoft.Jet.OLEDB.4.0;Data Source=Z:\\specificatie_gegevens\\specificatie_gegevens.mdb";
my $dbhold = DBI->connect("dbi:ADO:$dsnold" ) or die $DBI::errstr;
my $dbhnew = DBI->connect("dbi:ADO:$dsnnew" ) or die $DBI::errstr;

my $sthnew=$dbhnew->prepare("select * from Testconfiguraties order by Testconfiguratie");
my $sthold=$dbhold->prepare("select * from Testconfiguraties order by Testconfiguratie");

$sthnew->execute();
while (my @row=$sthnew->fetchrow_array) {
	print join(",", @row),"\n";
}
