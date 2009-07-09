#! /usr/bin/perl -w

use strict;
use XML::Simple;
use DateTime;
use DateTime::Duration;
use DateTime::Format::Strptime;
use Image::ExifTool qw(:Public);
use POSIX qw(mktime gmtime ctime);
use Getopt::Long;
use Data::Dumper;

my $gpxfile = undef;
my $nmeafile = undef;
my $picdir = undef;
my $reffile = undef;
my $reftime_src = undef;
my $reftime = undef;

sub usage {
	print "Usage:\n";
	print "   geotag.pl [--gpx <gpxfile> | --nmea <nmeafile> ] --dir <directory containing pictures> [--reffile <reference file> --reftime <reference time>]\n\n";
	print "       One of --gpx or --nmea must be specified\n";
	print "       --reffile and --reftime must be both present or both absent\n\n";
	exit(2);
}

my $result = GetOptions("gpx=s" => \$gpxfile,
			"nmea=s" => \$nmeafile,
			"dir=s" => \$picdir,
			"reffile=s" => \$reffile,
			"reftime=s" => \$reftime_src);

#print "gpx = $gpxfile\n" if defined $gpxfile;
#print "nmea = $nmeafile\n" if defined $nmeafile;
#print "dir = $picdir\n" if defined $picdir;
#print "reffile = $reffile\n" if defined $reffile;
#print "reftime = $reftime_src\n" if defined $reftime_src;


#print "result = $result\n";

usage() unless ((defined($gpxfile) xor defined($nmeafile)) and defined($picdir));
usage() if (defined($reffile) xor defined($reftime_src));

if (defined($reffile)) {
    my $exif = new Image::ExifTool;
    my $success = $exif->ExtractInfo("$picdir/$reffile");
    my $strp = new DateTime::Format::Strptime(pattern=>'%F %T');
    my $reftime = $strp->parse_datetime($reftime_src);
    my $date = $exif->GetValue('DateTimeOriginal');
    print "DateTimeOriginal = $date\n";
    print "Reftime = ",$reftime->strftime("%F %T"),"\n";
    my $picttime = $strp->parse_datetime($date); 
    my $difftime = $reftime - $picttime;
    print Dumper($reftime);
    print Dumper($picttime);
    print Dumper($difftime);
    exit();
}

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
    my $info=$exif->GetInfo('DateTimeOriginal');
#    print Dumper($info);
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
    $exif->SetNewValue('FileModifyDate',$isotime);
    $exif->WriteInfo("$picdir/$file","/temp/$file");
#    $exif->SetFileModifyDate($picdir/$file);
}
