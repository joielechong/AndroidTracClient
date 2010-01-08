#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use XML::Simple;
use Geo::Distance;

my $nodes;
my $ways;
my $dist;
my $infinity = 100000000;

sub shortest_path {
    my $n = shift;
    my $s = shift;    #start node
    my $t = shift;    #end node
 
    my ($I,$j,$k,$min);
    my @path;

    for $k (keys %$nodes) {
	$$nodes{$k}->{predecessor} = -1;
	$$nodes{$k}->{length} = $infinity;
	$$nodes{$k}->{label}='tentative';
    }
    $$nodes{$t}->{length} = 0;
    $$nodes{$t}->{label}='permanent';

    my $k1;
    $k = $t;
    do {
#	print "k=$k  loop1\n";
	for $I (keys %{$dist->{$k}}) {
#	    print "I=$I, k=$k  loop2\n";
	    if ($$nodes{$I}->{label} eq 'tentative') {
		$$nodes{$I}->{predecessor}=$k;
		$$nodes{$I}->{length} = $$nodes{$k}->{length}+$dist->{$k}->{$I};
#		print "     I=$I, k=$k, length=",$$nodes{$I}->{length},"\n";
	    }
	}
#	print("I=$I, k=$k: ");
#	for $j (sort keys %$nodes) {
#	    printf("{%d %d %s}",$$nodes{$j}->{predecessor},$$nodes{$j}->{length},$$nodes{$j}->{label});
#	}
#	print("\n");
	$k1=$k;
	$k=0;$min=$infinity;
#	for $I (keys %{$dist->{$k1}}) {
	for $I (keys %$nodes) {
	    if ($$nodes{$I}->{label} eq "tentative" && $$nodes{$I}->{length} < $min) {
		$min=$$nodes{$I}->{length};
		$k=$I;
	    }
	}
	$$nodes{$k}->{label}="permanent";
    }while ($k!=$s && $k != 0);
    
    $I=0;
    $k=$s;
    do {
	$path[$I++]=$k;
	$k=$$nodes{$k}->{predecessor};
    } while ($k gt '0');
    return @path;
}

sub print_path {
    my @p1=@_;
    print join(", ",@p1)," ",$$nodes{$p1[0]}->{length},"\n";
    for my $p (@p1) {
	print "$p : ";
	my @ws = @{$$nodes{$p}->{ways}};
	for my $w (@ws) {
	    my @tags = @{$$ways{$w}->{tag}};
	for my $t (@tags) {
	    print $t->{v}," " if $t->{k} eq 'name'
	}
	}
	print "\n";
    }
}

my %sources;
my $doc = XMLin('map.osm', ForceArray=>['tag']);
#print Dumper($doc);

$nodes = $doc->{node};
$ways = $doc->{way};
#print Dumper($ways);
my $geo=new Geo::Distance;
my $nrnodes;

print "Start met initialiseren\n";
foreach my $w (keys %$ways) {
#    print Dumper($ways->{$w}->{nd});
    $nrnodes = $#{$ways->{$w}->{nd}}+1;
    my ($n1,$n2);
    for (my $i=0;$i<$nrnodes-1;$i++) {
	$n1 = $ways->{$w}->{nd}->[$i]->{ref};
	$n2 = $ways->{$w}->{nd}->[$i+1]->{ref};
	my $distance = $geo->distance('meter',$$nodes{$n1}->{lon},$$nodes{$n1}->{lat}=>$$nodes{$n2}->{lon},$$nodes{$n2}->{lat});
#	print "$w :".$$nodes{$n1}->{lon}.",".$$nodes{$n1}->{lat}." ".$$nodes{$n2}->{lon}.",".$$nodes{$n2}->{lat}." $distance\n";
	$dist->{$n1}->{$n2}=$distance;
	$dist->{$n2}->{$n1}=$distance;
	$$nodes{$n1}->{ways} = () if (!defined($$nodes{$n1}));
	$$nodes{$n2}->{ways} = () if (!defined($$nodes{$n2}));
	push @{$$nodes{$n1}->{ways}},$w;
    }
    push @{$$nodes{$n2}->{ways}},$w;
}
#print Dumper($nodes);
print "Initialisatie is klaar\n";

print_path(shortest_path($nrnodes,'46071276','294062118'));
print_path(shortest_path($nrnodes,'46071276','294062059'));
print_path(shortest_path($nrnodes,'46071276','46051999'));
print_path(shortest_path($nrnodes,'46070723','46051999'));
print_path(shortest_path($nrnodes,'46070723','46026341'));
print_path(shortest_path($nrnodes,'46071276','46026341'));
