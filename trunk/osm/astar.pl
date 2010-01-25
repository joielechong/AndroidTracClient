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
    my $goes_to = shift;
    my $current_node = shift;
    
    my @path = ();  
    if (defined($came_from->{$current_node})) {
	@path = reconstruct_path($came_from,undef,$came_from->{$current_node});
    }
    push @path,$current_node;
    my $x = $current_node;
    if (defined($goes_to)) {
	while (exists($$goes_to{$x})) {
	    push @path,$$goes_to{$x};
	    $x=$$goes_to{$x};
	}
    }
    return  @path;
}

sub Astar {
    my $map = shift;
    my $startlat = shift;
    my $startlon = shift;
    my $goallat  = shift;
    my $goallon  = shift;
    my $vehicle = shift;
    
    my %closedset;
    my %goalset;
    my %startset;
    my %g_score;
    my %h_score;
    my %f_score;
    my %d_score;
    my %came_from;
    my %goes_to;
    
#    return "foutje" unless defined($$nodes{$start}) and defined($$nodes{$goal});
 
    $map->fetchCoor($startlat,$startlon,1) unless $map->inboundCoor($startlat,$startlon);
    $map->fetchCoor($goallat,$goallon,1) unless $map->inboundCoor($goallat,$goallon);
    my $start = $map->findNode($startlat,$startlon);
    my $goal = $map->findNode($goallat,$goallon);
    print "start = $start, goal = $goal ".(defined($vehicle)?$vehicle:"")."\n";
    
    $map->initRoute($vehicle);
    
#     closedset := the empty set                 % The set of nodes already evaluated.     
#     openset := set containing the initial node % The set of tentative nodes to be evaluated.
    
    $startset{$start} = 1;
    $goalset{$goal} = 1;
    
#     g_score[start] := 0                        % Distance from start along optimal path.
#     h_score[start] := heuristic_estimate_of_distance(start, goal)
#     f_score[start] := h_score[start]           % Estimated total distance from start to goal through y.
    
    $g_score{$start} = 0;
    $f_score{$start} = $h_score{$start} = $map->calc_h_score($start,$goal,$vehicle);
    $d_score{$start} = $map->distance($start,$goal);
    
    $g_score{$goal} = 0;
    $f_score{$goal} = $h_score{$goal} = $map->calc_h_score($goal,$start,$vehicle);
    $d_score{$goal} = $map->distance($goal,$start);

#
# de twee sets moeten nog uitgebreid cq vervangen door een set die uitgaat van de werkelijke positie en locatie op een weg vlakbij en niet de dichstbijzijnde node.
# dit vraagt het toevoegen van tijdelijke nodes en wegen. en dus een initiele set die mogelijk meer dan 1 node bevat.
# er zou in een straal van x m om het punt gezocht moeten worden, mogelijk dat de straal zich uitbreid. Probleem is nog hoe je dat snel kan doen.
#

    
#     while openset is not empty
    while (keys(%startset) != 0 or keys(%goalset) != 0) {
#         x := the node in openset having the lowest f_score[] value
	my $xs = -1;
	
	for my $k (keys(%startset)) {
	    $xs=$k if $xs == -1;
	    $xs=$k if ($f_score{$k} < $f_score{$xs});
	}
	if ($xs != -1) {
#         if x = goal
#             return reconstruct_path(came_from,goal)
#	if ($xs == $goal or $d_score{$xs}<10) {
#
# Dit is dus te simpel. Het is niet gegarandeerd dat de gevonden node ook werkelijk die van de beste route is, zelfs als deze op dat moment de laagste f_score heeft, kan er og een beter pad zijn. Mogelijk moeten we in de closed set zoeken.
# Hoe langer de route hoe groter de kans dat het goed gaat.
# Wat nu gebeurt is dat de twee langs elkaar heen werken en er toevallig een verbinding wordt gevonden.
#
	if (exists($goalset{$xs})) {
	    my @p = reconstruct_path(\%came_from,\%goes_to,$xs);
	    my $scores;
	    $scores->{d_score}=\%d_score;
	    $scores->{g_score}=\%g_score;
	    $scores->{h_score}=\%h_score;
	    $scores->{f_score}=\%f_score;
#	    print Dumper $scores;
	    return @p;
	}
#         remove x from openset
	delete($startset{$xs});
#         add x to closedset
	$closedset{$xs} = 1;
#	print "close $xs ".$f_score{$xs}."\n";
#         foreach y in neighbor_nodes(x)
#	for my $y (keys(%{$$way{$x}})) {
	for my $y ($map->neighbours($xs)) {
	    $map->fetchNode($y) unless $map->inboundNode($y);
#             if y in closedset
#                 continue
	    next if (defined($closedset{$y}));
#             tentative_g_score := g_score[x] + dist_between(x,y)
	    my $tentative_g_score = $g_score{$xs} + $map->cost($xs,$y);
# 
#             if y not in openset
#                 add y to openset
	    my $tentative_is_better;
	    unless (defined($startset{$y})) {
		$startset{$y} = 1 ;
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
		$came_from{$y} = $xs;
		$g_score{$y} = $tentative_g_score;
		$h_score{$y} = $map->calc_h_score($y,$goal);
		$d_score{$y} = $map->distance($y,$goal);
		$f_score{$y} = $g_score{$y}+$h_score{$y};
		print "strt $xs $y ",$g_score{$y}," ",$h_score{$y}," ",$f_score{$y},"\n";
#		print "openset=",join(", ",keys(%openset)),"\n";
	    }
	}
	}
	
	$xs = -1;
	
	for my $k (keys(%goalset)) {
	    $xs=$k if $xs == -1;
	    $xs=$k if ($f_score{$k} < $f_score{$xs});
	}
	if ($xs != -1) {
#         if x = goal
#             return reconstruct_path(came_from,goal)
#	if ($xs == $goal or $d_score{$xs}<10) {
	if (exists($startset{$xs})) {
	    my @p = reconstruct_path(\%came_from,\%goes_to,$xs);
	    my $scores;
	    $scores->{d_score}=\%d_score;
	    $scores->{g_score}=\%g_score;
	    $scores->{h_score}=\%h_score;
	    $scores->{f_score}=\%f_score;
#	    print Dumper $scores;
	    return @p;
	}
#         remove x from openset
	delete($goalset{$xs});
#         add x to closedset
	$closedset{$xs} = 1;
#	print "close $xs ".$f_score{$xs}."\n";
#         foreach y in neighbor_nodes(x)
#	for my $y (keys(%{$$way{$x}})) {
	for my $y ($map->neighbours($xs)) {
	    $map->fetchNode($y) unless $map->inboundNode($y);
#             if y in closedset
#                 continue
	    next if (defined($closedset{$y}));
#             tentative_g_score := g_score[x] + dist_between(x,y)
	    my $tentative_g_score = $g_score{$xs} + $map->cost($y,$xs);
# 
#             if y not in openset
#                 add y to openset
	    my $tentative_is_better;
	    unless (defined($goalset{$y})) {
		$goalset{$y} = 1 ;
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
		$goes_to{$y} = $xs;
		$g_score{$y} = $tentative_g_score;
		$h_score{$y} = $map->calc_h_score($start,$y);
		$d_score{$y} = $map->distance($y,$start);
		$f_score{$y} = $g_score{$y}+$h_score{$y};
		print "goal $y $xs ",$g_score{$y}," ",$h_score{$y}," ",$f_score{$y},"\n";
#		print "openset=",join(", ",keys(%openset)),"\n";
	    }
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
	    my $mwt = $map->way($w)->{tag};
	    print $$mwt{name}," " if defined($$mwt{name});
	    print $$mwt{highway}," " if defined($$mwt{highway});
	    print $$mwt{maxspeed}," " if defined($$mwt{maxspeed});
	    print $$mwt{ref}," " if defined($$mwt{ref});
            $oldw=$w;
	}
	$oldp=$p;
	print "\n";
    }
}

my $arg = shift;
my $map = OSM::Map->new();
#if (defined($arg) && ($arg eq "net")) {
#    $map->useNetdata(@bbox);
#} else {
#    $map->useLocaldata("map.osm");
#}

#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'car'));
print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588));
print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'foot'));
print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'bicycle'));
print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'car'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2886,4.8508));
#print_path($map,Astar($map,52.297275,4.8616077,52.2886,4.8508,'foot'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2886,4.8508,'bicycle'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2886,4.8508,'car'));
#print_path($map,Astar($map,52.297275,4.8616077,52.311311,4.8468799,'car'));
#print_path($map,Astar($map,52.297275,4.8616077,52.4176385,4.8708703,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.278473,4.846854));
#print_path($map,Astar($map,52.2973969,4.8620826,52.278473,4.846854,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.278473,4.846854,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.278473,4.846854,'car'));
#print_path($map,Astar($map,52.297275,4.8616077,52.4176385,4.8708703,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588));
#print_path($map,Astar($map,52.297275,4.8616077,52.2935821,4.8593675));
#print_path($map,Astar($map,52.297275,4.8616077,52.2935821,4.8593675,'foot'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2935821,4.8593675,'bicycle'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2935821,4.8593675,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2932895,4.8544163));
#print_path($map,Astar($map,52.297275,4.8616077,52.2932895,4.8544163));
#print_path($map,Astar($map,52.297275,4.8616077,52.2871944,4.8503294,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2871944,4.8503294,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'bicycle'));
#print_path($map,Astar($map,52.4184,4.8724,52.2973969,4.8620826,'car'));
#print_path($map,Astar($map,52.4184,4.8724,52.2973969,4.8620826,'bicycle'));
$map->saveOSMdata();

