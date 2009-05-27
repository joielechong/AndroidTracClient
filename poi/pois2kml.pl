#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use HTTP::Request;
use LWP::UserAgent;
use XML::Simple;
use DBI;

#$XML::Simple::PREFERRED_PARSER = 'XML::Parser';

my $outputKML;

$outputKML->{Folder}->{name}="POIS";
$outputKML->{Folder}->{Folder}->[0]->{name}="Flitspalen";
$outputKML->{Folder}->{Folder}->[0]->{Placemark}=();

my $lon1 = shift;
my $lat1 = shift;

my $lon2 = shift;
my $lat2 = shift;

my $no_rel = shift;

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl","","");
my $points;

if (defined($no_rel)) {
    $points = $dbh->selectall_hashref("SELECT * from pois.posten where lat>=$lat1 and lat<=$lat2 and lon>=$lon1 and lon<=$lon2 and updated and rel_id is null",'id');
} else {
    $points = $dbh->selectall_hashref("SELECT * from pois.posten where lat>=$lat1 and lat<=$lat2 and lon>=$lon1 and lon<=$lon2",'id');
}

my $count = 0;

foreach (keys(%$points)){
    my $rem = $points->{$_}->{commentaar};
    my $lat = $points->{$_}->{lat};
    my $file = $points->{$_}->{file};
    my $lon = $points->{$_}->{lon};
    my $id = $points->{$_}->{id};
    my $rel_id = $points->{$_}->{rel_id};

    $outputKML->{Folder}->{Folder}->[0]->{Placemark}->[$count]->{Point}->{coordinates}="$lon,$lat,0";
    $outputKML->{Folder}->{Folder}->[0]->{Placemark}->[$count]->{name}="$id: $rem, $file, $rel_id";
    $count++;
}

my $xml = XMLout($outputKML, NoAttr=>1,RootName=>'kml',XMLDecl=>"<?xml version='1.0' encoding='UTF-8' ?>");
$xml =~ s|\<kml\>|<kml xmlns=\"http://earth.google.com/kml/2.0\">|;

open KML, ">pois.kml" or die "Kan XML file niet creeeren\n";
#print $xml;
print KML $xml;
close KML;
