#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;
use Spreadsheet::WriteExcel;
use Spreadsheet::WriteExcel::Utility;

my $workbook = Spreadsheet::WriteExcel->new('overview.xls');
my %sheets;

my $bold=$workbook->add_format();
$bold->set_bold();
my $centerbold=$workbook->add_format();
$centerbold->set_bold();
$centerbold->set_align("center");
my $italic=$workbook->add_format();
$italic->set_italic();
my $boldbig=$workbook->add_format();
$boldbig->set_bold();
$boldbig->set_size(14);
my $wrap=$workbook->add_format();
$wrap->set_text_wrap();
my $wrapbold=$workbook->add_format();
$wrapbold->set_text_wrap();
$wrapbold->set_bold();
my $center=$workbook->add_format();
$center->set_align("center");
my $num=$workbook->add_format();
$num->set_num_format("$#,##0.00");
my $numbold=$workbook->add_format();
$numbold->set_num_format("$#,##0.00");
$numbold->set_bold();

my $overz = $workbook->add_worksheet("Overzicht");
$overz->set_header('&C&"Bold"&A');
$overz->set_landscape();

my $dbh = DBI->connect("dbi:Pg:dbname=httpd");
my $sth1 = $dbh->prepare("SELECT DISTINCT categorie FROM giro ORDER BY categorie");
my $sth2 = $dbh->prepare("SELECT min(datum) AS start,max(datum) AS eind from giro");
my $sth3 = $dbh->prepare("SELECT * from giro_maand");

$sth1->execute();
my $cattemp = $sth1->fetchall_arrayref();
$sth2->execute();
my ($start,$eind) = $sth2->fetchrow_array();

my @categories;
foreach my $cat (@$cattemp) {
	push @categories,$cat->[0];
}

my $startjaar=int($start / 10000);
my $eindjaar =int($eind / 10000);

$sth3->execute();
my $data=$sth3->fetchall_arrayref();
my %db;
my %cols;

my $colptr=1;
$overz->write(0,0,"Jaar",$centerbold);
foreach my $cat (sort @categories) {
	$cols{$cat} = $colptr;
	$overz->write(0,$colptr,$cat,$centerbold);
	my $celstr = xl_rowcol_to_cell(1,$colptr);  
	my $celend = xl_rowcol_to_cell($eindjaar-$startjaar+1,$colptr);  
	my $formula=("=SUM($celstr:$celend)");
	$overz->write($eindjaar-$startjaar+2,$colptr++,$formula,$numbold);
}

for my $jaar ($startjaar..$eindjaar) {
	foreach my $cat (@categories) {
		$db{$jaar}->{$cat}=0;
	}
	my $ws = $workbook->add_worksheet($jaar);
	$ws->set_landscape();
	$sheets{$jaar} = $ws;
	$overz->write($jaar+1-$startjaar,0,$jaar,$bold);
	$ws->set_header('&C&"Bold"&A');
	$ws->write(0,0,"Maand",$centerbold);
	for my $maand (1..12) {
		$ws->write($maand,0,$maand,$bold);
	}
	foreach my $cat (sort @categories) {
		my $col=$cols{$cat};
		$ws->write(0,$col,$cat,$centerbold);
		my $celstr = xl_rowcol_to_cell(1,$col);  
		my $celend = xl_rowcol_to_cell(12,$col);  
		my $formula=("=SUM($celstr:$celend)");
		$ws->write(13,$col++,$formula,$numbold);
	}
}

foreach my $m (@$data) {
	my $maand=$m->[0];
	my $jaar = int($maand/100);
	my $cat = $m->[1];
	my $bedrag = $m->[2];
	$db{$maand}->{$cat}=$bedrag;
	$db{$jaar}->{$cat} += $bedrag;
}
#print Dumper(\%db);

foreach my $key (sort keys %db) {
	my $ws;
	my $row;
	if ($key < 10000) {
		$ws = $overz;
		$row = $key - $startjaar + 1;
	} else {
		my $jaar = int($key/100);
		my $maand = $key - 100*$jaar;
		$ws = $sheets{$jaar};
		$row = $maand;
	}
	foreach my $cat (sort @categories) {
		my $col=$cols{$cat};
		my $bedrag = $db{$key}->{$cat};
		$ws->write($row,$col++,$bedrag,$num) unless (!defined($bedrag) or ($bedrag == 0));
	}	
}

exit();