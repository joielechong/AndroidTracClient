#! /usr/bin/perl -w

use strict;
use DBI;
use LWP::UserAgent;
use Data::Dumper;

my %vertaal = ('France'=>'F',
	       'Belgium'=>'B',
	       'The Netherlands' => 'NL',
	       'Austria'=>'A',
	       'Spain' => 'E',
	       'UK'=>'GB',
	       'Portugal'=>'P',
	       'Italy'=>'I',
	       'Poland'=>'PL',
	       'Finland'=>'SF',
	       'Germany'=>'D',
	       'Denmark'=>'DK',
	       'Norway' => 'N',
	       'Luxembourg'=>'L',
	       'Switzerland'=>'CH',
	       'Sweden'=>'S',
	       'Czech Republic'=>'CZ',
    );

my $keyfile='/home/mfvl/download/google_maps_key.txt';
my $ua = LWP::UserAgent->new();

my $google_key = undef;
my $table="pois.posten";

my $reverseURL = "http://maps.google.com/maps/geo?oe=utf8&sensor=false&output=csv";

open KEY, "<$keyfile" or die "Kan %keyfile niet openen: $@\n";
my $key = <KEY>;
close KEY;
chomp $key;

$google_key = "key=$key";
$reverseURL .= "&$google_key";

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl");

my $sth1 = $dbh->prepare("SELECT id,lat,lon FROM  pois.posten where land IS NULL and rel_id IS NULL and updated ORDER BY id");
my $sth2 = $dbh->prepare("UPDATE pois.posten set land=? where id=?");
$sth1->execute();

while (my @row=$sth1->fetchrow_array()) {
    my $id=$row[0];
    my $lat=$row[1];
    my $lon=$row[2];

    my $locurl=$reverseURL."&q=$lat,$lon";
    my $res = $ua->get($locurl);
    my ($code,$acc,$locatie) = split(',',$res->content,3);
    if ($code != 200) {
	$locatie="Fout. code = $code";
    } else {
	$locatie =~ s/^\"//;
	$locatie =~ s/\"$//;
	$locatie =~ s/^.*,//;
	$locatie =~ s/^ +//;
	$locatie =~ s/ +$//;
	$locatie =~ s/^\d+ //;
	
	my $lc = $vertaal{$locatie};
	die "Land $locatie bestaat nog niet, $id, $lon,$lat\n".$res->content."\n" unless defined $lc;
	print "$id $lon $lat $locatie $lc\n";
	$sth2->execute($lc,$id);
    }
    my $sleep=int(0.9+exp(rand(3.5)));
    print "sleep = $sleep\n";
    sleep $sleep;
}
