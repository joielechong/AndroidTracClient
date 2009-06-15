#! /usr/bin/perl -w

use strict;
use DBI;
use Image::ExifTool;
use Encode;
use Data::Dumper;

my $PREFIX="/data/Music/";

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth0 = $dbh->prepare("SELECT mp3.filename FROM mp3, (SELECT filename FROM mp3 EXCEPT SELECT filename FROM filetemp) AS xxx WHERE mp3.filename=xxx.filename;");
my $sth1 = $dbh->prepare("SELECT filename FROM mp3 WHERE artist IS NULL");
my $sth2 = $dbh->prepare("UPDATE mp3 set artist=?,song=?,album=?,track=?,year=?,genre=?,comment=?,duur=?,filesize=? WHERE filename=?");

chdir("/data/Music");
my $dbh = DBI->connect("DBI:Pg:dbname=httpd") or die "cannot open database\n";

#print $dbh->{AutoCommit},"\n";
$dbh->{AutoCommit}=0;

print "Check of alle mp3's nog bestaan\n";

$dbh->do("create temporary table filetemp (filename varchar)");

open PIJP,"find . -type f -name \"*.[mM][pP]3\" -o -name \"*.[wW][mM][aA]\"| cut -c3-|" or die "Kan filenamen lijst niet openen\n";

$dbh->do("copy filetemp from stdin;");

while (<PIJP>) {
    $dbh->pg_putline($_);
}
$dbh->pg_endcopy();
close PIJP;

my @row;
my @delfiles=();

$sth0->execute();
while (my @row=$sth0->fetchrow_array() ) {
    my $filename=$$row[0];
	$filename =~ s/'/''/g;
    push @delfiles,$filename  unless -f $filename;
}
print join(", ",@delfiles),"\n";

if ($#delfiles >= 0) {
#    my $cmd="delete from albumfoto where fotoid in (select id from fotos where filename in ('".join("','",@delfiles)."'));";
#    $dbh->do($cmd);
    
    my $cmd="delete from fotos where filename in ('".join("','",@delfiles)."');";
    $dbh->do($cmd);
}

$dbh->commit();
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
