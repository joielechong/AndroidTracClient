#! /usr/bin/perl -w

use strict;
use DBI;
use Image::ExifTool;
use Encode;
use Data::Dumper;

my $PREFIX="/data/Music/";

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT filename FROM mp3 WHERE artist IS NULL");
my $sth2 = $dbh->prepare("UPDATE mp3 set artist=?,song=?,album=?,track=?,year=?,genre=?,comment=?,duur=?,filesize=? WHERE filename=?");

my $exiftool = new Image::ExifTool;

$sth1->execute();

while (my @row=$sth1->fetchrow_array()) {
#    my $file = decode('utf8',$row[0]);
    my $file = encode('us-ascii',decode('utf8',$row[0]));
    my $info = $exiftool->ImageInfo($PREFIX.$file);
#    print Dumper($info);
    foreach my $key (keys %$info) {
	$$info{$key} = undef if $$info{$key} eq '';
    }
    my ($album,$title,$year,$filesize,$track,$duration,$artist,$genre,$comment);
    if (defined($$info{'Error'})) {
	print Dumper($info);
	sleep 1;
	next;
    }
    if ($$info{'FileType'} eq 'MP3') {
	$album = $$info{'Album'};
	$title = $$info{'Title'};
	$year  = $$info{'Year'};
	$filesize = $$info{'FileSize'};
	$track = $$info{'Track'};
	$duration = $$info{'Duration'};
	$artist = $$info{'Artist'};
	$genre  = $$info{'Genre'};
	$comment = $$info{'Comment'};
    } elsif ($$info{'FileType'} eq 'WMA') {
	$title = $$info{'Title'};
	$track = $$info{'TrackNumber'};
	$artist = $$info{'Author'};
	$duration = $$info{'PlayDuration'};
	$filesize = $$info{'FileSize'};
	$genre  = $$info{'Genre'};
	$album = $$info{'AlbumTitle'};
	$year = $$info{'CreationDate'};
	$year = substr($year,0,4) if defined($year);
    } else {
	next;
    }
    $sth2->execute($artist,$title,$album,$track,$year,$genre,$comment,$duration,$filesize,$file);
}
