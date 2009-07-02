#! /usr/bin/perl -w

use strict;
use DBI;
use File::Basename;
use Data::Dumper;

my %contents;
my %directories;
my %tracklist;

my $PREFIX="/data/Music/";

my @sufflist=('.mp3','.MP3','.wma','.WMA');

my $cdnr = shift;

die "Aanroep: $0 <cdnr>\n" unless defined $cdnr;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl") or die "cannot open database\n";
my $sth1 = $dbh->prepare("SELECT directory,count(*) FROM mp3cdcontents where cdnr=?  GROUP BY directory");
my $sth2 = $dbh->prepare("SELECT cdnr,directory,track,artist,song,filename FROM mp3cdcontents WHERE cdnr=? ORDER BY cdnr,directory,track");

$sth1->execute($cdnr);
while (my @row=$sth1->fetchrow_array()) {
    $directories{$row[0]} = $row[1];
    $tracklist{$row[0]} = ();
}

$sth2->execute($cdnr);
while (my @row=$sth2->fetchrow_array()) {
	my $file = $row[5];
    $contents{$file}->{cdnr}=$row[0];
    $contents{$file}->{directory}=$row[1];
    my $track = $row[2];
    if (defined($track)) {
	$track = $directories{$row[1]} if $track == -1;
	$track = undef if ($track < 1 or $track > $directories{$row[1]});
    }
    push @{$tracklist{$row[1]}},$track if defined($track);
    $contents{$file}->{track}=$track;
    $contents{$file}->{artist}=$row[3];
    $contents{$file}->{song}=$row[4];
    my @stat = stat("$PREFIX/".$file);
    $contents{$file}->{netto}=$stat[7];
}

#print Dumper(\%directories);
#print Dumper(\%contents);
#print Dumper(\%tracklist);

my $netto=0;

foreach my $dir (sort keys %directories) {
	my @inhoud;
	print STDERR "Nu verwerken van $dir\n";
	my $ntracks = $directories{$dir};
	foreach my $song (keys %contents) {
	    next if $dir ne $contents{$song}->{directory};
	    my $track = $contents{$song}->{track};
	    next unless defined $track;
	    unless (defined $inhoud[$track]) {
	        $inhoud[$track] = $song;
	    } else {
		my $inc=1;
		$inc = -1 if $track == $directories{$dir};
		while (defined($inhoud[$track])) {
		    $track += $inc;
		    if ($track > $directories{$dir}) {
			$inc = -1;
			$track--;
		    }
		}
		$inhoud[$track] = $song;
	    }
	}
	foreach my $song (keys %contents) {
	    next if $dir ne $contents{$song}->{directory};
	    my $track = $contents{$song}->{track};
	    next if defined $track;
	    $track = 1+ int(rand($directories{$dir}));
	    my $inc=1;
	    $inc = -1 if $track == $directories{$dir};
	    while (defined($inhoud[$track])) {
		$track += $inc;
		if ($track > $directories{$dir}) {
		    $inc = -1;
		    $track--;
		}
	    }
	    $inhoud[$track] = $song;
	}	
#	print Dumper(\@inhoud);
	for (my $i=1;$i<= $directories{$dir};$i++) {
	    my $file = $inhoud[$i];
	    my ($name,$path,$ext) = fileparse($file,@sufflist);
	    my $line = sprintf("%s/%3.3d - %s - %s%s=%s",$dir,$i,$contents{$file}->{song},$contents{$file}->{artist},$ext,$file);
	    print "$line\n";
	    $netto += $contents{$file}->{netto};
	}
}

print STDERR "Klaar\n  Netto $netto bytes\n\n";
