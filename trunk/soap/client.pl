#! /usr/local/bin/perl -w
  
use SOAP::Lite;
use Data::Dumper;

my $stock = shift; 

my $som = SOAP::Lite                                             
    -> uri('http://van-loon.xs4all.nl/Demo')                                             
    -> proxy('http://van-loon.xs4all.nl/services/server.cgi')
    -> stock($stock);
die $som->fault->{ faultstring } if ($som->fault);
print Dumper $som->result;
