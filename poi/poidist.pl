#! /usr/bin/perl -w

use strict;
use Geo::Distance;
use Data::Dumper;
use DBI;

my $afstand=50;

#my $dbh = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=radar.mdb");
my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $files = $dbh->selectall_arrayref("SELECT * from pois.inputfiles");
my $nrfiles = $#$files;
my %snelheid;
my %type;
foreach (@$files) {
    $snelheid{$_->[1]} = $_->[2];
    $type{$_->[1]} = $_->[3];
}

my $posten = $dbh->selectall_arrayref("SELECT lat,lon,posten.id,file,commentaar from pois.posten where rel_id is null  order by id;");

$dbh->do("TRUNCATE pois.close50");

my $maxrow=$#$posten;
my $sth3 = $dbh->prepare("INSERT INTO pois.close50 (id1, lat1, lon1, id2, lat2, lon2, distance, file1, rem1, file2, rem2, t1, t2) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");

print STDERR "aantal rows = $maxrow\n";

my $geo=new Geo::Distance;

my $wcount=0;
for my $i (0..$maxrow) {
  my ($lat1,$lon1,$id1,$file1,$rem1)=@{$$posten[$i]};
  my $type1=$type{$file1};
  next unless $type1;
  print STDERR "$i $wcount $id1";
  my $locations = $geo->closest(dbh=>$dbh,table=>'pois.disthelp1',lon=>$lon1,lat=>$lat1,unit=>'meter',distance=>$afstand);
  print STDERR "\r";
  if ($#$locations > 0) {
#      print Dumper([$lon1,$lat1,$locations]);
      my $count = ($#$locations)+1;
      my @lons;
      my @lats;
      my @dist;
      for (my $idx=0;$idx < $count;$idx++) {
	  $lons[$idx] = $locations->[$idx]->{lon};
	  $lats[$idx] = $locations->[$idx]->{lat};
	  $dist[$idx] = $locations->[$idx]->{distance};
      }
      my $sqlcmd = "SELECT p.id,dist.dist[gs.ser] as distance,p.lon,p.lat,p.file,p.commentaar FROM (select ARRAY[".join(",",@lons)."]) as lons(lon),(SELECT ARRAY[".join(",",@lats)."]) as lats(lat),(SELECT ARRAY[".join(",",@dist)."]) as dist(dist),generate_series(1,$count) as gs(ser), pois.posten as p WHERE p.lon=lons.lon[gs.ser] and p.lat=lats.lat[gs.ser] and p.rel_id is null and p.updated and p.id > $id1 and p.file != '$file1';";
#      print "$sqlcmd\n"; 

      my $buren = $dbh->selectall_hashref($sqlcmd,'id');
#      print Dumper($buren);
      foreach (keys(%$buren)){
	  my $rem2 = $buren->{$_}->{commentaar};
	  my $distance = $buren->{$_}->{distance};
	  my $lat2 = $buren->{$_}->{lat};
	  my $file2 = $buren->{$_}->{file};
	  my $lon2 = $buren->{$_}->{lon};
	  my $id2 = $buren->{$_}->{id};
	  my $type2 = $type{$file2};
	  next unless $type2;

	  if ($distance <=$afstand) {
	      $sth3->execute($id1,$lat1,$lon1,$id2,$lat2,$lon2,$distance,$file1,$rem1,$file2,$rem2,$type1,$type2);
	      $wcount++;
	  }
      }
  }
}

print "\n";
$dbh->disconnect();
