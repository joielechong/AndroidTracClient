#! /usr/bin/perl -w

use strict;

use Data::Dumper;
use Archive::Zip qw( :ERROR_CODES );
use DBI;
#use DBD::ADO::Const();

#my $directory = "C:/Program Files/POI-Warner MioMap Edition/pois/export/";
#my $directory = "d:/Tijdelijk/Uitvoer/";
my $directory = "uitvoer/";

#my $dbh = DBI->connect("dbi:SQLite2:dbname=radar");
#my $dbh = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=d:/Tijdelijk/radar2000.mdb");
#my $dbh = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=radar.mdb");
my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

#$dbh->trace(2,"dbitrace.log");

my $sth1=$dbh->prepare("SELECT DISTINCT type FROM pois.flitspalen WHERE NOT type IN ('Overig', 'mobiel') ");
my $sth2=$dbh->prepare("SELECT DISTINCT snelheid FROM pois.flitspalen WHERE type=?");
my $sth3=$dbh->prepare("SELECT id,lat,lon,commentaar,richting,bidirectioneel FROM pois.flitspalen WHERE type = ? and snelheid=? and not land in ('CH','A','D') ORDER BY lon,lat");
#my $sth3=$dbh->prepare("SELECT id,lat,lon,commentaar,richting,bidirectioneel FROM pois.flitspalen WHERE type = ? and snelheid=? ORDER BY lon,lat");

my %typconv = ("roodlicht"=>"REDLIGHT",
               "snelheid"=>"GATSO",
               "mobiel"=>"MOBILE",
	       "afstand"=>"DISTANCE",
               "traject"=>"SPECS",
               "trajectstart"=>"SPECS",
               "trajecteind"=>"SPECS");

open NIEUW,"| /usr/bin/todos >".$directory."SCFormat.asc";
#open NIEUW,">SCFormat.asc";

open FILE,">".$directory."flits_overig.asc";
close FILE;

my $wcount=0;
$sth1->execute();
while (my @types = $sth1->fetchrow_array()) {
  print $types[0],"\n";
  $sth2->execute($types[0]);
  while (my @speeds = $sth2->fetchrow_array()) {
    print $types[0]," ",$speeds[0],"\n";
    if ($speeds[0] != 0) {
      open FILE,">".$directory."flits_".$types[0]."_".$speeds[0].".asc";
    }
    else {
      open FILE, ">>".$directory."flits_overig.asc";
    }
    $sth3->execute($types[0],$speeds[0]);
    while (my $entries = $sth3->fetchrow_hashref()) {
      my $typ = $typconv{$types[0]};
      $typ = "GATSO" unless defined($typ);
      unless ($typ eq "DISTANCE") {
        printf FILE "%8.5f, %8.5f, \"%s (%s)\"\n",$$entries{"lon"},$$entries{"lat"},$$entries{"commentaar"},$$entries{"id"};
        printf NIEUW "%8.5f, %8.5f, \"%s:%s@%d\"",$$entries{"lon"},$$entries{"lat"},$typ,$$entries{"id"},$speeds[0];
        if (defined($$entries{richting})) {
          printf NIEUW ",%d,%d",$$entries{richting},1+$$entries{bidirectioneel};
        }
        printf NIEUW "\n";
	$wcount++;
      }
    }
    close FILE;
  }
}
close NIEUW;

$dbh->disconnect();

print "$wcount entries geschreven\n";
