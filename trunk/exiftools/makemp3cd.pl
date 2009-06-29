#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;

my %contents;
my %directories;
my %tracklist;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl") or die "cannot open database\n";
my $sth1 = $dbh->prepare("SELECT directory,count(*) FROM mp3cdcontents GROUP BY directory");
my $sth2 = $dbh->prepare("SELECT cdnr,directory,track,artist,song,filename FROM mp3cdcontents");

$sth1->execute();
while (my @row=$sth1->fetchrow_array()) {
    $directories{$row[0]} = $row[1];
    $tracklist{$row[0]} = ();
}

$sth2->execute();
while (my @row=$sth2->fetchrow_array()) {
    $contents{$row[5]}->{cdnr}=$row[0];
    $contents{$row[5]}->{directory}=$row[1];
    my $track = $row[2];
    if (defined($track)) {
	$track = $directories{$row[1]} if $track == -1;
	$track = undef if ($track < 1 or $track > $directories{$row[1]});
    }
    push @{$tracklist{$row[1]}},$track if defined($track);
    $contents{$row[5]}->{track}=$track;
    $contents{$row[5]}->{artist}=$row[3];
    $contents{$row[5]}->{song}=$row[4];
}

print Dumper(\%directories);
print Dumper(\%contents);
print Dumper(\%tracklist);

foreach my $dir (sort keys %directories) {
	my @inhoud;
	print "Nu verwerken van $dir\n";
	my $ntracks = $directories{$dir};
	foreach my $song (keys %content) {
	    next if $dir ne $content{$song}->{directory};
	    my $track = $content{$song}->{track}
	    next unless defined $track;
	    unless (defined $inhoud[$track]) {
	        $inhoud[$track] = $content{$song};
	    } else {
		my $inc=1;
		$inc = -1 if $track == $directories{$dir};
		while (defined($inhoud[$track])) {
		    $track += $inc;
		    if ($track > $directories{$dir}) {
			$inc = -1;
			$track=--;
		    }
		}
		$inhoud[$track] = $content[$song];
	    }
	}
	foreach my $song (keys %content) {
	    next if $dir ne $content{$song}->{directory};
	    my $track = $content{$song}->{track}
	    next if defined $track;
	    $track = rand($directories{$dir})+1;
	    unless (defined $inhoud[$track]) {
	        $inhoud[$track] = $content{$song};
	    } else {
		my $inc=1;
		$inc = -1 if $track == $directories{$dir};
		while (defined($inhoud[$track])) {
		    $track += $inc;
		    if ($track > $directories{$dir}) {
			$inc = -1;
			$track=--;
		    }
		}
		$inhoud[$track] = $content[$song];
	    }
	}
	print Dumper(\@inhoud);
}