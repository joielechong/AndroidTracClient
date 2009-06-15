#! /usr/bin/perl -w

use strict;

use Image::ExifTool qw(:Public);
use DBI;
use Data::Dumper;

chdir("/data");
my $dbh = DBI->connect("DBI:Pg:dbname=httpd") or die "cannot open database\n";

#print $dbh->{AutoCommit},"\n";
$dbh->{AutoCommit}=0;

print "Check of alle foto's nog bestaan\n";

$dbh->do("create temporary table filetemp (filename varchar)");

open PIJP,"find pictures/photos -type f| /usr/local/bin/iconv -t utf-8 -f iso-8859-1 |" or die "Kan filenamen lijst niet openen\n";

$dbh->do("copy filetemp from stdin;");

while (<PIJP>) {
    next if /Thumbs\.db/;
    next if /-thumb\.jpg/;
    next if /-thump\.jpg/;
    next if /\.shtml/;
    next if /\.[aA][vV][iI]/;
    next if /\.[mM][pP][gG]/;
    next if /\..[Gg][pP]/;
    next if /\.[mM][oO][vV]/;
    next if /\.[wW][aA][vV]/;
#    s/'/''/g;
    $dbh->pg_putline($_);
}
$dbh->pg_endcopy();
close PIJP;

my @row;
my @delfiles=();
my @oids=();

my $rows = $dbh->selectall_arrayref("select fotos.filename,thumbnail from fotos, (select filename from fotos except select filename from filetemp) as xxx where fotos.filename=xxx.filename;");

foreach my $row (@$rows) {
    my $filename=$$row[0];
    my $oid=$$row[1];
	$filename =~ s/'/''/g;
    push @delfiles,$filename  unless -f $filename;
    push @oids,$oid unless (defined($oid) && -f $filename);
}
print join(", ",@delfiles),"\n";

if ($#delfiles >= 0) {
#    my $cmd="delete from albumfoto where fotoid in (select id from fotos where filename in ('".join("','",@delfiles)."'));";
#    $dbh->do($cmd);
    
    my $cmd="delete from fotos where filename in ('".join("','",@delfiles)."');";
    $dbh->do($cmd);
}

if ($#oids >= 0) {
    for my $oid (@oids) {
	if (defined($oid)) {
            my $ret=$dbh->func($oid,'lo_unlink');
            print "Unlink of $oid returned $ret\n";
	}
    }
}
$dbh->commit();

print "Voeg nieuwe foto's toe aan database\n";
my $exiftool = new Image::ExifTool;

my $sth_insert=$dbh->prepare("insert into fotos values (?)");
my $sth_fotonr=$dbh->prepare("update fotos set fotonr=id where filename=?");
my $sth_fillinfo=$dbh->prepare("update fotos set fotonr=?,camera=?,auteur=?,omschrijving=?,datum=?,insidethumb=?,filmnr=?,hitcount=0 where filename=?");

$rows=$dbh->selectall_arrayref("select filename from filetemp except select filename from fotos;");

foreach my $row (@$rows) {
    my $filedb=$$row[0];
    my $file = $filedb;
    $file =~ s/''/'/g;
    $_=`file "$file"`;
    next unless /image data/;
    print "$file $filedb\n";
    my $info = ImageInfo($file);
    my $make = $$info{'Make'};
    my $model = $$info{'Model'};
    $model = $$info{'Camera Model Name'} unless defined($model);
    if ($make && $model) {
	$model = $make." ".$model;
    } elsif ($make) {
	$model = $make;
    }
    my $insidethumb = 0;
    $insidethumb = 1 if $$info{'ThumbnailImage'};
    my $datum = $$info{'DateTimeOriginal'};
    unless ($datum) {
	$datum = $$info{'DateCreated'};
    }
    my $artist = $$info{'Artist'};
    my $description = $$info{'Description'};
	$sth_insert->execute($file);
	$sth_fotonr->execute($file);
#    $dbh->do("insert into fotos values ('$filedb');");
#    $dbh->do("update fotos set fotonr=id where filename='$filedb'");
    my @changes=();
    my $nr;
    my $filmnr;
    
    if ($file =~ m:.*/PIC[T_](\d+)\.[Jj][Pp][Gg]:) {
	$nr=$1;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/P(\d\d\d)0(\d+)\.[Jj][Pp][Gg]:) {
	$nr=$2;
	$filmnr=$1;
	push @changes,"fotonr=$nr" if $nr;
	push @changes,"filmnr=$filmnr" if $filmnr;
    }
    if ($file =~ m:.*/D[SV]C[NF]?(\d+)\.[Jj][Pp][Gg]:) {
	$nr=$1;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/(\d+)[_]+(\d+)A?\.[Jj][Pp][Gg]:) {
	$nr=$2;
	$filmnr=$1;
	push @changes,"filmnr=$filmnr" if $filmnr;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/IMG_(\d+)\.[Jj][Pp][Gg]:) {
	$nr=$1;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/ST[ABCD]_(\d+)\.[Jj][Pp][Gg]:) {
	$nr=$1;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/(\d+)-(\d+)_IMG\.[Jj][Pp][Gg]:) {
	$nr=$2;
	$filmnr=$1;
	push @changes,"filmnr=$filmnr" if $filmnr;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($file =~ m:.*/[Ii]mage(\d+)\.[Jj][Pp][Gg]:) {
	$nr=$1;
	push @changes,"fotonr=$nr" if $nr;
    }
    if ($model) {
	$model = undef if ($model eq "Photonet Central Lab Scanner 4B150-35A");
	push @changes,"camera='$model'" if $model;
    }
    if ($artist) {
	push @changes,"auteur='$artist'";
    }
    if ($description) {
	push @changes,"omschrijving='$description'";
    }
    if ($datum) {
	$datum =~ s/:/-/g;
	$datum =~ s/ .*//;
	$datum = undef if $datum eq '0000-00-00';
	push @changes,"datum='$datum'" if $datum;
    }
    $sth_fillinfo->execute($nr,$model,$artist,$description,$datum,$insidethumb,$filmnr,$file);
	$dbh->commit();
}

print "Maak thumbnails\n";

my $cnt=0;
my $sth_loimport=$dbh->prepare("select lo_import(?)");
my $sth2=$dbh->prepare("update fotos set thumbnail=? where filename=?");
my $sth4=$dbh->prepare("update fotos set insidethumb=true where filename=?");
my $sth5=$dbh->prepare("select filename from fotos where thumbnail is null and not insidethumb;");

$sth5->execute();
while (@row=$sth5->fetchrow_array()) {
    my $filedb=$row[0];
    my $file = $filedb;
    $file =~ s/''/'/g;
    my $info = ImageInfo($file);
    unless ($$info{ThumbnailImage}) {
	if ($row[0] =~ /\.[jJ][pP][eE]?[gG]$/) {
	    my $tempname="/tmp/thumb_$$-$cnt.jpg";
	    print $row[0],"\n";
	    
	    my $cmd="djpeg \"$file\" | /usr/local/bin/pnmscale -xy 120 100 | cjpeg -progressive -optimize -outfile $tempname";
	    print "$cmd\n";
	    system($cmd);
	    $sth_loimport->execute($tempname);
	    my @row1 = $sth_loimport->fetchrow_array();
	    my $oid = $row1[0];
	    print Dumper($oid);
	    $sth2->execute($oid,$file) if defined($oid);
#	    $dbh->do("update fotos set thumbnail=$oid where filename='$filedb';") if (defined ($oid);
	    unlink($tempname);
	    $cnt++;
	}
    } else {
#	print "Thumbnail inside file $file\n";
	$sth4->execute($file);
    }
}

$sth5->finish();
$dbh->commit();

print "En de jaar en maand albums vullen\n";

$dbh->do("create temporary table albumtemp (naam varchar)");
$dbh->do("insert into albumtemp select distinct to_char(datum,'YYYY') from fotos");
$dbh->do("insert into albumtemp select distinct to_char(datum,'YYYY-MM') from fotos");
$dbh->do("delete from albumtemp where naam is null");
$dbh->do("insert into album (naam) select naam from albumtemp where not naam in (select naam from album)");
$dbh->do("insert into albumfoto (albumid,fotoid,seqnr) select a.id,f.id,f.id from fotos as f,album as a where (a.naam=to_char(f.datum,'YYYY') or a.naam=to_char(f.datum,'YYYY-MM')) and not a.id in (select albumid from albumfoto where fotoid=f.id)");

$dbh->commit();
#$dbh->disconnect();
