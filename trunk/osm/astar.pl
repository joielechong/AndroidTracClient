#! /usr/bin/perl -w

use strict;
use OSM::Map;
use Getopt::Long;
use Data::Dumper;
use Storable;

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
    
    $gg_score{$goal} = $gs_score{$start} = 0;
    $fg_score{$goal} = $hg_score{$goal} = $fs_score{$start} = $hs_score{$start} = $map->calc_h_score($start,$goal,$vehicle);
    $dg_score{$goal} = $ds_score{$start} = $map->distance($start,$goal);    
    
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
		for my $y (@{$map->neighbours($xs)}) {
                    my $xs1 = $xs;
                    my $prevnode = ${$to[$i]}{$xs1};
		    $map->fetchNode($y) unless $map->inboundNode($y);
		    next if (defined($closedset{$y}));
                    my @ynb = @{$map->neighbours($y)};
                    while ($#ynb == 1) {
#                        print "Proceeding on $y from $xs1".(defined($prevnode) ?" and $prevnode":"")."\n";
                        ${$g[$i]}{$y} = ${$g[$i]}{$xs1} + ($i==1?$map->cost($xs1,$y,$prevnode): $map->cost($y,$xs1,$prevnode));
                        if ($ynb[0] == $xs1) {
                            ${$to[$i]}{$y} = $xs1;
                            $prevnode=$xs1;
                            $xs1=$y;
                            $y = $ynb[1];
                        } else {
                            ${$to[$i]}{$y} = $xs1;
                            $prevnode=$xs1;
                            $xs1=$y;
                            $y = $ynb[0];
                        }
                        $closedset{$xs1}=$i;
  		        $map->fetchNode($y) unless $map->inboundNode($y);
		        last if (defined($closedset{$y}));
                        @ynb = @{$map->neighbours($y)};
                    }
                    next if (defined($closedset{$y}));
		    my $tentative_g_score = ${$g[$i]}{$xs1} + ($i==1?$map->cost($xs1,$y,$prevnode):$map->cost($y,$xs1,$prevnode));
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
			${$to[$i]}{$y} = $xs1;
			${$g[$i]}{$y} = $tentative_g_score;
			${$h[$i]}{$y} = $map->calc_h_score($y,$goal);
			${$d[$i]}{$y} = $map->distance($y,$goal);
			${$f[$i]}{$y} = ${$g[$i]}{$y}+${$h[$i]}{$y};
#			my $w = $map->getway($xs,$y);
#			print "$i $xs $y ",${$g[$i]}{$y}," ",${$h[$i]}{$y}," ",${$f[$i]}{$y},"\n" if $i==1;
#			print "$i $y $xs ",${$g[$i]}{$y}," ",${$h[$i]}{$y}," ",${$f[$i]}{$y},"\n" if $i==2;
		    }
		}
	    }
	    print "\r$xs ",${$d[$i]}{$xs};
#            print "$i xs=$xs openset=",join(", ",keys(%{$openset[$i]})),"\n";
#	     dump_openset($map,$openset[$i]);
	}
    }
    
#     return failure
    open FAIL,">astar.err.log";
    print FAIL "----------------------------\nOpenset\n----------------------------\n";
    print FAIL Dumper \@openset;
    print FAIL "----------------------------\nClosedset\n----------------------------\n";
    print FAIL Dumper \%closedset;
    print FAIL "----------------------------\nf\n----------------------------\n";
    print FAIL Dumper \@f;
    print FAIL "----------------------------\ng\n----------------------------\n";
    print FAIL Dumper \@g;
    print FAIL "----------------------------\nh\n----------------------------\n";
    print FAIL Dumper \@h;
    print FAIL "----------------------------\nto\n----------------------------\n";
    print FAIL Dumper \@to;
    print FAIL "----------------------------\ngl\n----------------------------\n";
    print FAIL Dumper \@gl;
    close FAIL;
    die "foutje";
}

my $filnum=0;
sub dump_openset {
	my $map = shift;
	my $openset = shift;
	
	my $f=$filnum++;
	$filnum %=10;
	if (open UIT,">astardmp-$filnum.csv") {
	        foreach my $o (keys(%{$openset})) {
	            printf UIT "%9.5f\t%9.5f\t%d\n",$map->getCoor($o),$o;
	    }
	    close UIT;
	}
}

sub print_path {
    my $map = shift;
    
    open PP,">>route.log";
    print PP "Route log:\n\n";
    
    my @p1=@_;
    print join(", ",@p1),"\n";
    print PP join(", ",@p1),"\n";
#    print " ",$map->node($p1[0])->{length} if defined($map->node($p1[0])->{length});

    my $oldp;
    my $oldw;
    my $dir;
    my $olddir;
    my $totdist=0;
    for my $p (@p1) {
        my $w;

	print "$p -> ";
	print PP "$p -> ";
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
	    print PP $w,": ";
            my $ww=$map->loadway($w);
	    my $mwt = $ww->{tag};
	    print $$mwt{name}," " if defined($$mwt{name});
	    print $$mwt{highway}," " if defined($$mwt{highway});
	    print $$mwt{maxspeed}," " if defined($$mwt{maxspeed});
	    print $$mwt{ref}," " if defined($$mwt{ref});
	    print PP $$mwt{name}," " if defined($$mwt{name});
	    print PP $$mwt{highway}," " if defined($$mwt{highway});
	    print PP $$mwt{maxspeed}," " if defined($$mwt{maxspeed});
	    print PP $$mwt{ref}," " if defined($$mwt{ref});
            if (defined($oldp)) {
                my $d = $map->dist($oldp,$p);
	        if (defined($d)) {
                    $olddir = $dir;
                    $dir = $map->direction($oldp,$p);
		    print "$dir $d ";
		    print PP "$dir $d ";
		    $totdist += $d;
	        }
	        print $totdist," ";
	        print PP $totdist," ";
            }
            $oldw=$w;
            my $locstr = $map->findLocation($p);
            print $locstr;
            print PP $locstr;
	}
	$oldp=$p;
	print "\n";
	print PP "\n";
    }
    print PP "\n===============End of log ==================\n\n";
    close PP;
}

my $new=0;
my $dbonly=0;
my $netonly=0;
my $confile='astarconf.xml';
my $mapfile = undef;
my $bbox = undef;
my $directory;

my %options = ('new' => \$new,
               'dbonly' => \$dbonly,
               'net' => \$netonly,
               'conf=s' => \$confile,
               'map=s'=>\$mapfile,
               'dir=s'=>\$directory,
               'bbox=s'=>\$bbox);
               
my $result = GetOptions(%options);

my $map = OSM::Map->new();

my @files = ();
if (defined($mapfile)) {
    $map->importOSMfile($mapfile);
} elsif (defined ($bbox)) {
    $map->importBbox($bbox);
} elsif (defined($directory)) {
    opendir(my $dh, $directory) || die "can't opendir $directory: $!";
    @files = grep { /map_.*\.osm$/ && -f "$directory/$_" } readdir($dh);
    map {s:^:$directory/: => $_} @files;
    closedir $dh;
    foreach my $f (@files) {
        $map->importOSMfile("$f");
    }
}

unless ($dbonly) {
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

###Huis atletiek
#print_path($map,Astar($map,52.297275,4.8616077,52.28728,4.85771));
#print_path($map,Astar($map,52.297275,4.8616077,52.28728,4.85771,'foot'));
#print_path($map,Astar($map,52.297275,4.8616077,52.28728,4.85771,'bicycle'));
#print_path($map,Astar($map,52.297275,4.8616077,52.28728,4.85771,'car'));

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
print_path($map,Astar($map,52.2973969,4.8620826,52.27909,4.86404,'car'));
print_path($map,Astar($map,52.27909,4.86404,51.9972199,4.3855367,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367,'car'));
#print_path($map,Astar($map,51.9972199,4.3855367,52.2973969,4.8620826,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367,'bicycle'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.9972199,4.3855367));
#print_path($map,Astar($map,52.4184,4.8724,51.9972199,4.3855367,'car'));

## ProRail
#print_path($map,Astar($map,52.2973969,4.8620826,52.087473,5.115715,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,52.087473,5.115715,'bicycle'));
#print_path($map,Astar($map,52.087473,5.115715,52.2973969,4.8620826,'car'));

## ICT Barendrecht
#print_path($map,Astar($map,52.2973969,4.8620826,51.8503978,4.5091717,'car'));
#print_path($map,Astar($map,51.8503978,4.5091717,52.2973969,4.8620826,'car'));
#print_path($map,Astar($map,52.2973969,4.8620826,51.8503978,4.5091717,'bicycle'));

## Roquebrune
#print_path($map,Astar($map,52.2973969,4.8620826,43.4046930,6.6792379,'car'));

###Brussel
#print_path($map,Astar($map,52.2973969,4.8620826,50.8417207,4.3832422,'car'));
}
$map->postprocess();
