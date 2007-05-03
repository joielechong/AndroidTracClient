#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "/home/mfvl/perl/lib";
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;

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
my $filename = "JRU 2007-02-24 07h 21m 28s.jdta";
my $buffer;

my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
       $atime,$mtime,$ctime,$blksize,$blocks)
           = stat($filename);

open FILE,"<$filename" or die "kan JRU file niet openen\n";
binmode FILE;
open DB,">testjru.dbg";
open my $fhxml,">testjru.xml";
print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<JRULog>\n";
open my $fhcsv,">testjru.csv";

read(FILE,$buffer,$size);
my $pos=0;
my $reccnt = 0;

ETCS::Debug::debugOff();

my $x;
$x->{filter}->{mintime}="20070207000000000";
$x->{filter}->{maxtime}="20070207235959999";

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
        $message->setFilter($x->{filter});
        my $msgstat = $message->setMessage(unpack("B*",substr($buffer,$pos,$length)));
        if ($msgstat >= 0) {
            print DB Dumper($message);
            XMLout($message, NoAttr=>0,OutputFile=>$fhxml,RootName=>$message->{MessageType});
            CSVout($message,$fhcsv);
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
close $fhcsv;
print $fhxml "</JRULog>\n";
close $fhxml;
close FILE;
close DB;
