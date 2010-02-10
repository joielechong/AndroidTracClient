#! /usr/bin/perl -w

use strict;
use OSM::Map;
use Data::Dumper;
use Storable;

my $dbfile = "localdatabase.data";

sub reconstruct_path {
    my $came_from = shift;
    my $goes_to = shift;
    my $current_node = shift;
    
    my @path = ();
    my @rev;
    my $cn = $current_node;
    
    while (defined($came_from->{$cn})) {
	$cn = $came_from->{$cn};
        push @rev,$cn;
    }
    while ($cn=pop(@rev)) {
	push @path,$cn;
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
    
    my (%closedset,%goalset,%startset,%gs_score,%hs_score,%fs_score,%ds_score,%gg_score,%hg_score,%fg_score,%dg_score,%came_from,%goes_to);
    my (@openset,@f,@g,@h,@d,@to,@gl);
    
    $map->initRoute($vehicle);
    
    $map->fetchCoor($startlat,$startlon,1) unless $map->inboundCoor($startlat,$startlon);
    $map->fetchCoor($goallat,$goallon,1) unless $map->inboundCoor($goallat,$goallon);
    my $start = $map->findNode($startlat,$startlon,10);
    my $goal = $map->findNode($goallat,$goallon,10);
    print "start = $start, goal = $goal ".(defined($vehicle)?$vehicle:"")."\n";

    $startset{$start} = 1;
    $goalset{$goal} = 2;
    
    $gs_score{$start} = 0;
    $fs_score{$start} = $hs_score{$start} = $map->calc_h_score($start,$goal,$vehicle);
    $ds_score{$start} = $map->distance($start,$goal);
    
    $gg_score{$goal} = 0;
    $fg_score{$goal} = $hg_score{$goal} = $map->calc_h_score($goal,$start,$vehicle);
    $dg_score{$goal} = $map->distance($goal,$start);
    
    
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
    $gl[1] = $goal;
    $gl[2] = $start;
    
    while (keys(%startset) != 0 or keys(%goalset) != 0) {
	for (my $i=1;$i<=2;$i++) {
	    my $goal = $gl[$i];
	    my $xs = undef;
	    
	    for my $k (keys(%{$openset[$i]})) {
		$xs=$k unless defined($xs);
		$xs=$k if (${$f[$i]}{$k} < ${$f[$i]}{$xs});
	    }
	    if (defined($xs)) {
		if (exists($closedset{$xs}) and $closedset{$xs} != $i) {
		    my @p = reconstruct_path($to[1],$to[2],$xs);
		    return @p;
		}
		delete(${$openset[$i]}{$xs});
		$closedset{$xs} = $i;
#	        print "close $xs ".$f_score{$xs}."\n";                
		for my $y ($map->neighbours($xs)) {
		    $map->fetchNode($y) unless $map->inboundNode($y);
		    next if (defined($closedset{$y}));
                    my $prevnode = ${$to[$i]}{$xs};
		    my $tentative_g_score = ${$g[$i]}{$xs} + ($i==1?$map->cost($xs,$y,$prevnode):$map->cost($y,$xs,$prevnode));
		    my $tentative_is_better;
		    unless (defined(${$openset[$i]}{$y})) {
			${$openset[$i]}{$y} = 1 ;
			$tentative_is_better = 1;
		    } elsif ($tentative_g_score < ${$g[$i]}{$y}) {
			$tentative_is_better = 1;
		    } else {
			$tentative_is_better = 0;
		    }
		    if ($tentative_is_better) {
			${$to[$i]}{$y} = $xs;
			${$g[$i]}{$y} = $tentative_g_score;
			${$h[$i]}{$y} = $map->calc_h_score($y,$goal);
			${$d[$i]}{$y} = $map->distance($y,$goal);
			${$f[$i]}{$y} = ${$g[$i]}{$y}+${$h[$i]}{$y};
#			my $w = $map->getway($xs,$y);
#			print "$i $xs $y ",${$g[$i]}{$y}," ",${$h[$i]}{$y}," ",${$f[$i]}{$y},"\n" if $i==1;
#			print "$i $y $xs ",${$g[$i]}{$y}," ",${$h[$i]}{$y}," ",${$f[$i]}{$y},"\n" if $i==2;
#		        print "openset=",join(", ",keys(%openset)),"\n";
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
    my $dir;
    my $olddir;
    my $totdist=0;
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
	    my $d = $map->dist($oldp,$p);
	    if (defined($d)) {
                $olddir = $dir;
                $dir = $map->direction($oldp,$p);
		print "$dir $d ";
		$totdist += $d;
	    }
	    print $totdist," ";
            $oldw=$w;
            print $map->findLocation($p);
	}
	$oldp=$p;
	print "\n";
    }
}

my $arg = shift;
my $map;
if (defined($arg)) {
    $map = OSM::Map->new();
    if ($arg eq "local") {
        $map->useLocaldata("map.osm");
    }
} else {
    $map = retrieve $dbfile;
    $map->initialize();
}

###huis school
#print_path($map,Astar($map,52.297277,4.862030,52.29334,4.85876));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2933,4.8588,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.2935821,4.8593675,'car'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'foot'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'bicycle'));
#print_path($map,Astar($map,52.297275,4.8616077,52.2933,4.8588,'car'));

###Huis hockey
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

###Coentunnel
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'foot'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.4184,4.8724,'bicycle'));
#print_path($map,Astar($map,52.4184,4.8724,52.2973969,4.8620826,'car'));
#print_path($map,Astar($map,52.4184,4.8724,52.2973969,4.8620826,'bicycle'));

## Croon Delft
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367));
#print_path($map,Astar($map,52.4184,4.8724,51.9972199,4.3855367,'car'));

#I# CT Barendrecht
#print_path($map,Astar($map,52.2973969,4.8620826,51.8503978,4.5091717,'car'));
#print_path($map,Astar($map,51.8503978,4.5091717,52.2973969,4.8620826,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.8503978,4.5091717,'bicycle'));

## ProRail
print_path($map,Astar($map,52.2973969,4.8620826,52.087473,5.115715,'car'));
print_path($map,Astar($map,52.2973969,4.8620826,52.087473,5.115715,'bicycle'));
print_path($map,Astar($map,52.087473,5.115715,52.2973969,4.8620826,'car'));

## Roquebrune
#print_path($map,Astar($map,52.2973969,4.8620826,43.4046930,6.6792379,'car'));

###Brussel
#print_path($map,Astar($map,52.2973969,4.8620826,50.8417207,4.3832422,'car'));

$map->saveOSMdata($dbfile);
store $map,$dbfile;
