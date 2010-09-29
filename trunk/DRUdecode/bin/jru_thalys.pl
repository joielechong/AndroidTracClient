#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "/home/mfvl/perl/lib";
use lib "../lib";   # gebruik lokale versie in subdirectory lib
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;
use File::Basename;
use ETCS::DB;
use Getopt::Long;

sub resync {
    my ($buffer,$pos,$size) = @_;
    
    my @magic = ("0106","0A0C","0A0A","0909","1108","0B07","020D","0006","1407","0307","0F07");
    my $newpos = $size;
        
    foreach my $gms (@magic) {
        my $gm = pack("H4",$gms);
        my $nextgm = index($$buffer,$gm,$pos);
        print "Eerst volgende $gms = $nextgm\n";
        if ($nextgm > 0) {
            $newpos = $nextgm if $nextgm < $newpos;
        }
    }
    return $newpos;
}

sub CSVout {
    my $message = shift;
    my $fhcsv = shift;
    my @fields;
    
    if (tell($fhcsv) == 0) {
        my @hfields;
        for (my $i=0;$i<21;$i++) {
         $hfields[$i] = $message->{Header}->{Fields}->[$i]->{Field};
         $hfields[$i] = "" unless defined($hfields[$i]);
        }
        print $fhcsv join(";",@hfields),"\n";
    }
    
    for (my $i=0;$i<21;$i++) {
        $fields[$i] = $message->{Header}->{Fields}->[$i]->{Text};
        $fields[$i] = $message->{Header}->{Fields}->[$i]->{Decimal} unless defined($fields[$i]);
        $fields[$i] = "" unless defined($fields[$i]);
    }
    print $fhcsv join(";",@fields),"\n";
}

#
# Main program
#
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
my $result = GetOptions("jru=s" => \$jruFileName,
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
print "Open jRU file: " . $jruFileName . "\n" if $verbose;

my $buffer;

my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
       $atime,$mtime,$ctime,$blksize,$blocks)
           = stat($jruFileName);
           
open FILE,"<$jruFileName" or die "kan JRU file niet openen\n";
binmode FILE;

my $db;
$db = ETCS::DB::new($trainid) if defined($trainid);

# zet het filter
my $x;
if ($filter ne "") {
    my ($mindate, $mintime, $maxdate, $maxtime) = split(/,/, $filter, 4);
    print "Zet filter: " . $mindate . "-" . $mintime . " tot " . $maxdate . "-" . $maxtime . "\n"
	if $verbose;
		$x->{filter}->{mintime}="$mindate$mintime";
		$x->{filter}->{maxtime}="$maxdate$maxtime";
}


# open output file
my $fhcsv = DruCsvFile::new($csvFileName) if $csvFileName ne "";

# open debug file
open my $dbgFile, ">TEST.TXT" if $debug;

# open xml file
my $fhxml;
if ($xmlFileName ne "") {
    print "Open XML file: " . $xmlFileName . "\n" if $verbose;
    open $fhxml,">" . $xmlFileName;
    print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DRULog>\n" if $xmlFileName ne "";
}

# zet debug uit in ETCS package
ETCS::Debug::debugOff();
ETCS::Debug::debugOn() if $debugall;

read(FILE,$buffer,$size);
my $pos=0;
my $reccnt = 0;

ETCS::Debug::debugOn();
ETCS::JRU::SetAnsaldo();

my $oldlength = 0;
my $oldmessage = undef;
while ($pos < $size) {
    $reccnt++;
    my $nid_message = unpack("C",substr($buffer,$pos,1));
    my $length = unpack("n",substr($buffer,$pos+1,2))>>6;
    if (($length < 27) || ($nid_message > 25) || (($nid_message==1) && ($length != 27))) {
        
        print "\nIllegale frame. NID_MESSAGE = $nid_message. Lengte = $length\n";
        print "Huidige record = $reccnt\n";
        print "Positie in file = $pos\n";
        print "Vorige frame:\n",Dumper($oldmessage);
        
        my $newpos = resync(\$buffer,$pos-$oldlength+1,$size);
        
        print "Hersynchronisatie naar $newpos, oude pos = $pos, reccnt = $reccnt\n";
        $pos = $newpos;
        $oldlength = 0;
    } else {
        my $message = ETCS::JRU::new();
        $message->setFilter($x->{filter}) if $filter ne "";
        my $msgstat = $message->setMessage(unpack("B*",substr($buffer,$pos,$length)));
        if ($msgstat >= 0) {
            print $dbgFile Dumper($message) if $debug;
            XMLout($message, NoAttr=>0,OutputFile=>$fhxml,RootName=>$message->{MessageType}) if ($xmlFileName ne "");
            CSVout($message,$fhcsv) if $csvFileName ne "";
	 			    $db->store($message) if defined($db);
            $oldlength=$length;
            $oldmessage = $message;
            $pos += $length;
        } elsif (! $message->filteredOut()) {
            print "\nIllegale frame. NID_MESSAGE = $nid_message. Lengte = $length\n";
            print "Huidige record = $reccnt\n";
            print "Positie in file = $pos\n";
            print "Vorige frame:\n",Dumper($oldmessage) if defined($oldmessage);
            print "Huidig frame:\n",Dumper($message);
            
            my $newpos = resync(\$buffer,$pos+1,$size);
            
            print "Hersynchronisatie naar $newpos, oude pos = $pos, reccnt = $reccnt\n";
            $pos = $newpos;
            $oldlength = 0;
            $oldmessage = undef;
        } else {
            $oldlength=$length;
            $oldmessage = $message;
            $pos += $length;
        }
	if (defined($message->{filter}) && ($message->{filter}->{msgtime} < $message->{filter}->{mintime})) {
	    last;
	};
    }
}
close $fhcsv if $csvFileName ne "";;
if ($xmlFileName ne "") {
	print $fhxml "</JRULog>\n";
	close $fhxml;
}
close FILE;
close $dbgFile if $debug;
