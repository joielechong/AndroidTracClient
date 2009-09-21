#! /usr/bin/perl -w

use strict;

use Data::Dumper;
use DBI;
use Archive::Zip;
use XML::Simple;

my $kml;

$kml->{kml}->{xmlns}="http://earth.google.com/kml/2.1";
$kml->{kml}->{Document}->{Folder}->[0]->{name}="Flitspalen";



my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $sth1 = $dbh->prepare("SELECT p.id,lon AS longtitude,lat AS latitude,commentaar,p.naam AS name,richting,bidirectioneel,p.land,insert_date,update_date,snelheid,type,inmio FROM pois.posten AS p Inner Join pois.inputfiles AS if ON p.file = if.naam WHERE rel_id IS NULL  AND updated =  'true' AND if.naam NOT LIKE  '%goedkop%'");

$sth1->execute();
my $cnt=0;

while (my ($id,$longitude,$latitude,$commentaar,$name,$richting,$bidirectioneel,$land,$insert_date,$updatedate,$snelheid,$type,$inmio)= $sth1->fetchrow_array()) {
    $kml->{kml}->{Document}->{Folder}->[0]->{Placemark}->[$cnt]->{Point}->[0]->{coordinates}="$longitude,$latitude";
    $kml->{kml}->{Document}->{Folder}->[0]->{Placemark}->[$cnt]->[0]->{description}="$id $commentaar $snelheid $type";
    $kml->{kml}->{Document}->{Folder}->[0]->{Placemark}->[$cnt]->{LookAt}->{[0]->longitude}=$longitude;
    $kml->{kml}->{Document}->{Folder}->[0]->{Placemark}->[$cnt]->{LookAt}->[0]->{latitude}=$latitude;
    $kml->{kml}->{Document}->{Folder}->[0]->{Placemark}->[$cnt]->{LookAt}->[0]->{name}=$name;
    $cnt++;
}

my $xml = XMLout($kml,RootName=>'kml');
print $xml;
