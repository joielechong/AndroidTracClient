#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use Data::HexDump;
use Encode;
use DBI;

sub printData {
    my $data=shift;

    if (defined($data)) {
	print HexDump($data);
    } else {
	print "* * * *Undefined* * * *\n";
    }

}

#my $dbhPg = DBI->connect("dbi:Pg:dbname=mfvl","","");

#my $dbh=DBI->connect("dbi:SQLite:dbname=iGo-orig.db","","");
my $dbh=DBI->connect("dbi:SQLite:dbname=iGO.db","","");
my $sth1 = $dbh->prepare("SELECT * from WPOIGLOUP_V1");
my $sth2 = $dbh->prepare("SELECT * from WPOILABEL_V1");
my $sth3 = $dbh->prepare("SELECT * from WPOITREE_V1");
my $sth4 = $dbh->prepare("SELECT * from TRACKS_V1");
my $sth5 = $dbh->prepare("SELECT * from PIN_V1");
my @groups;
my $i=0;

$sth1->execute();
print "WPOIGLOUP_V1\n";
while (my $row=$sth1->fetchrow_hashref()) {
    print $row->{ID},", ",$row->{F1},", ",$row->{F2},"\n";
    my $hdr=$row->{HDR};
    my $data=$row->{DATA};
    print "HDR=\n";
    printData($hdr);
    print "DATA=\n";
    printData($data);
    my @fields = unpack("VCV7C4",$data);
    push @fields,substr($data,0x25,$fields[12]*2);
    push @fields,decode("UTF-16LE",$fields[13]);
    print Dumper(\@fields);
}

my @labels;
$i = 0;

open CSV,">pois.csv";
print CSV "bytes,f1,f2,f3,f4,f5,time,f7,id,group,f10,f11,f12,f13,f14,lonint,latint,f17,icon,Lengte,lon,lat,Naam\n";
$sth2->execute();
print "WPOILABEL_V1\n";
while (my $row=$sth2->fetchrow_hashref()) {
    print $row->{ID},", ",$row->{F1},", ",$row->{F2},"\n";
    my $hdr=$row->{HDR};
    my $data=$row->{DATA};
    print "HDR=\n";
    printData($hdr);
    print "DATA=\n";
    printData($data);
    my @fields = unpack("lC5Ll3C5l4C",$data);
    push @fields,"\"".substr($data,0x2f,($fields[19]-1)*2)."\"";
    $labels[$i]->{naamle} = decode("UTF-16LE",$fields[20]);
    $labels[$i]->{naam} = $fields[20];
    $labels[$i]->{lon} = $fields[15]*1.0/0x800000;
    $labels[$i]->{lat} = $fields[16]*1.0/0x800000;
    my $temp = pop(@fields);
    push @fields,$labels[$i]->{lon};
    push @fields,$labels[$i]->{lat};
    push @fields,$temp;
    $labels[$i]->{icon} = $fields[18];
    $labels[$i]->{id} = $fields[8];
    $labels[$i]->{group} = $fields[9];
    $labels[$i]->{time} = $fields[6];
    print Dumper(\@fields);
    print Dumper($labels[$i]);
    print CSV join(",",@fields),"\n";
    $i++;
}

#print Dumper(\@labels);
close CSV;
