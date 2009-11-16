#! /usr/local/bin/perl -w

use strict;

#use lib "/web/perlclasses/soap";
use WSDL::Generator;

my $wsdl_param = {
    'schema_namesp' => 'http://van-loon.xs4all.nl/Demo',
    'services'      => 'Services',
    'service_name'  => 'Demo',
    'target_namesp' => 'http://van-loon.xs4all.nl/Demo',
    'documentation' => 'Test of WSDL::Generator',
    'location'      => 'http://van-loon.xs4all.nl/services/server.cgi'
};
my $wsdl = WSDL::Generator->new($wsdl_param);

my $test=Demo->new();
$test->hi();
$test->bye();
$test->echo("tekst");
$test->stocks();
$test->stock("AHOLD");
$test->fault();
my $result = $wsdl->get('Demo');

print $result;


