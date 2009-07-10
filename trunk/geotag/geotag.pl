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

sub dump2csv {
    my $gpxdata = shift;

    foreach my $key (sort (keys(%$gpxdata))) {
	print "$key,".$$gpxdata{$key}->{lat}.",".$$gpxdata{$key}->{lon}.",".$$gpxdata{$key}->{alt}.",";
	print $$gpxdata{$key}->{spd} if defined($$gpxdata{$key}->{spd});
	print "\n";
    }
}

sub gps2iso {
    my $gpstime = shift;
    my $gpsdate = shift;
    $gpsdate =~ m/(..)(..)(..)/;
    my $iso = "20$3-$2-$1T";
    $gpstime =~ m/(..)(..)(..)..../;
    $iso .= "$1:$2:$3Z";
    return $iso;
}

sub gps2dec {
    my $degstr=shift;
    my $ref = shift;

    my $deg=int ($degstr/100);
    $degstr -= 100*$deg;
    $deg += $degstr/60.0;
    $deg = -$deg if ($ref eq "S") or ($ref eq "W");
    return $deg;
}

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

my $strp1 = new DateTime::Format::Strptime(pattern=>'%F %T',time_zone=>'Europe/Amsterdam');
my $strp2 = new DateTime::Format::Strptime(pattern=>'%Y:%m:%d %T',time_zone=>'Europe/Amsterdam');
my $difftime = DateTime::Duration->new(seconds=>0);
 
if (defined($reffile)) {
    my $exif = new Image::ExifTool;
    my $success = $exif->ExtractInfo("$picdir/$reffile");
    my $reftime = $strp1->parse_datetime($reftime_src);
    my $date = $exif->GetValue('DateTimeOriginal');
    my $picttime = $strp2->parse_datetime($date); 
#    print "DateTimeOriginal = $date\n";
#    print "Reftime = ",$reftime->strftime("%F %T"),"\n";
    $difftime = $reftime - $picttime;
#    print "Picttime = ",$picttime->strftime("%F %T"),"\n";
#    print Dumper($difftime);
}

my %gpxdata;

if (defined($gpxfile)) {

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
	    $gpxdata{$$trkpt{'time'}}->{'alt'} = 0;
	}
    }
} else {  # dus is het NMEA
    my $lastdate;
    my $lastalt=0;
    open NMEA,"<$nmeafile" or die "Kan $nmeafile niet openen\n";
    while (<NMEA>) {
	my ($inline,$chk) = split('\*');
	next unless defined $chk;
	if ($inline =~ /^\$GPRMC/) {
	    my ($tag,$gpstime,$status,$lat,$latref,$lon,$lonref,$spd,$course,$gpsdate,$magvar,$magvarref) = split(",",$inline);
	    unless (defined($gpsdate)) {
		$lastdate = undef;
		$lastalt = undef;
		next;
	    }
	    $lastdate=$gpsdate;
	    next unless defined($status) and ($status eq "A");
	    my $iso = gps2iso($gpstime,$gpsdate);
	    $gpxdata{$iso}->{lat} = gps2dec($lat,$latref);
	    $gpxdata{$iso}->{lon} = gps2dec($lon,$lonref);
	    $gpxdata{$iso}->{alt} = $lastalt unless defined $gpxdata{$iso}->{alt};
	    $gpxdata{$iso}->{spd} = $spd*1.852 if defined($spd) and ($spd ne "");
	} elsif ($inline =~ /^\$GPGGA/) {
	    next unless defined $lastdate;
	    my ($tag,$gpstime,$lat,$latref,$lon,$lonref,$fixqual,$nrsat,$hdop,$altitude,$altunit,$geoidht,$geoidunit,$res1,$res2) = split(",",$inline);
	    next if $fixqual == 0;
	    unless (defined($altitude)) {
		$lastdate = undef;
		$lastalt = undef;
		next;
	    }
	    my $iso = gps2iso($gpstime,$lastdate);
	    $gpxdata{$iso}->{lat} = gps2dec($lat,$latref);
	    $gpxdata{$iso}->{lon} = gps2dec($lon,$lonref);
	    $gpxdata{$iso}->{alt} = $lastalt = $altitude;
	}
    }
}

#dump2csv(\%gpxdata);

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
    next unless defined($date);

    my $newdate = $date;
    my $oldtime = $strp2->parse_datetime($date);
    my $newtime = $oldtime + $difftime;
    $newdate = $newtime->strftime("%Y:%m:%d %T");
    $newtime->set_time_zone('UTC');

    my $isotime=$newtime->strftime("%FT%TZ");
    my $lat=$gpxdata{$isotime}->{'lat'};
    my $lon=$gpxdata{$isotime}->{'lon'};
    my $alt=$gpxdata{$isotime}->{'alt'};
    my $spd=$gpxdata{$isotime}->{'spd'};
    next unless defined($lat);
    print "$file\noldtime = $date\nnewdate = $newdate\nisotime=$isotime\n";

#
# FIXME wat als GPS minder vaak dan 1/s GPX slaat bij identiek over
#
    $lat = 'undef' unless defined($lat);
    $lon = 'undef' unless defined($lon);
    print " $isotime $lat $lon\n";
    if ($lat >=0) {
	$exif->SetNewValue('GPSLatitude',$lat);
	$exif->SetNewValue('GPSLatitudeRef','N');
    } else {
	$exif->SetNewValue('GPSLatitude',-$lat);
	$exif->SetNewValue('GPSLatitudeRef','S');
    }
    if ($lon >= 0) {
	$exif->SetNewValue('GPSLongitude',$lon);
	$exif->SetNewValue('GPSLongitudeRef','E');
    } else {
	$exif->SetNewValue('GPSLongitude',-$lon);
	$exif->SetNewValue('GPSLongitudeRef','W');
    }
    $exif->SetNewValue('GPSAltitude',$alt)  if (defined($alt));
    $exif->SetNewValue('GPSSpeed',$spd) if (defined($spd)) ;
    $exif->SetNewValue('GPSTimeStamp',substr($isotime,11,8));
    $exif->SetNewValue('FileModifyDate',$newdate,Protected=>1);
    $exif->WriteInfo("$picdir/$file","/temp/$file");
}
