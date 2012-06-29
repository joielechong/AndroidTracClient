#!/usr/bin/perl -w

use strict;
use Encode;

my $fhIN;
my $fhUIT;

sub lees_ov2 {
    my ($buffer,$startpos,$size) = @_;

    my $record_type = unpack("C",substr($buffer,$startpos,2));
    my $length = unpack("L",substr($buffer,$startpos+1,4));
#    print "$record_type $length\n";
    if ($record_type == 1) {
	my $pos = $startpos + 21;
	my $endpos = $startpos + $length;
	my $lon1 = unpack("l",substr($buffer,$startpos+5,4));
	my $lat1 = unpack("l",substr($buffer,$startpos+9,4));
	my $lon2 = unpack("l",substr($buffer,$startpos+13,4));
	my $lat2 = unpack("l",substr($buffer,$startpos+17,4));
#	print "Area ",$lon1/100000.0,", ",$lat1/100000.0,", ",$lon2/100000.0,", ",$lat2/100000.0,"\n";
	while ($pos < $endpos) {
	    $pos += lees_ov2(substr($buffer,$pos,$length),0,$length);
	}
    } elsif ($record_type == 2 || $record_type == 15 || $record_type==3) {
	my $lon = unpack("l",substr($buffer,$startpos+5,4));
	my $lat = unpack("l",substr($buffer,$startpos+9,4));
	my $string = substr($buffer,$startpos+13,$length-14);
#	my $string = encode("utf8",substr($buffer,$startpos+13,$length-14));
	$string =~ s/\"/\'\'/g;
	$string =~ s/\000//g;
	$string =~ s/\r//g;
	$string =~ s/\n//g;
	print $fhUIT $lon/100000.0,", ",$lat/100000.0,", \"$string\"\n";
    } else {
	print "Record type $record_type niet ondersteund\n";
	return 1;  # 1 byte gelezen anders blijven we hangen
    }
    return $length;
}

sub dumpov2 {
    my $file_in = shift;
    my $file_uit = shift;
#    $file_uit .= ".test";

    my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,$mtime,$ctime,$blksize,$blocks) = stat($file_in);
    if (open $fhIN,"<$file_in" ) {
	if (open $fhUIT,">$file_uit") {
#	    print $fhUIT "; Readable locations in $file_in\n";
#	    print $fhUIT ";\n";
#	    print $fhUIT "; Longitude,    Latitude, \"Name\"\n";
#	    print $fhUIT ";========== ============ ==================================================\n";
	    print $fhUIT ";lon,lat,descr\n";
	    my $buffer;
	    read($fhIN,$buffer,$size);
	    my $pos=0;
	    while ($pos < $size) {
		$pos += lees_ov2($buffer,$pos,$size-$pos);
	    }
	    close $fhUIT;
	    close $fhIN;
	} else {
	    print STDERR "Dumpov2: kan $file_uit niet openen\n";
	}
    } else {
	print STDERR "Dumpov2: kan $file_in niet openen\n";
    }
}

sub modtime {
	my $filename=shift;

	if (-e $filename) {
		my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,$mtime,$ctime,$blksize,$blocks) = stat($filename);
		return $mtime;
	} else {
		return 0;
	}
}

my $startsync = shift;

opendir(DIR, ".") || die "can't opendir current directory: $!";
my @ov2s = grep { /\.ov2$/ } readdir(DIR);
closedir DIR;

foreach (@ov2s) {
#	print $_,"\n";
	s/.ov2//;
	my $ov2f = "$_.ov2";
	my $ascf = "$_.asc";
	my $timeasc=modtime($ascf);
	my $timeov2=modtime($ov2f);

#	print "$ov2f: $timeasc $timeov2\n";

	unlink($ascf) if $timeasc < $timeov2;
#	system("dumpov2 \"$ov2f\"") if $timeasc < $timeov2;
	dumpov2($ov2f,$ascf) if $timeasc < $timeov2;
#	unlink($ov2f) if -f $ascf;
}

#sleep 5;

#system("../../synctool.exe") if $startsync eq "sync";
