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
    my %gs_score;
    my %hs_score;
    my %fs_score;
    my %ds_score;
    my %gg_score;
    my %hg_score;
    my %fg_score;
    my %dg_score;
    my %came_from;
    my %goes_to;
    my @openset;
    my @f;
    my @g;
    my @h;
    my @d;
    my @to;
    
    $map->initRoute($vehicle);
    
    $map->fetchCoor($startlat,$startlon,1) unless $map->inboundCoor($startlat,$startlon);
    $map->fetchCoor($goallat,$goallon,1) unless $map->inboundCoor($goallat,$goallon);
    my $start = $map->findNode($startlat,$startlon);
    my $goal = $map->findNode($goallat,$goallon);
    print "start = $start, goal = $goal ".(defined($vehicle)?$vehicle:"")."\n";
    
#     closedset := the empty set                 % The set of nodes already evaluated.     
#     openset := set containing the initial node % The set of tentative nodes to be evaluated.
    
    $startset{$start} = 1;
    $goalset{$goal} = 2;
    
    $openset[1] = \%startset;
    $openset[2] = \%goalset;
    $f[1] = \%fs_score;
    $f[2] = \%fg_score;
    $g[1] = \%gs_score;
    $g[2] = \%gg_score;
    $h[1] = \%hs_score;
    $h[2] = \%hg_score;
    $d[1] = \%ds_score;
    $d[2] = \%dg_score;
    $to[1] = \%came_from;
    $to[2] = \%goes_to;
    
#     g_score[start] := 0                        % Distance from start along optimal path.
#     h_score[start] := heuristic_estimate_of_distance(start, goal)
#     f_score[start] := h_score[start]           % Estimated total distance from start to goal through y.
    
    $gs_score{$start} = 0;
    $fs_score{$start} = $hs_score{$start} = $map->calc_h_score($start,$goal,$vehicle);
    $ds_score{$start} = $map->distance($start,$goal);
    
    $gg_score{$goal} = 0;
    $fg_score{$goal} = $hg_score{$goal} = $map->calc_h_score($goal,$start,$vehicle);
    $dg_score{$goal} = $map->distance($goal,$start);
    
#
# de twee sets moeten nog uitgebreid cq vervangen door een set die uitgaat van de werkelijke positie en locatie op een weg vlakbij en niet de dichstbijzijnde node.
# dit vraagt het toevoegen van tijdelijke nodes en wegen. en dus een initiele set die mogelijk meer dan 1 node bevat.
# er zou in een straal van x m om het punt gezocht moeten worden, mogelijk dat de straal zich uitbreid. Probleem is nog hoe je dat snel kan doen.
#
    
#     while openset is not empty
    while (keys(%startset) != 0 or keys(%goalset) != 0) {
	for (my $i=1;$i<=2;$i++) {
	    my %came_from = %{$to[$i]};
	    
	    #         x := the node in openset having the lowest f_score[] value
	    my $xs = -1;
	    
	    for my $k (keys(%{$openset[$i]})) {
		$xs=$k if $xs == -1;
		$xs=$k if (${$f[$i]}{$k} < ${$f[$i]}{$xs});
	    }
	    if ($xs != -1) {
#         if x = goal
#             return reconstruct_path(came_from,goal)
#	if ($xs == $goal or $d_score{$xs}<10) {
#
#
		if (exists($closedset{$xs}) and $closedset{$xs} != $i) {
		    my @p = reconstruct_path($to[1],$to[2],$xs);
		    return @p;
		}
#         remove x from openset
		delete(${$openset[$i]}{$xs});
#         add x to closedset
		$closedset{$xs} = $i;
#	print "close $xs ".$f_score{$xs}."\n";
#         foreach y in neighbor_nodes(x)
#	for my $y (keys(%{$$way{$x}})) {
		for my $y ($map->neighbours($xs)) {
		    $map->fetchNode($y) unless $map->inboundNode($y);
#             if y in closedset
#                 continue
		    next if (defined($closedset{$y}));
#             tentative_g_score := g_score[x] + dist_between(x,y)
		    my $tentative_g_score = ${$g[$i]}{$xs} + ($i==1?$map->cost($xs,$y):$map->cost($y,$xs));
# 
#             if y not in openset
#                 add y to openset
		    my $tentative_is_better;
		    unless (defined(${$openset[$i]}{$y})) {
			${$openset[$i]}{$y} = 1 ;
#                 tentative_is_better := true
			$tentative_is_better = 1;
#             elseif tentative_g_score < g_score[y]
		    } elsif ($tentative_g_score < ${$g[$i]}{$y}) {
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
			${$to[$i]}{$y} = $xs;
			${$g[$i]}{$y} = $tentative_g_score;
			${$h[$i]}{$y} = $map->calc_h_score($y,$goal);
			${$d[$i]}{$y} = $map->distance($y,$goal);
			${$f[$i]}{$y} = ${$g[$i]}{$y}+${$h[$i]}{$y};
			print "$i $xs $y ",${$g[$i]}{$y}," ",${$h[$i]}{$y}," ",${$f[$i]}{$y},"\n";
#		print "openset=",join(", ",keys(%openset)),"\n";
		    }
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
if (defined($arg) && ($arg eq "local")) {
    $map->useLocaldata("map.osm");
}

#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'car'));
print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'foot'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'bicycle'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'car'));
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

