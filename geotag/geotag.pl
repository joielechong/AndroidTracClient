#! /usr/bin/perl -w

use strict;
use XML::Simple;
use Data::Dumper;
use Image::ExifTool qw(:Public);
use POSIX qw(mktime gmtime ctime);
use Getopt::Long;

my $gpxfile = undef;
my $picdir = undef;
my $reffile = undef;
my $reftime = undef;

sub usage {
	print "Usage:\n   geotag.pl --gpx <gpxfile> --dir <directory containing pictures> [--reffile <reference file> --reftime <reference time>]\n\n";
	exit(2);
}

my $result = GetOptions("gpx=s" => \$gpxfile,
			"dir=s" => \$picdir,
			"reffile=s" => \$reffile,
			"reftime=s" => \$reftime);

#print "gpx = $gpxfile\n" if defined $gpxfile;
#print "dir = $picdir\n" if defined $picdir;
#print "reffile = $reffile\n" if defined $reffile;
#print "reftime = $reftime\n" if defined $reftime;


print "result = $result\n";

usage() unless (defined($gpxfile) and defined($picdir));
usage() unless (defined($reffile) xor defined($reftime));

my %gpxdata;

my $xml=XML::Simple->new();

my $ref = $xml->XMLin($gpxfile, ForceArray=>['trkseg','trkpt']);

#FIXME check of wel ingelezen

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
#
# FIXME afhandelen returncode
#

#    my $info = ImageInfo("$picdir/$file");
#    print Dumper($info);
    my $info=$exif->GetInfo('DateTimeOriginal');
# 
# FIXME hier iets mee doen?
#
    my $date = $exif->GetValue('DateTimeOriginal');
#
# FIXME wat als er geen tijd is?
#
    print "$file $date";
    my ($datum,$tijd) = split(' ',$date);
    my ($year,$month,$day) = split(':',$datum);
    my ($hour,$min,$sec) = split(':',$tijd);
#
#  FIXME: nu nog een handmatige correctie gaat via $correct
#
    if (defined($reffile)) {  
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
    }

    my $isotime=sprintf("%4.4d-%2.2d-%2.2dT%2.2d:%2.2d:%2.2dZ",$year,$month,$day,$hour,$min,$sec);
    my $lat=$gpxdata{$isotime}->{'lat'};
    my $lon=$gpxdata{$isotime}->{'lon'};
    next unless defined($lat);
#
# FIXME wat als GPS minder vaak dan 1/s
#
    $lat = 'undef' unless defined($lat);
    $lon = 'undef' unless defined($lon);
    print " $isotime $lat $lon\n";
    $exif->SetNewValue('GPSLatitude',$lat);
    $exif->SetNewValue('GPSLatitudeRef','N');  #FIXME  zou niet vast moeten zijn
    $exif->SetNewValue('GPSLongitude',$lon);
    $exif->SetNewValue('GPSLongitudeRef','E');  #FIXME  zou niet vast moeten zijn
    $exif->SetNewValue('GPSAltitude',0); # FIXME NMEA levert wel hoogte
    $exif->SetNewValue('GPSTimeStamp',substr($isotime,11,8));
    $exif->SetNewValue('FileModifyDate',$isotime)
    $exif->WriteInfo("$picdir/$file","/temp/$file");
#    $exif->SetFileModifyDate($picdir/$file);
}
