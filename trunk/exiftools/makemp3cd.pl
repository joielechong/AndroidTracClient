#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;

my %contents;
my %directories;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl") or die "cannot open database\n";
my $sth1 = $dbh->prepare("SELECT directory,count(*) FROM mp3cdcontents GROUP BY directory");
my $sth2 = $dbh->prepare("SELECT cdnr,directory,track,artist,song,filename FROM mp3cdcontents");

$sth1->execute();
while (my @row=$sth1->fetchrow_array()) {
    $directories{$row[0]} = $row[1];
}

$sth2->execute();
while (my @row=$sth2->fetchrow_array()) {
    $contents{$row[5]}->{cdnr}=$row[0];
    $contents{$row[5]}->{directory}=$row[1];
    my $track = $row[2];
    $track = undef if (defined($track) and ($track < 1 or $track > $directories{$row[1]}));
    $contents{$row[5]}->{track}=$track;
    $contents{$row[5]}->{artist}=$row[3];
    $contents{$row[5]}->{song}=$row[4];
}

print Dumper(\%directories);
print Dumper(\%contents);
