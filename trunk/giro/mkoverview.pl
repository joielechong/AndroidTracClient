#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;
use Spreadsheet::WriteExcel;

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

my $startjaar=$start / 10000;
my $eindjaar =$eind / 10000;

$sth3->execute();
my $data=$sth3->fetchall_arrayref();
my %db
for (my $jaar=$startjaar;$jaar<=$eindjaar;$jaar++) {
	for my $cat (@categories) {
		$db{$jaar}->{$cat}=0;
	}
}
foreach my $m (@$data) {
	my $maand=$m->[0];
	my $jaar = $maand/100;
	my $cat = $m->[1];
	my $bedrag = $m->[2];
	$db{$maand}->{$cat}=$bedrag;
	$db{$jaar}->{$cat} += $bedrag;
}
print Dumper(\%db);

exit();

my $workbook = Spreadsheet::WriteExcel->new('overview.xls');
my $overz = $workbook->add_worksheet("Overzicht");
$overz->set_header('&C&"Bold"&A');
$overz->set_footer('&L&D&R&P/&N');
$overz->set_landscape();
$overz->set_margins_TB(1.8/2.54);
$overz->set_margins_LR(1.5/2.54);
$overz->set_column(0,0,65);
$overz->set_column(1,1,16);
$overz->set_column(2,2,40);
#my $status = $workbook->add_format();
#$status->set_num_format('[Color 10]='OPEN';[Red]='FAIL';General');


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

my %rowptrs;
my %sheets;
my %subjects;
my $uc;
my $dep;
my %reqhdr;

$overz->write("A1","KEMA DRS Testspecification",$boldbig);
my $date = localtime(time());
$overz->write("A2","Generated on $date",$boldbig);
$overz->write("A4","Release:",$boldbig);
$overz->write("A5","Tester:",$boldbig);
$overz->write("A6","Date:",$boldbig);
$overz->write("A8","Contents",$bold);
$overz->write("B8","Result",$bold);
$overz->write("C8","Remarks",$bold);

my $overzptr=8;
my $overz1st=$overzptr+1;

my $prsheet = $workbook->add_worksheet("PR Verificatie");
$prsheet->set_header('&C&"Bold"&A');
$prsheet->set_footer('&L&D&R&P/&N');
$prsheet->set_landscape();
$prsheet->set_margins_TB(1.8/2.54);
$prsheet->set_margins_LR(1.5/2.54);
$prsheet->set_column(0,0,7);
$prsheet->set_column(1,1,80);
$prsheet->set_column(2,2,7);
$prsheet->set_column(3,3,40);
$prsheet->write("A1","Ticket",$centerbold);
$prsheet->write("B1","Summary",$centerbold);
$prsheet->write("C1","Result",$centerbold);
$prsheet->write("D1","Remarks/PRs",$centerbold);


my $scenarios=$xmltree->{'XMI.content'}->{'UML:Model'}->{'UML:Namespace.ownedElement'}->{'UML:Package'}->{'UML:Namespace.ownedElement'}->{'UML:UseCase'};
foreach $uc (sort keys %$scenarios) {
#	print Dumper ($uc), Dumper $scenarios->{$uc};
	next if substr($uc,0,3) eq "ZZZ";
	
	my $ws = $workbook->add_worksheet(substr($uc,0,25));
	$ws->set_header('&C&"Bold"&A');
	$ws->set_footer('&L&D&R&P/&N');
	$ws->set_landscape();
	$ws->set_margins_TB(1.8/2.54);
	$ws->set_margins_LR(1.5/2.54);
	$ws->set_column(0,0,14);
	$ws->set_column(1,1,50);
	$ws->set_column(2,3,8,$center);
	$ws->set_column(4,4,50);
	
	$ws->write("A1",$uc,$boldbig);
	
	my $rowptr=1;
	my $tvs = $scenarios->{$uc}->{'UML:ModelElement.taggedValue'}->{'UML:TaggedValue'};
	foreach my $tv (@$tvs) {
		next unless $tv->{tag} eq "documentation";
		my $lines = $tv->{value};
		my @lines = split("\n",$lines);	
		$rowptr++;
		foreach my $line (@lines) {
			if (substr($line,0,1) eq '=') {
				substr($line,0,1) = '-';
			}
			$ws->write($rowptr++,0,$line);
		}
		
	}
	
	$rowptr++;
	$ws->write($rowptr++,0,"Preconditions",$bold);
	
	my $id=$scenarios->{$uc}->{'xmi.id'};
	$subjects{$id}=$uc;
	$sheets{$uc} = $ws;
	my $constraints = $scenarios->{$uc}->{'UML:ModelElement.constraint'}->{'UML:Constraint'};
	my $keeptext;
	foreach my $constraint (keys %$constraints) {
		if ($constraint eq 'UML:ModelElement.taggedValue') {
			my $tvs = $constraints->{$constraint}->{'UML:TaggedValue'};
			foreach my $tv (@$tvs) {
				next unless $tv->{tag} eq "description";
				my $lines = $tv->{value};
				my @lines = split("\n",$lines);	
				foreach my $line (@lines) {
					if (substr($line,0,1) eq '=') {
						substr($line,0,1) = '-';
					}
					$ws->write($rowptr++,1,"  ".$line,$italic);
				}
			}
		} elsif ($constraint eq 'name') {
			$ws->write($rowptr++,1,$constraints->{$constraint});
		} else {
			$ws->write($rowptr++,1,$constraint);
			my $tvs = $constraints->{$constraint}->{'UML:ModelElement.taggedValue'}->{'UML:TaggedValue'};
			foreach my $tv (@$tvs) {
				next unless $tv->{tag} eq "description";
				my $lines = $tv->{value};
				my @lines = split("\n",$lines);	
				foreach my $line (@lines) {
					if (substr($line,0,1) eq '=') {
						substr($line,0,1) = '-';
					}
					$ws->write($rowptr++,1,"  ".$line,$italic);
				}
			}
		}
	}
	$rowptr++;
	$ws->repeat_rows($rowptr,$rowptr);
	$ws->write($rowptr,2,"Pass",$bold);
	$ws->write($rowptr,3,"Fail",$bold);
	$ws->write($rowptr,4,"Remarks/PRs",$bold);
	$rowptr++;
	$rowptrs{$uc} = $rowptr;
	$reqhdr{$uc} = 0;
}

my $testcases=$xmltree->{"XMI.extensions"}->{"EAModel.scenario"}->{EAScenario};
print "Testcase Naam,EA_ID\n";
my $old_subject="";
foreach my $tc (sort keys %$testcases) {
	my $id = $testcases->{$tc}->{"xmi.id"};
	my $subjid = $testcases->{$tc}->{subject};
	my $subject=$subjects{$subjid};
	next unless defined $subject;

	if ($subject ne $old_subject) {
		$overz->write($overzptr++,0,$subject,$bold);
		$old_subject=$subject;
	}
	
	my $type = $testcases->{$tc}->{type};
	print "$tc,$id,$subject\n";
	my $rowptr=$rowptrs{$subject};
	my $ws = $sheets{$subject};
	$ws->write($rowptr,0,$type,$bold);
	$ws->write($rowptr,1,$tc,$wrapbold);

	$overz->write($overzptr,0,$tc);
	$overz->write($overzptr++,1,"OPEN");

	$rowptr++;
	my $descr = $testcases->{$tc}->{"description"};
	if (defined($descr)) {
#		print "$descr\n";
		my @lines = split("\n",$descr);	
		foreach my $line (@lines) {
			if (substr($line,0,1) eq '=') {
				substr($line,0,1) = '-';
			}
			$ws->write($rowptr++,1,$line,$wrap);
		}
	}
	$rowptr++;
	$rowptrs{$subject} = $rowptr;
	$reqhdr{$subject} = 0;
}

$overz->write("E3","TC");
$overz->write("D4","PASS");
$overz->write("D5","FAIL");
$overz->write("D6","NOT");
$overz->write("D7","OPEN");
$overz->write("E4","=COUNTIF(B$overz1st:B$overzptr,D4)");
$overz->write("E5","=COUNTIF(B$overz1st:B$overzptr,D5)");
$overz->write("E6","=COUNTIF(B$overz1st:B$overzptr,D6)");
$overz->write("E7","=COUNTIF(B$overz1st:B$overzptr,D7)");
$overz->write_formula("E8","=SUM(E4:E7)");
my $f_change = $workbook->add_format();
$f_change->set_num_format('0%');
$overz->write("F4","=E4/E8",$f_change);
$overz->write("F5","=E5/E8",$f_change);
$overz->write("F6","=E6/E8",$f_change);
$overz->write("F7","=E7/E8",$f_change);

my $traceability=$xmltree->{'XMI.content'}->{'UML:Model'}->{'UML:Namespace.ownedElement'}->{'UML:Package'}->{'UML:Namespace.ownedElement'}->{'UML:Dependency'};
foreach my $dep (@$traceability) {
	my $tvs = $dep->{'UML:ModelElement.taggedValue'}->{'UML:TaggedValue'};
	my $usecase = undef;
	my $requirement = undef;
	foreach my $tv (@$tvs) {
		$usecase = $tv->{value} if ($tv->{tag} eq "ea_sourceName");
		$requirement = $tv->{value} if ($tv->{tag} eq "ea_targetName");
	}
	if (defined($usecase) and defined($requirement)) {
		next if $usecase =~ /^ZZZ/;
		next if $requirement =~ /.DELETED$/;
		my $rowptr = $rowptrs{$usecase};
		my $ws = $sheets{$usecase};
#		print "$usecase $requirement\n";
		if ($reqhdr{$usecase} == 0) {
			$ws->write($rowptr++,0,"Requirements",$bold);
			$reqhdr{$usecase}=1;
		}
		$ws->write($rowptr++,1,$requirement);
		$rowptrs{$usecase}=$rowptr;
	}
}
