#! /usr/bin/perl -w

use strict;
use LWP::UserAgent;
use Data::Dumper;
use XML::Simple;
use Geo::Distance;

my %profiles = ('foot' =>    {allowed => ['footway','pedestrian','cycleway','service','residential','unclassified','tertiary','secondary','primary'],
                              boete => [0,0,10,20,0,20,30,30,30],
			      maxsnelheid => 5,
			      oneway=>0},
		'bicycle' => {allowed => ['cycleway','service','residential','unclassified','tertiary','secondary','primary'],
                              boete => [0,20,0,20,30,30,30],
			      maxsnelheid => 15,
			      oneway => 1},
		'car' =>      {allowed => ['service','residential','unclassified','tertiary','secondary','primary','motorway_link','motorway'],
                              boete => [30,30,20,20,10,0,0,0],
			      maxsnelheid => 160,
			      oneway =>1}
	       );
			   
my %highways = (footway =>5,
                pedestrian=>5,
		cycleway=>15,
		service=>30,
		residential=>30,
		unclassified=>50,
		tertiary=>50,
		secondary=>50,
		primary=>50,
		motorway_link=>100,
		motorway=>120);

my $nodes;
my $ways;
my $dist;
my $infinity = 9999999999;

my @bbox = (4.83,52.28,4.88,52.31);
my $getmapcmd ="http://api.openstreetmap.org/api/0.6/map?bbox=";

my $geo=new Geo::Distance;
my $ua = LWP::UserAgent->new;

sub getmapdata {
    my @bbox = @_;
    return -1  if $#bbox != 3 ;
    
    my $url = $getmapcmd.join(",",@bbox);
    print "url = $url\n";
    my $req = HTTP::Request->new(GET =>$url);
    my $result = $ua->request($req);
    print "Data is binnengehaald\n";
    return -1 unless $result->code == 200;
    return $result->content;
}

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
    my $vehicle = shift;
    $vehicle = 'car' unless defined($vehicle);
    
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
	$k=0;
	$min=$infinity;
#	for $I (keys %{$dist->{$k1}}) {
	for $I (keys %$nodes) {
	    if ($$nodes{$I}->{label} eq "tentative" && $$nodes{$I}->{length} < $min) {
		$min=$$nodes{$I}->{length};
		$k=$I;
	    }
	}
	$$nodes{$k}->{label}="permanent";
    } while ($k!=$s && $k != 0);
    
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
    my $oldp;
    my $oldw;
    for my $p (@p1) {
        my $w;
	print "$p -> ";
	my @ws = @{$$nodes{$p}->{ways}};
	if ($#ws == 0) {
	    $w = $ws[0];
	} else {
	    if (defined($oldp)) {  
	        if (defined($oldw)) {
	            for my $w1 (@ws) {
		        if ($w1==$oldw) {
			    $w=$w1;
			    last;
			}
	            }
		}
		unless (defined($w)) {
	            my @ws1 = @{$$nodes{$oldp}->{ways}};
		    for my $w1 (@ws) {
		        for my $w2 (@ws1) {
			    if ($w1==$w2) {
			      $w=$w1;
			      last;
			    }
			}
			last if defined($w);
		    }
		}
	    }
	}
	if (defined($w)) {
	    print $w,": ";
	    print $$ways{$w}->{tag}->{name}," " if defined($$ways{$w}->{tag}->{name});
	    print $$ways{$w}->{tag}->{highway}," " if defined($$ways{$w}->{tag}->{highway});
	    print $$ways{$w}->{tag}->{maxspeed}," " if defined($$ways{$w}->{tag}->{maxspeed});
            $oldw=$w;
	}
	$oldp=$p;
	print "\n";
    }
}

sub usable_way {
    my $w = shift;
    return exists($w->{tag}->{highway});
}

my %sources;

my $arg = shift;
my $doc;
if (defined($arg) && ($arg eq "net")) {
    my $data = getmapdata(@bbox);
    die "Cannot connect\n" if $data eq "-1";
    open MAP, ">newmap.osm";
    print MAP $data;
    close MAP;
    $doc = XMLin('newmap.osm', ForceArray=>['tag'],KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
} else {
    print "Inlezen map.osm\n";
    $doc = XMLin('map.osm', ForceArray=>['tag'],KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
}
open DUMP,">dump1.map.osm.txt";
print DUMP Dumper($doc);
close DUMP;

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
    my $oneway = $ways->{$w}->{tag}->{oneway};
    $oneway = "no" unless defined $oneway;
    $oneway = "yes" if $oneway eq "true";
    $oneway = "yes" if $oneway eq "1";
    $oneway = "rev" if $oneway eq "-1";
    if ($oneway eq "no") {
	delete $ways->{$w}->{tag}->{oneway} if exists($ways->{$w}->{tag}->{oneway});
    } else {
        $ways->{$w}->{tag}->{oneway} = $oneway;
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

foreach my $n (keys %$nodes) {
    delete $$nodes{$n} unless exists($$nodes{$n}->{ways});
}


open DUMP,">dump2.map.osm.txt";
print DUMP Dumper($doc);
close DUMP;

#print Dumper($nodes);
print "Initialisatie is klaar\n";

print_path(Astar('46071276','294062118'));
print_path(Astar('46070723','294062118','foot'));
print_path(Astar('46070723','294062118','car'));
print_path(Astar('46070723','294062118','bicycle'));
print_path(Astar('46071276','294062059'));
print_path(Astar('46071276','46051999'));
print_path(Astar('46070723','46051999'));
print_path(Astar('46070723','46026341'));
print_path(Astar('46071276','46026341'));
print_path(Astar('46071276','289899699'));
