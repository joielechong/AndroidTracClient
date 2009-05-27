#! /usr/bin/perl -w

use strict;
use DBI;

my $dbname=shift;
my $table=shift;
my $dsnnew="Provider=Microsoft.Jet.OLEDB.4.0;Data Source=".$dbname;
print $dsnnew,"\n";

my $dbhnew = DBI->connect("dbi:ADO:$dsnnew" ) or die $DBI::errstr;

my $sthnew=$dbhnew->prepare("select * from $table");

$sthnew->execute();
while (my @row=$sthnew->fetchrow_array) {
	print join(",", @row),"\n";
}
