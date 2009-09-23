#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;
use CGI;
#use DBD::ADO::Const();

my %vertaal  = ( 0x6b7b => ['snelheid',undef],
				 0x6b7c => ['snelheid',30],
				 0x6b7d => ['snelheid',40],
				 0x6b7e => ['snelheid',50],
				 0x6b7f => ['snelheid',60],
				 0x6b80 => ['snelheid',70],
				 0x6b81 => ['snelheid',80],
				 0x6b82 => ['snelheid',90],
				 0x6b83 => ['snelheid',100],
   				 0x6b86 => ['snelheid',130],
				 0x6b8a => ['mobiel',undef],
				 0x6b89 => ['roodlicht',undef],
				 0x6b6e => ['roodlicht',30],
				 0x6b6f => ['roodlicht',40],
				 0x6b70 => ['roodlicht',50],
				 0x6b71 => ['roodlicht',60],
				 0x6b72 => ['roodlicht',70],
   				 0x6b74 => ['roodlicht',90],
				 0x6b75 => ['roodlicht',100],
				 0x6b76 => ['roodlicht',110],
				 0x6b77 => ['roodlicht',120],
				 0x6b78 => ['roodlicht',130],
				 0x014a => ['traject',undef],
				 0x6b8f => ['traject',30],
				 0x6b90 => ['traject',40],
				 0x6b91 => ['traject',50],
				 0x6b92 => ['traject',60],
				 0x6b93 => ['traject',70],
				 0x6b94 => ['traject',80],
				 0x6b95 => ['traject',90],
				 0x6b96 => ['traject',100],
				 0x6b97 => ['traject',110],
				 0x6b98 => ['traject',120],
				 0x6b99 => ['traject',130]
				);

my $q = new CGI;
my $file = $q->param('upload');
if (defined($file)) {

  print $q->header(-type=>'text/plain',-expires=>'-1h');
#  my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
#      $atime,$mtime,$ctime,$blksize,$blocks) = stat($file);
  my $size=33000;
           
  my $dbh = DBI->connect("dbi:Pg:dbname=mfvl host=van-loon.xs4all.nl");
  my $sth1=$dbh->prepare("INSERT INTO pois.poisupdate (id,lat,lon,ruwedata,veld1,veld2,veld3,veld4,type,snelheid) VALUES (?,?,?,?,?,?,?,?,?,?)");
  my $sth2=$dbh->prepare("DELETE FROM pois.poisupdate");
  my $sth3=$dbh->prepare("INSERT INTO pois.posten (lat,lon,file,naam) SELECT DISTINCT lat,lon,'POISUpdate.upd','PU-'||lat||'-'||lon from \"vergelijk poisupdate\" where id is NULL");
  my $sth4=$dbh->prepare("INSERT INTO pois.posten (lat,lon,file,naam) VALUES (?,?,'POISUpdate.upd','PU-'||?||'-'||?)");

  $sth2->execute();

  my $fh = $q->upload('upload')->handle;
#  open IN,"<$file" or die "Kan file $file niet openen\n";
  binmode $fh;

  my $buffer;
  my $bytes = read($fh,$buffer,$size);

  for (my $pos = 0;$pos <$bytes;$pos += 0120) {
    my $entry = substr($buffer,$pos,0120);
    my $lat = unpack("l",substr($entry,0,4));
    my $lon = unpack("l",substr($entry,4,4));
    my $field1 = unpack("l",substr($entry,8,4));
    my $field2 = unpack("l",substr($entry,12,4));
    my $field3 = unpack("l",substr($entry,16,4));
    my $field4 = unpack("l",substr($entry,20,4));
    my $hex = unpack("H*",substr($entry,8));
    print $pos/0120," ",$lat/1000000.0," ",$lon/1000000.0," $field1 $field2 $field3 $field4";
    my $type = undef;
    my $snelheid = undef;
	
    if ($field1 == 0x6b7b && $field2 == 0 && $field3 == 0) {
	$type='deleted';
	print " $type";
    } elsif (defined($vertaal{$field1})) {
	$type = $vertaal{$field1}->[0];
	$snelheid = $vertaal{$field1}->[1];
	print "$type $snelheid";
    }
    print " $hex\n";
	
    $sth1->execute(($pos/0120)+1,$lat/1000000.0,$lon/1000000.0,$hex,$field1,$field2,$field3,$field4,$type,$snelheid);
    $sth4->execute($lat/1000000,$lon/1000000,$lat/1000000,$lon/1000000);
  }
} else {
  print $q->header;
  print $q->start_html("POIUpdate upload");
  print $q->start_multipart_form();
  print $q->filefield(-name=>'upload',-default=>'POIUpdate.upd',-size=>50);
  print $q->submit(-name=>'Verzenden');
  print $q->endform();
  print $q->end_html();
}
