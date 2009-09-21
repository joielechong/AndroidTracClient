#! /usr/bin/perl -w

use strict;

use Data::Dumper;
use DBI;
use Archive::Zip qw(:CONSTANTS);
use XML::Compile::Util;
use XML::Compile::Schema;

my $schema = XML::Compile::Schema->new;
$schema->importDefinitions('ogckml22.xsd');
my $doc =XML::LibXML::Document->new('1.0','UTF-8');

my $kml = XML::LibXML::Element->new('kml');
$kml->setNamespace('http://www.opengis.net/kml/2.2');

my $document=$kml->addNewChild(undef,'Document');
$document->setAttribute('id','light');
my $folder = $document->addNewChild(undef,'Folder');
$folder->appendTextChild('name','Flitspalen');

my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $sth1 = $dbh->prepare("SELECT p.id,lon AS longtitude,lat AS latitude,commentaar,p.naam AS name,richting,bidirectioneel,p.land,insert_date,update_date,snelheid,type,inmio FROM pois.posten AS p Inner Join pois.inputfiles AS if ON p.file = if.naam WHERE rel_id IS NULL  AND updated =  'true' AND if.naam NOT LIKE  '%goedkop%'");

$sth1->execute();

while (my ($id,$longitude,$latitude,$commentaar,$name,$richting,$bidirectioneel,$land,$insert_date,$update_date,$snelheid,$type,$inmio)= $sth1->fetchrow_array()) {
		my $placemark = $folder->addNewChild(undef,'Placemark');
		my $description = "<pre>ID: $id\nNaam: $name\nOmschr: $commentaar\n";
		$description .= "Snelh: $snelheid\n" if defined $snelheid;
		$description .= "Type: $type\n" if defined $type;
		$description .= "Richting: $richting, $bidirectioneel\n" if defined $richting;
		$placemark->addNewChild(undef,'Orientation')->appendTextChild('heading',($bidirectioneel?-$richting:$richting)) if defined $richting;
		$description .= "Land: $land\n" if defined $land;
		$description .= "inmio: $inmio\n" if defined $inmio;
		$description .= "</pre>\n";
		$placemark->addNewChild(undef,'Point')->appendTextChild('coordinates',"$longitude,$latitude");
		$placemark->appendTextChild('description',$description);
}

$doc->setDocumentElement($kml);

my $zip = Archive::Zip->new();
my $sm = $zip->addString($doc->toString,'flits.kml');
$sm->desiredCompressionMethod(COMPRESSION_DEFLATED);
$sm->desiredCompressionLevel(9);
$zip->writeToFileNamed('flits.kmz');
