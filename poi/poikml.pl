#! /usr/bin/perl -w

use strict;

use Data::Dumper;
use DBI;
use Archive::Zip qw(:CONSTANTS);
use XML::LibXML;

my $doc =XML::LibXML::Document->new('1.0','UTF-8');

my $kml = XML::LibXML::Element->new('kml');
$kml->setNamespace('http://www.opengis.net/kml/2.2');

my $document=$kml->addNewChild(undef,'Document');
$document->setAttribute('id','light');
my $folder = $document->addNewChild(undef,'Folder');
$folder->appendTextChild('name','Flitspalen');

my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $sth1 = $dbh->prepare("SELECT p.id,lon AS longtitude,lat AS latitude,commentaar,p.naam AS name,richting,bidirectioneel,p.land,insert_date,update_date,snelheid,type,inmio,p.file  FROM pois.posten AS p Inner Join pois.inputfiles AS if ON p.file = if.naam WHERE rel_id IS NULL  AND updated =  'true' AND if.naam NOT LIKE  '%goedkop%'");

$sth1->execute();

while (my ($id,$longitude,$latitude,$commentaar,$name,$richting,$bidirectioneel,$land,$insert_date,$update_date,$snelheid,$type,$inmio,$file)= $sth1->fetchrow_array()) {
		my $placemark = $folder->addNewChild(undef,'Placemark');
		my $description = "<pre>ID: $id\nNaam: $name\nOmschr: $commentaar\nFile: $file\n";
		$description .= "Snelh: $snelheid\n" if defined $snelheid;
		$description .= "Type: $type\n" if defined $type;
		if (defined($richting)) {
			$description .= "Richting: $richting, $bidirectioneel\n";
			$placemark->addNewChild(undef,'Orientation')->appendTextChild('heading',($bidirectioneel?-$richting:$richting));
		}
		if (defined($land)){
			$description .= "Land: $land\n" ;
		} else {
			$description .= "</pre>Land<form action='http://van-loon.xs4all.nl/cgi-bin/updpoi.pl'><input type='hidden' name='id' value='$id'><input name='Land' length=3><input type='submit'></form><pre>\n";
		}
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

#my $xmlschema = XML::LibXML::Schema->new(location=>'ogckml22.xsd');
#eval {$xmlschema->validate($doc); };
