#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "../Perl/lib";   # gebruik lokale versie in subdirectory lib
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use ETCS::DB;
use ETCS::DRU_Ansaldo;
use XML::Simple;
use IO::Handle;
use Getopt::Long;

require DruCsvFile;

# record_dumped - array met NID_MESSAGE nummers die reeds in de dump file zijn gekomen.
my %record_dumped = ();

my $druFileName = "";
my $jruFileName = "";
my $csvFileName = "";
my $xmlFileName = "";
my $filter = "";
my $trainid;	# for db storage
my $verbose = 0;	# debug
my $debug = 0;
my $debugall = 0;

# parse commandline arguments
my $result = GetOptions("dru=s" => \$druFileName,
			"csv=s" => \$csvFileName,
			"xml=s" => \$xmlFileName,
			"filter=s" => \$filter,
			"trainid=s"=> \$trainid,
			"verbose" => \$verbose,
			"debug" => \$debug,
			"debugall" => \$debugall);

# set debug when debugall
$debug = 1 if $debugall;

# check argument(s)
die "Error: no input file given\n" if $druFileName eq $jruFileName;
print "Open DRU file: " . $druFileName . "\n" if $verbose;

open my $DRUfh,$druFileName or die "Error: Kan DRU file " . $druFileName . " niet openen\n";
my $DRU = ETCS::DRU_Ansaldo::new($DRUfh);

my $db;
$db = ETCS::DB::new($trainid) if defined($trainid);

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
    print "Open XML file: " . $xmlFileName . "\n" if $verbose;
    open $fhXml,">" . $xmlFileName;
    print $fhXml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DRULog>\n" if $xmlFileName ne "";
}

# zet debug uit in ETCS package
ETCS::Debug::debugOff();
ETCS::Debug::debugOn() if $debugall;

# read records
 MAINLOOP: while ( my $message=$DRU->next() ){
     unless ($message->{MessageType} =~ /^SKIP/) {
	 
	 unless (($message->{MessageType} eq "JRUMessage") &&
		 ($message->{Header}->{Fields}->[0]->{Decimal} == 14)) {  # STM Information even niet
	     XMLout($message, NoAttr=>0,OutputFile=>$fhXml,RootName=>$message->{MessageType})
		 if $xmlFileName ne "";
	     
	     # process message to CVS
	     $CsvFile->process($message) if $csvFileName ne "";
	     
	     $db->store($message) if defined($db);
	 }
     }
 }

print $fhXml "</DRULog>\n" if $xmlFileName ne "";
close $fhXml if $xmlFileName ne "";

close $dbgFile if $debug;

$CsvFile->close() if $csvFileName ne "";

close $DRUfh;

print "Klaar\n";
