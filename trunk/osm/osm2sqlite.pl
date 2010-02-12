#! /usr/bin/perl -w

use XML::Simple;
use Data::Dumper;

open ENT,">entity.csv";
open NODE,">nodeinfo.csv";
open TAGS,">tags.csv";
open ND,">waynd.csv";
open MEM,">member.csv";

opendir(my $dh, "maps") || die "can't opendir : $!";
my @files = grep { /\.osm$/ && -f "maps/$_" } readdir($dh);
closedir $dh;
foreach my $f (@files) {

my $content = "maps/$f";
print "$f\n";
my $result = XMLin($content, ForceArray=>['tag','nd','member','way','node','relation'],KeyAttr=>[]);

my $nodes = $result->{node};

foreach my $n (@$nodes) {
     print ENT $n->{id},"|node\n";
     print NODE join("|",($n->{id},$n->{lat},$n->{lon},$n->{version},$n->{timestamp})),"\n";
     my $tags = $n->{tag};
     foreach my $t (@$tags) {
         print TAGS join("|",($n->{id},$t->{k},$t->{v})),"\n";
     }
}

my $ways = $result->{way};
foreach my $w (@$ways) {
    print ENT $w->{id},"|way\n";
    my @nds=@{$w->{nd}};
    for (my $i=0;$i<=$#nds;$i++) {
        print ND join("|",($w->{id},$nds[$i]->{ref},$i)),"\n";
    }
     my $tags = $w->{tag};
     foreach my $t (@$tags) {
         print TAGS join("|",($w->{id},$t->{k},$t->{v})),"\n";
     }
}

my $rels = $result->{relation};
foreach my $r (@$rels) {
    print ENT $r->{id},"|relation\n";
    my @mems = @{$r->{member}};
    for (my $i=0;$i<=$#mems;$i++) {
        print MEM join("|",($r->{id},$mems[$i]->{type},$mems[$i]->{ref},$mems[$i]->{role},$i)),"\n";
    }
     my $tags = $r->{tag};
     foreach my $t (@$tags) {
         print TAGS join("|",($r->{id},$t->{k},$t->{v})),"\n";
     }
}
}