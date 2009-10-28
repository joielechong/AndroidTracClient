#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use Data::HexDump;
use DBI;
use Encode;

sub printData {
    my $data=shift;

    if (defined($data)) {
	print HexDump($data);
    } else {
	print "* * * *Undefined* * * *\n";
    }

}

#my $dbhPg = DBI->connect("dbi:Pg:dbname=mfvl","","");

#my $dbh=DBI->connect("dbi:SQLite:dbname=iGO-20091016.db","","");
my $dbh=DBI->connect("dbi:SQLite:dbname=iGO.db","","");
my $sth1 = $dbh->prepare("SELECT * from WPOIGLOUP_V1");
my $sth2 = $dbh->prepare("SELECT * from WPOILABEL_V1");
my $sth3 = $dbh->prepare("SELECT * from WPOITREE_V1");
my $sth4 = $dbh->prepare("SELECT * from TRACKS_V1");
my $sth5 = $dbh->prepare("SELECT * from PIN_V1");


my @pins;
my $i=0;
$sth5->execute();
print "PIN_V1\n";
while (my $row=$sth5->fetchrow_hashref()) {
    print $row->{ID},", ",$row->{F1},", ",$row->{F2},"\n";
    $pins[$i]->{id} = $row->{ID};
    $pins[$i]->{f1} = $row->{F1};
    $pins[$i]->{f2} = $row->{F2};
    my $hdr=$row->{HDR};
    my $data=$row->{DATA};
#    print "HDR=\n";
#    printData($hdr);
    $pins[$i]->{hdr} = $hdr;
#    print "DATA=\n";
#    printData($data);
    my @fields = unpack("VVVVVa*",$data);
#    print Dumper(\@fields);
    $pins[$i]->{lon} = $fields[1]*1.0/0x800000;
    $pins[$i]->{lat} = $fields[2]*1.0/0x800000;
    $pins[$i]->{veld1} = $fields[0];
    $pins[$i]->{veld4} = $fields[3];
    $pins[$i]->{count} = $fields[4];
    $pins[$i]->{string} = $fields[5];
    $pins[$i]->{stringle} = decode("UTF-16LE",$fields[5]);
#    $pins[$i]->{lon}=unpack("l",substr($data,0x04,4))*1.0/0x800000;
#    $pins[$i]->{lat}=unpack("l",substr($data,0x08,4))*1.0/0x800000;
#    $pins[$i]->{veld1}=unpack("l",substr($data,0x0,4));
#    $pins[$i]->{veld4}=unpack("l",substr($data,0xc,4));
#    my $l=ord(substr($data,0x10,4));
#    $pins[$i]->{naamu8} = substr($data,0x14,2*$l);
#    $pins[$i]->{naam} = encode('ascii',$pins[$i]->{naamu8});
    $i++;
}
print Dumper(\@pins);

