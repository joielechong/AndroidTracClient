#! /usr/local/bin/perl -w
  
  use SOAP::Lite;

  print SOAP::Lite                                             
    -> uri('http://van-loon.xs4all.nl/Demo')                                             
    -> proxy('http://van-loon.xs4all.nl/services/server.pl')
    -> echo('test')                                                    
    -> result;
