#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;
use Getopt::Long;

require DruCsvFile;

# record_dumped - array met NID_MESSAGE nummers die reeds in de dump file zijn gekomen.
my %record_dumped = ();

#
# Main program
#

my $druFileName = "";
my $jruFileName = "";
my $csvFileName = "";
my $xmlFileName = "";
my $filter = "";
my $verbose = 0;	# debug
my $debug = 0;
my $debugall = 0;

# parse commandline arguments
my $result = GetOptions("dru=s" => \$druFileName,
			"csv=s" => \$csvFileName,
			"xml=s" => \$xmlFileName,
			"filter=s" => \$filter,
			"verbose" => \$verbose,
			"debug" => \$debug,
			"debugall" => \$debugall);

# set debug when debugall
$debug = 1 if $debugall;

# check argument(s)
die "Error: no input file given\n" if $druFileName eq $jruFileName;

# open input file
print "Open DRU file: " . $druFileName . "\n" if $verbose;
open my $DRUfh,$druFileName or die "Error: Kan DRU file " . $druFileName . " niet openen\n";
binmode($DRUfh);
my $DRU = ETCS::DRU::new($DRUfh);
print "Aantal DRU records: ",$DRU->{reccount}."\n" if $verbose;

# zet het filter
if ($filter ne "") {
	my ($mindate, $mintime, $maxdate, $maxtime) = split(/,/, $filter, 4);
	print "Zet filter: " . $mindate . "-" . $mintime . " tot " . $maxdate . "-" . $maxtime . "\n"
		if $verbose;
	$DRU->setFilter($mindate, $mintime, $maxdate, $maxtime);
}


# open output file
my $CsvFile = DruCsvFile::new($csvFileName) if $csvFileName ne "";

# open debug file
open my $dbgFile, ">TEST.TXT" if $debug;

# open xml file
my $fhXml;
if ($xmlFileName ne "") {
	print "Open XML file: " . $xmlFileName . "\n"
		if $verbose;
	open $fhXml,">" . $xmlFileName;
	print $fhXml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DRULog>\n" if $xmlFileName ne "";
}

# zet debug uit in ETCS package
ETCS::Debug::debugOff();

# read records
MAINLOOP: while (my $message=$DRU->next()) {
	
	XMLout($message, NoAttr=>0,OutputFile=>$fhXml,RootName=>$message->{MessageType})
		if $xmlFileName ne "";
		
	# process message to CVS
	$CsvFile->process($message) if $csvFileName ne "";

	# dump record if not dumped already
	if ($debug) {
		my $record_nummer = "JRU-" . $message->{Header}->{Fields}->[0]->{Decimal};
		$record_nummer = "DRU-" . $message->{Header}->{Fields}->[0]->{Decimal}
			if ($message->{MessageType} eq "DRUMessage");
		if (not defined $record_dumped{$record_nummer}) {
			$record_dumped{$record_nummer} = 1 if (not $debugall);
			print "Dump record type " . $record_nummer . "\n";
			print $dbgFile Dumper($message);
		}
	}
}

print $fhXml "</DRULog>\n" if $xmlFileName ne "";
close $fhXml if $xmlFileName ne "";

close $dbgFile if $debug;

$CsvFile->close() if $csvFileName ne "";

close $DRUfh;

print "Klaar\n";
