#! /usr/bin/perl -w

use strict;
use XML::Simple;
use Data::Dumper;
use Image::ExifTool qw(:Public);
use POSIX qw(mktime gmtime ctime);

my $gpxfile = shift;
my $picdir = shift;
my $correct = shift;

my %gpxdata;



my $xml=XML::Simple->new();

my $ref = $xml->XMLin($gpxfile);
foreach my $seg (@{$ref->{trk}->{trkseg}}) {
#    print "Segment\n";
#    print Dumper($seg);
    foreach my $trkpt (@{$seg->{trkpt}}) {
#	print "Trackpoints\n";
#	print Dumper($trkpt);
	$gpxdata{$$trkpt{'time'}}->{'lat'} = $$trkpt{'lat'};
	$gpxdata{$$trkpt{'time'}}->{'lon'} = $$trkpt{'lon'};
    }
}
#print Dumper(\%gpxdata);

opendir FOTOOS,$picdir;
my @files = sort (grep {/\.[Jj][pP][gG]$/} readdir(FOTOOS));
closedir FOTOOS;

#print join(",",@files),"\n";

foreach my $file (@files) {
    my $exif = new Image::ExifTool;
    my $success = $exif->ExtractInfo("$picdir/$file");

#    my $info = ImageInfo("$picdir/$file");
#    print Dumper($info);
    my $info=$exif->GetInfo('DateTimeOriginal');
    my $date = $exif->GetValue('DateTimeOriginal');
    print "$file $date";
    my ($datum,$tijd) = split(' ',$date);
    my ($year,$month,$day) = split(':',$datum);
    my ($hour,$min,$sec) = split(':',$tijd);
#
#  FIXME: nu nog een handmatige correctie gaat via $correct
#
    $sec -= 4;
    if ($sec < 0) {
	$sec += 60;
	$min--;
    }
    $min -= 58;
    if ($min < 0) {
	$min += 60;
	$hour--;
    }
#    
#  Einde FIXME
#    
    my $isotime=sprintf("%4.4d-%2.2d-%2.2dT%2.2d:%2.2d:%2.2dZ",$year,$month,$day,$hour,$min,$sec);
    my $lat=$gpxdata{$isotime}->{'lat'};
    my $lon=$gpxdata{$isotime}->{'lon'};
    next unless defined($lat);
    $lat = 'undef' unless defined($lat);
    $lon = 'undef' unless defined($lon);
    print " $isotime $lat $lon\n";
    $exif->SetNewValue('GPSLatitude',$lat);
    $exif->SetNewValue('GPSLatitudeRef','N');
    $exif->SetNewValue('GPSLongitude',$lon);
    $exif->SetNewValue('GPSLongitudeRef','E');
    $exif->SetNewValue('GPSAltitude',0);
    $exif->SetNewValue('GPSTimeStamp',substr($isotime,11,8));
    $exif->WriteInfo("$picdir/$file","/temp/$file");
}
