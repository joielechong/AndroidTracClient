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
	next unless $3 eq "A";
	my $rek=$1;
	$rek = 'ASN AANDELENFONDS' if $rek eq 'ABF';
	$rek = 'OHRA TOTAAL FONDS' if $rek eq 'OTF';
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
    
    open PIJP,"$DBFLST $DBFBAS/aandeel.dbf | " or die "kan aandeel niet openenen\n";
    
    open (PSQL,"|psql -q -d koersdata") or die "Cannot start psql: $!\n";
    
    print PSQL "delete from aandeelid;\n";
    print PSQL "copy aandeelid from stdin;\n";
    
    while (<PIJP>) {
	s/ \.  /0.00/g;
	my ($datum,$aantal,$kosten,$actieid,$rekid,$koers)= /^  (\d+)[ ]+([+-]?\d+\.\d+)[ ]+([+-]?\d+\.\d+|       )[ ]+(\d+)[ ]+(\d+)[ ]+([+-]?\d+\.\d+)$/;
	
	$kosten=0 if $kosten eq "       ";
	$kosten/=$EURO;
	$koers/=$EURO if ($datum < 19990000);
	
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
	    
	    next if ($rekid == -1);
	    $rekids{$rekorg}=$rekid;
	}
	$datum =~ s/(....)(..)(..)/$3-$2-$1/;
	printf PSQL "%d\t%s\t%d\t%.4f\t%.2f\t%.4f\n",$rekid,$datum,$actieid,$aantal,$kosten,$koers;
    }
    
    close (PSQL);
    
    close(PIJP);
}

my $result=$conn->exec("select max(datum) from koers where naam='AMS EOE INDEX'");
my @row = $result->fetchrow;
my $dk=$row[0];

$result=$conn->exec("select naam,actietab.actie,sum(aantal),sum(aantal*koers),sum(kosten) from aandeelid,rekening,actietab  where aandeelid.id=rekening.id and actietab.id=aandeelid.actie  group by naam,actietab.actie order by naam,actietab.actie'");

my ($somaantal,$sombedr,$opbrengst,$somkost);
my $fonds="";
if ($result->resultStatus == PGRES_TUPLES_OK ) {
    while (@row = $result->fetchrow) {
	my ($naam,$actie,$aantal,$bedrag,$kosten);
	($naam,$actie,$aantal,$bedrag,$kosten) = @row;
	
	if ($fonds ne $naam) { 
	    if ($fonds ne '') {
		printf "      %-20s ========== ========== ========\n", ' ';
		printf "      %-20s %10.4f %10.2f %8.2f\n", ' ', $somaantal,$sombedr,$somkost;
		if ($somaantal > 0) {
		    my $reskoers=$conn->exec("select slot from koers where naam='$fonds' and datum='$dk'");
		    my @rk = $reskoers->fetchrow;
		    my $koers;
		    unless (@rk) {
			$reskoers=$conn->exec("select max(datum) from koers where naam='$fonds'");
			@rk=$reskoers->fetchrow;
			my $dk=$rk[0];
			my $resk=$conn->exec("select slot from koers where naam='$fonds' and datum='$dk'");
			@rk = $resk->fetchrow;
		    }
		    $koers=$rk[0];
		    printf "      KOERS %10.4f     %10.4f %10.2f\n",$koers,$somaantal,$somaantal*$koers;
		    $opbrengst -= $somaantal*$koers;
		}
		printf "      %-20s %10s %10.2f\n\n","OPBRENGST"," ",-$opbrengst-$somkost;
	    }
	    printf "%s\n", $naam;
	    $fonds = $naam;
	    $somaantal = 0;
	    $sombedr = 0;
	    $opbrengst = 0;
	    $somkost = 0;
	}
	printf "      %-20s %10.4f %10.2f %8.2f\n", $actie, $aantal, $bedrag, $kosten;
	$somaantal += $aantal;
	$sombedr += $bedrag;
	$somkost += $kosten;
	$opbrengst += $bedrag if (($actie eq "INLEG") or ($actie eq "OPNAME") or ($actie eq "SALARIS") or ($actie eq "SALDO"));
    }
    
    if ($fonds ne '') {
	printf "      %-20s ========== ========== ========\n", ' ';
	printf "      %-20s %10.4f %10.2f %8.2f\n", ' ', $somaantal, $sombedr,$somkost;
	if ($somaantal > 0) {
	    my $reskoers=$conn->exec("select slot from koers where naam='$fonds' and datum='$dk'");
	    my @rk = $reskoers->fetchrow;
	    my $koers;
	    unless (@rk) {
		$reskoers=$conn->exec("select max(datum) from koers where naam='$fonds'");
		@rk=$reskoers->fetchrow;
		my $dk=$rk[0];
		my $resk=$conn->exec("select slot from koers where naam='$fonds' and datum='$dk'");
		@rk = $resk->fetchrow;
	    }
	    $koers=$rk[0];
	    printf "      KOERS %10.4f     %10.4f %10.2f\n",$koers,$somaantal,$somaantal*$koers;
	    $opbrengst -= $somaantal*$koers;
	}
	printf "      %-20s %10s %10.2f\n\n","OPBRENGST"," ",-$opbrengst-$somkost;
    }
}

