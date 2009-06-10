#! /usr/bin/perl -w

use strict;
use Pg;

my %rekids;
my %acties;

my $conn=Pg::connectdb("dbname=koersdata") or die "Cannot connect to database: $!\n";
die "Cannot connect to database\n" unless ($conn->status == PGRES_CONNECTION_OK);

if (shift) {
    my $EURO=2.20371;
    my $DBFLST="/usr/local/bin/dbflst";
    my $DBFBAS="/p233/c/dbase/geld";
    
    my @acties;
    
    open PIJP,"$DBFLST $DBFBAS/budgetcd.dbf | " or die "kan budgetcd niet openenen\n";
    
    open (PSQL,"|psql -q -d koersdata") or die "Cannot start psql: $!\n";
#    open (PSQL,"|cat") or die "Cannot start psql: $!\n";
    
    print PSQL "delete from budgetcd;\n";
    print PSQL "copy budgetcd from stdin;\n";
    
    while (<PIJP>) {
#	print;
	chomp;
	my ($budgetcode,$id) = /  (....................) (...)(.*)/;
	printf  PSQL "%s\t%d\n",$budgetcode,$id;
#	printf  "%s\t%d\n",$budgetcode,$id;

	
    }
    close (PSQL);
    
    close(PIJP);
}


