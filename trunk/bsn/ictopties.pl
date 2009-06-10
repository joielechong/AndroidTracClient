#! /usr/local/bin/perl -w

use strict;
use Pg;

my @maand=('','JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC');

my %opties;
#$opties{ICTOPT97}=10.03;
#$opties{ICTOPT98}=15.89;
$opties{ICTOPT99}=33.45;

my $fonds='ICT';
my $HOME=$ENV{HOME};

my $conn=Pg::connectdb("dbname=koersdata") or die "Cannot connect to database: $!\n";
die "Cannot connect to database\n" unless ($conn->status == PGRES_CONNECTION_OK);
my $optie;

my $filopen=0;

foreach $optie (keys %opties) {
    my $datum;
    my $result=$conn->exec("select max(datum) from koers where naam='$optie'");
    if ($result->resultStatus == PGRES_TUPLES_OK ) {
	my @row=$result->fetchrow;
	$datum=$row[0];
	print "$optie $datum\n";
    }
    $result=$conn->exec("select datum,open,hoog,laag,slot from koers where naam='$fonds' and datum > '$datum' order by datum;");
    if ($result->resultStatus == PGRES_TUPLES_OK ) {
	my @row;
	my ($datum,$open,$hoog,$laag,$slot);
	while (@row = $result->fetchrow) {
	    ($datum,$open,$hoog,$laag,$slot) = @row;
	    my ($jaar,$maand,$dag)=split("-",$datum);
	    $jaar %= 100;
	    $open -= $opties{$optie};
	    $hoog -= $opties{$optie};
	    $laag -= $opties{$optie};
	    $slot -= $opties{$optie};
	    $open = 0.01 if $open < 0.01;
	    $hoog = 0.01 if $hoog < 0.01;
	    $laag = 0.01 if $laag < 0.01;
	    $slot = 0.01 if $slot < 0.01;

	    unless ($filopen) {
		open FILE1,">>$HOME/bsn/data/ictopties.bsn" or die "Kan bsn/data/ictopties.bsn niet openen\n";
		open FILE2,">>$HOME/ftp_ict/ictopties.bsn" or die "Kan ftp_ict/ictopties.bsn niet openen\n";
		$filopen=1;
	    }


	    printf FILE1 "0/1/%2.2d-%s-%2.2d\r\n9|30|%s|%d|%.4f|%.4f|%.4f|%.4f\n",$dag,$maand[$maand],$jaar,$optie,0,$open,$hoog,$laag,$slot;
	    printf FILE2 "0/1/%2.2d-%s-%2.2d\r\n9|30|%s|%d|%.4f|%.4f|%.4f|%.4f\n",$dag,$maand[$maand],$jaar,$optie,0,$open,$hoog,$laag,$slot;
	}
    }
}

close FILE1 if $filopen;
close FILE2 if $filopen;
