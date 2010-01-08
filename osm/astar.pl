#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use XML::Simple;
use Geo::Distance;

my $nodes;
my $ways;
my $dist;
my $infinity = 100000000;

my $geo=new Geo::Distance;

sub distance {
    my $n1 = shift;
    my $n2 = shift;
    return $geo->distance('meter',$$nodes{$n1}->{lon},$$nodes{$n1}->{lat}=>$$nodes{$n2}->{lon},$$nodes{$n2}->{lat});
}

# 
# function reconstruct_path(came_from,current_node)
#     if came_from[current_node] is set
#         p = reconstruct_path(came_from,came_from[current_node])
#         return (p + current_node)
#     else
#         return the empty path
sub reconstruct_path {
  my $came_from = shift;
  my $current_node = shift;
  
  my @path = ();  
  if (defined($came_from->{$current_node})) {
  @path = reconstruct_path($came_from,$came_from->{$current_node});
  }
  push @path,$current_node;
  return  @path;
}
sub Astar {
    
    my $start = shift;
    my $goal  = shift;
    
    my %closedset;
    my %openset;
    my %g_score;
    my %h_score;
    my %f_score;
    my %came_from;
    
    return "foutje" unless defined($$nodes{$start}) and defined($$nodes{$goal});
    
#     closedset := the empty set                 % The set of nodes already evaluated.     
#     openset := set containing the initial node % The set of tentative nodes to be evaluated.
    
    $openset{$start} = 1;
    
#     g_score[start] := 0                        % Distance from start along optimal path.
#     h_score[start] := heuristic_estimate_of_distance(start, goal)
#     f_score[start] := h_score[start]           % Estimated total distance from start to goal through y.
    
    $g_score{$start} = 0;
    $f_score{$start} = $h_score{$start} = distance($start,$goal);
    
#     while openset is not empty
    while (keys(%openset) != 0) {
#         x := the node in openset having the lowest f_score[] value
	my $x = -1;
	
	for my $k (keys(%openset)) {
	    $x=$k if $x == -1;
	    $x=$k if ($f_score{$k} < $f_score{$x});
	}
#         if x = goal
#             return reconstruct_path(came_from,goal)
	if ($x == $goal) {
	    my @p = reconstruct_path(\%came_from,$goal);
	    return @p;
	}
#         remove x from openset
	delete($openset{$x});
#         add x to closedset
	$closedset{$x} = 1;
#         foreach y in neighbor_nodes(x)
	for my $y (keys(%{$$dist{$x}})) {
#             if y in closedset
#                 continue
	    next if (defined($closedset{$y}));
#             tentative_g_score := g_score[x] + dist_between(x,y)
	    my $tentative_g_score = $g_score{$x} + $dist->{$x}->{$y};
# 
#             if y not in openset
#                 add y to openset
	    my $tentative_is_better;
	    unless (defined($openset{$y})) {
		$openset{$y} = 1 ;
# 
#                 tentative_is_better := true
		$tentative_is_better = 1;
#             elseif tentative_g_score < g_score[y]
	    } elsif ($tentative_g_score < $g_score{$y}) {
#                 tentative_is_better := true
		$tentative_is_better = 1;
#             else
	    } else {
#                 tentative_is_better := false
		$tentative_is_better = 0;
	    }
#             if tentative_is_better = true
#                 came_from[y] := x
#                 g_score[y] := tentative_g_score
#                 h_score[y] := heuristic_estimate_of_distance(y, goal)
#                 f_score[y] := g_score[y] + h_score[y]
	    if ($tentative_is_better) {
		$came_from{$y} = $x;
		$g_score{$y} = $tentative_g_score;
		$h_score{$y} = distance($y,$goal);
		$f_score{$y} = $g_score{$y}+$h_score{$y};
	    }
	}
    }
#     return failure
    return "foutje";
}

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
    print join(", ",@p1);
    print " ",$$nodes{$p1[0]}->{length} if defined($$nodes{$p1[0]}->{length});
    print "\n";
    for my $p (@p1) {
	print "$p : ";
	my @ws = @{$$nodes{$p}->{ways}};
	for my $w (@ws) {
	    my @tags = @{$$ways{$w}->{tag}};
	    for my $t (@tags) {
		print $t->{v}," " if ($t->{k} eq 'name') or ($t->{k} eq 'highway') or ($t->{k} eq 'maxspeed');
	    }
	}
	print "\n";
    }
}

sub usable_way {
    my $w = shift;
    return 0 unless defined($w->{tag});
    my @tags = @{$w->{tag}};
    for my $t (@tags) {
	return 1 if ($t->{k} eq 'highway');
    }
    return 0;
}

my %sources;
print "Inlezen map.osm\n";
my $doc = XMLin('map.osm', ForceArray=>['tag']);
#open DUMP,">map.osm.txt";
#print DUMP Dumper($doc);
#close DUMP;

$nodes = $doc->{node};
$ways = $doc->{way};
#print Dumper($ways);
my $nrnodes;

print "Start met initialiseren\n";
foreach my $w (keys %$ways) {
    unless (usable_way($ways->{$w})) {
	delete($$ways{$w});
	next;
    }
    
#    print Dumper($ways->{$w}->{nd});
    $nrnodes = $#{$ways->{$w}->{nd}}+1;
    my ($n1,$n2);
    for (my $i=0;$i<$nrnodes-1;$i++) {
	$n1 = $ways->{$w}->{nd}->[$i]->{ref};
	$n2 = $ways->{$w}->{nd}->[$i+1]->{ref};
	my $distance = distance($n1,$n2);
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

print_path(Astar('46071276','294062118'));
print_path(Astar('46071276','294062059'));
print_path(Astar('46071276','46051999'));
print_path(Astar('46070723','46051999'));
print_path(Astar('46070723','46026341'));
print_path(Astar('46071276','46026341'));
print_path(Astar('46071276','289899699'));