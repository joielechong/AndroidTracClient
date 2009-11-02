#! /usr/bin/perl -w

use lib "/home/mfvl/lib/perl/";
#use lib "/mnt/xs4all/";
use strict;
use POSIX qw /mktime strftime/;

use FondsenDB;
use DataSource;
use XML::Simple;
use Data::Dumper;

my $home=$ENV{'HOME'};
$home = "" unless defined $home;
my $dllist = XMLin("$home/etc/dl.xml");
my %dllist = %{$dllist->{source}};
print Dumper(\%dllist);
exit(1);

my $fdbh = new FondsenDB;
#$fdbh->{dryrun} = 1;

DataSource::opencsvlog("/web/www/dailycsvlog.txt");
open F, "<".$ENV{HOME}."/etc/dl.list" or die "Kan dl.list niet openen\n";
while (<F>) {
	next if /^#/;
    my ($file,$url) = split("=",$_,2);
    print $file,"\n";
	my ($method,$num) = split('\056',$file);
	my $ds = DataSource->new($method);
	
	$ds->fetchURL($url);
	$ds->process($fdbh);
}

$fdbh->exportKoersen();
DataSource::closecsvlog();
