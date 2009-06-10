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
    
    open PIJP,"$DBFLST $DBFBAS/giro.dbf | " or die "kan giro niet openenen\n";
    
    open (PSQL,"|psql -q -d koersdata") or die "Cannot start psql: $!\n";
#    open (PSQL,"|cat") or die "Cannot start psql: $!\n";
    
    print PSQL "delete from giro;\n";
    print PSQL "copy giro from stdin;\n";
    
    while (<PIJP>) {
#	print;
	chomp;
	my ($datum,$mut,$volgnr,$reknr,$naam,$bud,$bedrag,$opmerk) = /  (........) (...) (...) (..........) (................................) (...) (..........) (.*)/;
	$volgnr=0 if $volgnr eq "   ";
	$opmerk=~s/[ ]+$//;
	$naam=~s/[ ]+$//;
	$mut=~s/[ ]+$//;
	$datum =~ s/(....)(..)(..)/$3-$2-$1/;
	printf  PSQL "%s\t%s\t%d\t%d\t%s\t%d\t%.2f\t%s\n",$datum,$mut,$volgnr,$reknr,$naam,$bud,$bedrag,$opmerk;
#	printf  "%s\t%s\t%d\t%d\t%s\t%d\t%.2f\t%s\n",$datum,$mut,$volgnr,$reknr,$naam,$bud,$bedrag,$opmerk;
	
    }
    close (PSQL);
    
    close(PIJP);
}


