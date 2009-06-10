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
    
    open PIJPREK,"$DBFLST $DBFBAS/rekening.dbf |" or die "kan rekening niet openen\n";
    
    my @rekening;
    
    while (<PIJPREK> ) {
	/^[ ]*([A-Z0-9_-]+)[ ]+(\d+) ([A-Z]).*$/;
	next unless $3 eq "S";
	my $rek=$1;
	$rek = 'ASN PLUSREKENING' if $rek eq 'ASNPLUS';
	$rek = 'ASN MILIEUREKENING' if $rek eq 'ASN';
	$rekening[$2]=$rek;
    }
    close PIJPREK;

    my @acties;
    
    open PIJPACT,"$DBFLST $DBFBAS/actietab.dbf |" or die "kan actietab niet openen\n";
    
    while (<PIJPACT> ) {
	/^[ ]*([A-Z0-9_-]+)[ ]+(\d+)$/;
	$acties[$2]=$1;
    }
    close PIJPACT;
    
    open PIJP,"$DBFLST $DBFBAS/spaar.dbf | " or die "kan spaar niet openenen\n";
    
    open (PSQL,"|psql -q -d koersdata") or die "Cannot start psql: $!\n";
#    open (PSQL,"|cat") or die "Cannot start psql: $!\n";
    
    print PSQL "delete from spaar;\n";
    print PSQL "copy spaar from stdin;\n";
    
    while (<PIJP>) {
#	print;
	s/ \.  /0.00/g;
	my ($datum,$bedrag,$actieid,$rekid)= /^  (\d+)[ ]+([+-]?\d+\.\d+)[ ]+(\d+)[ ]+(\d+).*$/;
	
	my $rekening=$rekening[$rekid];
	my $actie=$acties[$actieid];
	my $rekorg=$rekening;

	if (exists $acties{$actie}) {
	    $actieid=$acties{$actie};
	} else {
	    my $result=$conn->exec("select id from actietab where actie='$actie'");
	    if ($result->resultStatus == PGRES_TUPLES_OK ) {
		my @row=$result->fetchrow;
		$actieid=$row[0];
		if (@row) {
		    $acties{$actie}=$actieid;
		} else {
		    die "$actie bestaat nog niet\n";
		}
	    }
	}
	
	if (exists $rekids{$rekening}) {
	    $rekid=$rekids{$rekening};
	} else {
	    
	    my $result=$conn->exec("select naam2 from vertaal where naam1='$rekening'");
	    if ($result->resultStatus == PGRES_TUPLES_OK ) {
		my @row=$result->fetchrow;
		if (@row) {
		    if ($row[0] eq "-") {
			print STDERR "$rekening kan niet worden vertaald\n";
			next;
		    }
		    $rekening=$row[0];
		}
	    }
	    
	    $result=$conn->exec("select id from rekening where naam='$rekening'");
	    if ($result->resultStatus == PGRES_TUPLES_OK ) {
		my @row=$result->fetchrow;
		if (@row) {
		    $rekid=$row[0];
		} else {
		    print STDERR  "$rekening bestaat nog niet\n";
		    $rekid=-1;
		}
	    }
	    
	    if ($rekid == -1) {
		$result=$conn->exec("select nextid from ids where tabel='rekening';");
		$rekid=$result->fetchrow;
		$result=$conn->exec("insert into rekening values('".$rekening."',".$rekid.",'S','22-06-1999')");
		my $newid=$rekid;
		$newid++;
		$result=$conn->exec("update ids set nextid=$newid where tabel='rekening'");
	    }
	    
	    $rekids{$rekorg}=$rekid;
	}
	$datum =~ s/(....)(..)(..)/$3-$2-$1/;
	printf PSQL "%d\t%s\t%.2f\t%d\n",$rekid,$datum,$bedrag,$actieid;
#	printf  "%d\t%s\t%d\t%.4f\t%.2f\t%.4f\n",$rekid,$datum,$bedrag,$actieid;
    }
    
    close (PSQL);
    
    close(PIJP);
}


