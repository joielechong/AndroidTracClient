#! /usr/bin/perl -w

use strict;

use OSM::Map;

use Data::Dumper;

my @bbox = (4.83,52.28,4.88,52.31);

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
    my $map = shift;
    my $start = shift;
    my $goal  = shift;
    my $vehicle = shift;
    
    my %closedset;
    my %openset;
    my %g_score;
    my %h_score;
    my %f_score;
    my %d_score;
    my %came_from;
    
#    return "foutje" unless defined($$nodes{$start}) and defined($$nodes{$goal});
    return "foutje" unless defined($map->node($start)) and defined($map->node($goal));
    
    
    $map->setVehicle($vehicle);
    
#     closedset := the empty set                 % The set of nodes already evaluated.     
#     openset := set containing the initial node % The set of tentative nodes to be evaluated.
    
    $openset{$start} = 1;
    
#     g_score[start] := 0                        % Distance from start along optimal path.
#     h_score[start] := heuristic_estimate_of_distance(start, goal)
#     f_score[start] := h_score[start]           % Estimated total distance from start to goal through y.
    
    $g_score{$start} = 0;
    $f_score{$start} = $h_score{$start} = $map->calc_h_score($start,$goal,$vehicle);
    $d_score{$start} = $map->distance($start,$goal);
    
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
	if ($x == $goal or $d_score{$x}<10) {
	    my @p = reconstruct_path(\%came_from,$x);
#	    print Dumper \%d_score;
	    return @p;
	}
#         remove x from openset
	delete($openset{$x});
#         add x to closedset
	$closedset{$x} = 1;
#	print "close $x\n";
#         foreach y in neighbor_nodes(x)
#	for my $y (keys(%{$$way{$x}})) {
	for my $y ($map->neighbours($x)) {
#             if y in closedset
#                 continue
	    next if (defined($closedset{$y}));
#             tentative_g_score := g_score[x] + dist_between(x,y)
	    my $tentative_g_score = $g_score{$x} + $map->cost($x,$y);
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
#                 f_score[y] := g_score[y] r+ h_score[y]
	    if ($tentative_is_better) {
		$came_from{$y} = $x;
		$g_score{$y} = $tentative_g_score;
		$h_score{$y} = $map->calc_h_score($y,$goal);
		$d_score{$y} = $map->distance($y,$goal);
		$f_score{$y} = $g_score{$y}+$h_score{$y};
#		print "$x $y ",$g_score{$y}," ",$h_score{$y}," ",$f_score{$y},"\n";
#		print "openset=",join(", ",keys(%openset)),"\n";
	    }
	}
    }
#     return failure
    return "foutje";
}

sub print_path {
    my $map = shift;
    my @p1=@_;
    print join(", ",@p1);
    print " ",$map->node($p1[0])->{length} if defined($map->node($p1[0])->{length});
    print "\n";
    my $oldp;
    my $oldw;
    for my $p (@p1) {
        my $w;
	print "$p -> ";
	my @ws = $map->getways($p);
	if ($#ws == 0) {
	    $w = $ws[0];
	} else {
	    if (defined($oldp)) {
	        $w=$map->getway($oldp,$p);
	    }
	}
	if (defined($w)) {
	    print $w,": ";
	    print $map->way($w)->{tag}->{name}," " if defined($map->way($w)->{tag}->{name});
	    print $map->way($w)->{tag}->{highway}," " if defined($map->way($w)->{tag}->{highway});
	    print $map->way($w)->{tag}->{maxspeed}," " if defined($map->way($w)->{tag}->{maxspeed});
            $oldw=$w;
	}
	$oldp=$p;
	print "\n";
    }
}

my $arg = shift;
my $map = OSM::Map->new();
if (defined($arg) && ($arg eq "net")) {
    $map->useNetdata(@bbox);
} else {
    $map->useLocaldata("map.osm");
}

print_path($map,Astar($map,'46071276','294062118'));
print_path($map,Astar($map,'46070723','294062118'));
print_path($map,Astar($map,'46070723','294062118','foot'));
print_path($map,Astar($map,'46070723','294062118','bicycle'));
print_path($map,Astar($map,'46070723','294062118','car'));
print_path($map,Astar($map,'46071276','46051999'));
print_path($map,Astar($map,'46070723','46051999'));
print_path($map,Astar($map,'46070723','46026341','bicycle'));
print_path($map,Astar($map,'46071276','46026341','car'));
print_path($map,Astar($map,'46070723','294062118','car'));
print_path($map,Astar($map,'46071276','289899699'));
print_path($map,Astar($map,'46071276','289899699','foot'));
print_path($map,Astar($map,'46071276','289899699','bicycle'));
print_path($map,Astar($map,'46071276','289899699','car'));
$map->saveOSMdata();
