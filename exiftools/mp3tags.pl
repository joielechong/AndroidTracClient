#! /usr/bin/perl -w

use strict;
use DBI;
use Image::ExifTool;
use MP3::Tag;
use Encode;
use Data::Dumper;

my $PREFIX="/data/Music/";

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl") or die "cannot open database\n";
my $sth0 = $dbh->prepare("SELECT mp3.filename FROM mp3, (SELECT filename FROM mp3 EXCEPT SELECT filename FROM filetemp) AS xxx WHERE mp3.filename=xxx.filename;");
my $sth1 = $dbh->prepare("SELECT filename FROM mp3 WHERE artist IS NULL");
my $sth2 = $dbh->prepare("UPDATE mp3 set artist=?,song=?,album=?,track=?,year=?,genre=?,comment=?,duur=?,filesize=? WHERE filename=?");
my $sth3 = $dbh->prepare("INSERT INTO mp3 (filename) SELECT filename FROM filetemp EXCEPT SELECT filename FROM mp3;");
my $sth4 = $dbh->prepare("SELECT filename,artist,song,album,track,year,genre,comment FROM mp3 WHERE filename ILIKE '%.mp3' AND NOT artist IS NULL AND artist <> '' ORDER BY filename");

chdir("/data/Music");
#print $dbh->{AutoCommit},"\n";
$dbh->{AutoCommit}=0;

print "Check of alle mp3's nog bestaan\n";

$dbh->do("create temporary table filetemp (filename varchar)");

open PIJP,"find . -type f -name \"*.[mM][pP]3\" -o -name \"*.[wW][mM][aA]\"| cut -c3-| " or die "Kan filenamen lijst niet openen\n";

$dbh->do("copy filetemp from stdin;");

while (<PIJP>) {
    my $file = $_;
    my $efile = encode('UTF-8',$file);
    $dbh->pg_putline($efile);
}
$dbh->pg_endcopy();
close PIJP;

my @row;
my @delfiles=();

$sth0->execute();
while (my @row=$sth0->fetchrow_array() ) {
    my $filename=$row[0];
    $filename =~ s/'/''/g;
    push @delfiles,$filename  unless -f $filename;
}
print join(", ",@delfiles),"\n";

if ($#delfiles >= 0) {
    my $cmd="delete from mp3 where filename in ('".join("','",@delfiles)."');";
    $dbh->do($cmd);
}
$dbh->commit();

$dbh->{AutoCommit}=1;

$sth3->execute();

my $exiftool = new Image::ExifTool;

$sth1->execute();

while (my @row=$sth1->fetchrow_array()) {
    my $efile = $row[0];
    my $file = decode('UTF-8',$efile);
#    print "$file\n";
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
    $sth2->execute($artist,$title,$album,$track,$year,$genre,$comment,$duration,$filesize,$efile);
}

$sth4->execute();

while (my @row=$sth4->fetchrow_array()) {
    my ($efile,$artist,$title,$album,$track,$year,$genre,$comment) = @row;
    $artist='' unless defined($artist);
    $title='' unless defined($title);
    $album='' unless defined($album);
    $track='' unless defined($track);
    $year='' unless defined($year);
    $genre='' unless defined($genre);
    $comment='' unless defined($comment);
    $artist =~ s/ +$// ;
    $title =~ s/ +$//;
    $album =~ s/ +$//;
    $track =~ s/ +$//;
    $year =~ s/ +$//;
    $genre =~ s/ +$//;
    $comment =~ s/ +$//;

    my $file = decode('UTF-8',$efile);
#    print "$file\n";
    my $mp3 = MP3::Tag->new($file);
    next unless defined($mp3);
    my ($mp_title, $mp_track, $mp_artist, $mp_album, $mp_comment, $mp_year, $mp_genre) = $mp3->autoinfo();
    my $change=0;
    if ($mp_title ne $title) {
	$mp3->title_set($title);
	$change=1;
    }
    if ($mp_track ne $track) {
	$mp3->track_set($track);
	$change=1;
    }
    if ($mp_artist ne $artist) {
	$mp3->artist_set($artist);
	$change=1;
    }
    if ($mp_album ne $album) {
	$mp3->album_set($album);
	$change=1;
    }
    if ($mp_comment ne $comment) {
	$mp3->comment_set($comment);
    }
    if ($mp_year ne $year) {
	$mp3->year_set($year);
	$change=1;
    }
    if ($mp_genre ne $genre) {
	$mp3->genre_set($genre);
    }
    if ($change) {
	print "$title $mp_title\n";
	print "$track $mp_track\n";
	print "$artist $mp_artist\n";
	print "$album $mp_album\n";
	print "$comment $mp_comment\n";
	print "$year $mp_year\n";
	print "$genre $mp_genre\n";
	print "Changed: $file\n";
	eval {
	    $mp3->update_tags();
	};warn $@ if $@;
    }
    $mp3->close();
}
