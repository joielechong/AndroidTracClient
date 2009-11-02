#! /usr/local/bin/perl -w
  
  use SOAP::Lite;

  print SOAP::Lite                                             
    -> uri('http://localhost/Demo')                                             
    -> proxy('http://localhost/cgi-bin/hibye.cgi')
    -> bye()                                                    
    -> result;
