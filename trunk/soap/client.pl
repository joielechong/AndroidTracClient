#! /usr/local/bin/perl -w
  
  use SOAP::Lite;

  my $som = SOAP::Lite                                             
    -> uri('http://van-loon.xs4all.nl/Demo')                                             
    -> proxy('http://van-loon.xs4all.nl/services/server.cgi')
    -> echo('test');
die $som->fault->{ faultstring } if ($som->fault);
  print $som->result;
