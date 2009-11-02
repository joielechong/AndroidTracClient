#! /usr/local/bin/perl -w

  use SOAP::Transport::HTTP;

  SOAP::Transport::HTTP::CGI   
    -> dispatch_to('/web/perlclasses/soap/','Demo')
    -> handle;

