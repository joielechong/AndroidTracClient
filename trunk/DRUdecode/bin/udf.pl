#! /usr/bin/perl -w

use lib "L:/BB21-CN/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;



ETCS::Debug::debugOn();
    
#
# Main program
#

while (my $file = shift) {
    
    if ($file =~ /(.*).[uU][dD][fF]$/) {
	$file = $1;
    }

    open FILE,"<$file.udf" or die "kan file $file.udf  niet openen\n";
    open DB,">$file.dbg";
    open my $fhxml,">$file.xml";
    print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<UDFLog>\n";
    
    while (<FILE>) {
	if (/^.DATAS / || /^.DATAL /) {
	    chomp;
	    my $bal = ETCS::Balise->new();
	    my $hex = substr($_,6);
	    print $hex,"\n";
	    my $bits = unpack("B*",pack("H*",join("",split(" ",$hex))));
	    my $bitlen = $bal->setMessage($bits);
	    print DB Dumper($bal);
	    XMLout($bal, NoAttr=>0,OutputFile=>$fhxml,RootName=>$bal->{MessageType});        
	}
    }
    print $fhxml "</UDFLog>\n";
    close $fhxml;
    close FILE;
    close DB;
}
