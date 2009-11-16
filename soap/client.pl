#! /usr/local/bin/perl -w
  
use SOAP::Lite;
use Data::Dumper;

#SOAP::Lite->import(+trace => all );

my $stock = shift; 

my $som = SOAP::Lite-> service('http://van-loon.xs4all.nl/services/Demo.wsdl');

#    -> proxy('http://van-loon.xs4all.nl/services/server.cgi')
#my $som=$srv-> stock($stock);
print Dumper $som->stocks();
print Dumper $som->stock($stock);
#die $som->fault->{ faultstring } if ($som->fault);
#print Dumper $som->result;
