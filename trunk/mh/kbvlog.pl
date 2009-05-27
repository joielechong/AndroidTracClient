#! /usr/bin/perl -w

use strict;
use DBI;
my $dbh = DBI->connect("dbi:Pg:dbname=mh");
my $sth = $dbh->prepare("insert into kbvlog (systeem,bron,tijd,proces,type,bericht) values (?,?,?,?,?,?)");

my $filename = shift;

print "filename = $filename\n";

open FILE, "<$filename" or die "Kan $filename niet openen, $!\n";

while (<FILE>) {
    print;
    chomp;
    my ($systeem,$tijd,$bron,$proces,$type,@bericht) = split(",");
    my $bericht = join(",",@bericht);
    $sth->execute($systeem,$bron,$tijd,$proces,$type,$bericht);
}
close FILE;


$dbh->disconnect;
