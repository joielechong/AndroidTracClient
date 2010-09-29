#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;
use File::Basename;


#
# Main program
#

sub CSVout {
    my $message = shift;
    my $fhcsv = shift;
    my $msg;
    
    if (defined ($message->{MessageToRBC}->{Packets})) {
        $msg = $message->{MessageToRBC}->{Packets};
        foreach my $p (@$msg) {
            if ($p->{Fields}->[0]->{Decimal} == 0) {
                if (tell($fhcsv) == 0) {
                   my @hfields;
                   $hfields[0] = "Tijd";
                   for (my $i=0;$i<14;$i++) {
                       $hfields[$i+1] = $p->{Fields}->[$i]->{Field};
                       $hfields[$i+1] = "" unless defined($hfields[$i+1]);
                   }   
                   print $fhcsv join(";",@hfields),"\n";
                }
                my @fields;
                $fields[0] = $message->{Tijd};
                for (my $i=0;$i<14;$i++) {
                    $fields[$i+1] = $p->{Fields}->[$i]->{Text};
                    $fields[$i+1] = $p->{Fields}->[$i]->{Decimal} unless defined($fields[$i+1]);
                    $fields[$i+1] = "" unless defined($fields[$i+1]);
                }
                print $fhcsv join(";",@fields),"\n";
            }
        }
    } elsif (defined($message->{Balise}->{Header})) {
        $msg = $message->{Balise}->{Header};
        my @fields;
        $fields[0] = $message->{Tijd};
        for (my $i=0;$i<14;$i++) {
            $fields[$i+1] = "";
        }
        for (my $i=0;$i<10;$i++) {
            $fields[$i+15] = $msg->{Fields}->[$i]->{Decimal};
            $fields[$i+15] = $msg->{Fields}->[$i]->{Text} unless defined($fields[$i+15]);
            $fields[$i+15] = "" unless defined($fields[$i+15]);
        }
        print $fhcsv join(";",@fields),"\n";
        
    }
}

my $filename = shift;
my $file = basename($filename,('.txt'));



open FILE,"<$filename" or die "kan canape file $filename niet openen\n";
open DB,">$file.dbg";
open my $fhxml,">$file.xml";
print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CanapeLog>\n";
open my $fhcsv,">$file.csv";

ETCS::Debug::debugOff();

while (<FILE>) {
    my $message = ETCS::Canape::processCanapeLine($_);
    if (defined($message)) {
        print DB Dumper($message);
        XMLout($message, NoAttr=>0,OutputFile=>$fhxml,RootName=>$message->{MessageType});
        CSVout($message,$fhcsv);
    }
}

close $fhcsv;
print $fhxml "</CanapeLog>\n";
close $fhxml;
close FILE;
close DB;
