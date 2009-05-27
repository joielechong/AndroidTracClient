#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use HTTP::Request;
use LWP::UserAgent;
use XML::Simple;
use DBI;

#$XML::Simple::PREFERRED_PARSER = 'XML::Parser';

my $baseurl="http://maps.google.com/maps/geo?key=ABQIAAAAhN9BXMs5QuEWNPELVVQK9RSoE5om2NR-mWZGYVWTmUqo7KwJSRRu77NO5gbQQbn3w2uh8WrOsG4P3A&output=xml";

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl","","");
my $sth=$dbh->prepare("SELECT DISTINCT * from miolijst");

my $sthup = $dbh->prepare("UPDATE naw set lon=?,lat=? where straat=? and stad=? and land =?");

$sth->execute();

my $outputKML;

$outputKML->{Folder}->{name}="iGO POI";
$outputKML->{Folder}->{Folder}->[0]->{name}="Niet benoemd";
$outputKML->{Folder}->{Folder}->[1]->{name}="Adressen";
$outputKML->{Folder}->{Folder}->[1]->{Placemark}=();

sub addAddress {
    my ($naam,$adres,$count) = @_;
    
    $adres =~s/ /+/g;
    my $url = "$baseurl&q=$adres";
#    print $url,"\n";
    my $request = HTTP::Request->new(GET => $url);
    
    my $ua = LWP::UserAgent->new;
    $ua->agent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");
    my $response = $ua->request($request);
    
#    print Dumper($ua);
#    print Dumper($response);
    
    my $content=$response->content;
    my $ref = XMLin($content);
    
#    print Dumper($ref);
    print "$count,$naam,$adres\n";
    print $ref->{Response}->{Status}->{code},"\n";
    print $ref->{Response}->{name},"\n";
    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{name}="$naam,".$ref->{Response}->{name};
    if ($ref->{Response}->{Status}->{code} == 200) {
	print $ref->{Response}->{Placemark}->{Point}->{coordinates},"\n";    
	$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Point}->{coordinates}=$ref->{Response}->{Placemark}->{Point}->{coordinates};
    }
    return $ref->{Response}->{Placemark}->{Point}->{coordinates};
}

my $count=0;

while (my $row = $sth->fetchrow_hashref()) {
    my $naam=$row->{naam};
    $naam =~ s/^Fam. //;
    $naam =~ s/^Dhr. //;
    $naam =~ s/^Frau //;
    $naam =~ s/^Mevrouw //;
    $naam =~ s/^Mw. //;

    my $adres = $row->{straat};
    my $mioicon = $row->{mioicon};
    if (!($adres =~ m/^Postbus/) ){
	if (defined($row->{postcode})) {
	    $adres .= ",".$row->{postcode}." ".$row->{stad};
	} else {
	    $adres .= ",".$row->{stad};
	}
	if (defined($row->{provincie})) {
	    $adres .= ",".$row->{provincie};
	}
	$adres .= ",".$row->{land};
	
	if (defined($row->{lon})) {
	    print "$count,$naam,$adres\n";
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{name}="$naam, $adres";
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Point}->{coordinates}=$row->{lon}.",".$row->{lat}.",0";
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{LabelStyle}='';
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{heading}=0;
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{href}='http://my.pdamill.com/~fable/igopoi.bmp';
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{y}=30*$mioicon;
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{w}=30;
	    $outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{h}=30;
	    $count++;
	} else {
	    my $coordinates = addAddress($naam,$adres,$count);
	    if (defined($coordinates)) {
		my ($lon,$lat,$dum)=split(',',$coordinates);
		$sthup->execute($lon,$lat,$row->{straat},$row->{stad},$row->{land});
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{LabelStyle}='';
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{heading}=0;
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{href}='http://my.pdamill.com/~fable/igopoi.bmp';
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{y}=30*$mioicon;
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{w}=30;
		$outputKML->{Folder}->{Folder}->[1]->{Placemark}->[$count]->{Style}->{IconStyle}->{Icon}->{h}=30;
		$count++;    
	    }
	}
    }
}

#print Dumper($outputKML);
my $xml = XMLout($outputKML, NoAttr=>1,RootName=>'kml',XMLDecl=>"<?xml version='1.0' encoding='UTF-8' ?>");
$xml =~ s|\<kml\>|<kml xmlns=\"http://earth.google.com/kml/2.0\">|;
    
open KML, ">iGO.kml" or die "Kan XML file niet creeeren\n";
#print $xml;
print KML $xml;
close KML;
