#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;


#
# Main program
#

sub CSVout {
    my $message = shift;
    my $fhcsv = shift;
    
    if (defined ($message->{MessageToRBC}->{Packets})) {
        my $msg = $message->{MessageToRBC}->{Packets};
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
    }
}

open FILE,"<knaap2.txt" or die "kan canape file niet openen\n";
open DB,">knaap2.dbg";
open my $fhxml,">knaap2.xml";
print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CanapeLog>\n";
open my $fhcsv,">knaap2.csv";

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
