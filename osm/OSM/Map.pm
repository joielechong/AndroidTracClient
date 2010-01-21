{
    package OSM::Map;
    
    use strict;
    use vars qw(@ISA $VERSION);
    
    use LWP::UserAgent;
    use Data::Dumper;
    use XML::Simple;
    use Geo::Distance;
    
    require Exporter;
    @ISA = qw(Exporter);
    
    my $getmapcmd;
    my $infinity;
    my $geo;
    my $ua;
    
    BEGIN  {
        $OSM::Map::VERSION = "0.1";
        $XML::Simple::PREFERRED_PARSER = "XML::Parser";
        $getmapcmd ="http://api.openstreetmap.org/api/0.6/map?bbox=";
        $infinity = 9999999999;
        $geo=new Geo::Distance;
        $ua = LWP::UserAgent->new;
    }
    
    my $vehicle;
    my %highways;
    my %profiles;
    my $nodes;
    my $ways;
    my @bounds;
    my $dist;
    my $way;
    
    sub setVehicle {
        my $self = shift;
	$vehicle = shift;
    }
    
    sub node {
        my $self = shift;
	my $n = shift;
	return $$nodes{$n};
    }
    
    sub way {
	my $self = shift;
	my $n = shift;
	return $$ways{$n};
    }
    
    sub new {
        my $this = shift;
	my $conffile = shift;
	my $class = ref($this) || $this;
	my $self = {};
	bless $self, $class;
	$self->initialize($conffile);
	return $self;
    }
    
    sub initialize {
        my $self = shift;
	
	my $conffile = shift;
	$conffile = "astarconf.xml" unless defined $conffile;
	
        my $conf = XMLin($conffile,ForceArray=>['highway','profile'],KeyAttr=>{allowed=>'highway',profile=>'name',highway=>'name'});
#        print Dumper($conf);
        %profiles=%{${$$conf{profiles}}{profile}};
        %highways=%{${$$conf{highways}}{highway}};
    }
    
    sub usable_way {
        my $w = shift;
        return exists($w->{tag}->{highway}) || (exists($w->{tag}->{route}) and $w->{tag}->{route} eq "ferry");
    }
    
    sub distanceCoor {
        my ($self,$lat1,$lon1,$lat2,$lon2) = @_;
        return $geo->distance('meter',$lon1,$lat1,$lon2,$lat2);
    }
    
    sub distance {
        my $self = shift;
        my $n1 = shift;
        my $n2 = shift;
        return $geo->distance('meter',$$nodes{$n1}->{lon},$$nodes{$n1}->{lat}=>$$nodes{$n2}->{lon},$$nodes{$n2}->{lat});
    }
    
    sub store {
	my ($self,$newnodes,$newways,$newbounds) = @_;
	for my $n (keys %$newnodes) {
	    $$nodes{$n} = $$newnodes{$n} unless exists($$nodes{$n});
	}
	for my $w (keys %$newways) {
	    $$ways{$w} = $$newways{$w} unless exists($$ways{$w});
	}
	push @bounds,$newbounds;
    }
    
    sub inboundCoor {
        my ($self,$lat,$lon) = @_;
	
	for my $b (@bounds) {
	   return 1 if ($lat<=$b->{maxlat} && $lat >=$b->{minlat} && $lon<=$b->{maxlon} && $lon>=$b->{minlon});
	}
	return 0;
    }
    
    sub inboundNode {
        my ($self,$node) = @_;
	my $lat = $$nodes{$node}->{lat};
	my $lon = $$nodes{$node}->{lon};
	return $self->inboundCoor($lat,$lon);
    }
    
    sub procesdata {
        my $self = shift;
	my $doc = shift;
	my $nodes;
	my $ways;
	my $bounds;

        $nodes = $doc->{node};
        $ways = $doc->{way};
	$bounds = $doc->{bounds};
#        print Dumper($ways);
        my $nrnodes;
	
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
#            print Dumper($ways->{$w}->{nd});
            $nrnodes = $#{$ways->{$w}->{nd}}+1;
            my ($n1,$n2);
            for (my $i=0;$i<$nrnodes-1;$i++) {
	        $n1 = $ways->{$w}->{nd}->[$i]->{ref};
    	        $n2 = $ways->{$w}->{nd}->[$i+1]->{ref};
	        $way->{$n1}->{$n2}=$w;
	        $way->{$n2}->{$n1}=$w;
            }
        }
	
        foreach my $n (keys %$nodes) {
            delete $$nodes{$n} unless exists($way->{$n});
        }
	$self->store($nodes,$ways,$bounds);
    }
    
    sub saveOSMdata {
        my $doc;
	$doc->{node}=$nodes;
	$doc->{way}=$ways;
	$doc->{bounds}=\@bounds;
	if (open NEW,">saveddata.osm") {
	    print NEW XMLout($doc, KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
	    close NEW;
	}
    }

    sub loadOSMdata {
        my $self = shift;
	my $data = shift;
        return XMLin($data, ForceArray=>['tag'],KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
    }

    sub useLocaldata {
        my $self =  shift;
	my $filename = shift;
	$filename = "map.osm" unless defined $filename;
	my $doc = $self->loadOSMdata($filename);
        $self->procesdata($doc);
    }
    
    sub useNetdata {
        my $self =  shift;
	my @bbox = @_;
        return -1  if $#bbox != 3 ;
    
        my $url = $getmapcmd.join(",",@bbox);
        print "url = $url\n";
        my $req = HTTP::Request->new(GET =>$url);
        my $result = $ua->request($req);
        return -1 unless $result->code == 200;
#	if (open NEW,">newmap.osm") {
#	    print NEW $result->content;
#	    close NEW;
#	}
	my $doc = $self->loadOSMdata($result->content);
        $self->procesdata($doc);
	print "\n";
    }
    
    sub fetchCoor {
	my ($self,$lat,$lon) = @_;
	my $minlon=$lon-0.025;
	my $maxlon=$lon+0.025;
	my $minlat=$lat-0.025;
	my $maxlat=$lat+0.025;
	for my $b (@bounds) {
            if ($minlat<=$b->{maxlat} && $minlat >=$b->{minlat} && $minlon<=$b->{maxlon} && $minlon>=$b->{minlon}) {
	        if ($lon > $b->{maxlon}) {
    	            $minlon = $b->{maxlon};
		} else { 
		    $minlat = $b->{maxlat};
		}
	    }
            if ($maxlat<=$b->{maxlat} && $maxlat >=$b->{minlat} && $minlon<=$b->{maxlon} && $minlon>=$b->{minlon}) {
	        if ($lon > $b->{maxlon}) {
    	            $minlon = $b->{maxlon};
		} else { 
		    $maxlat = $b->{minlat};
		}
	    }
            if ($minlat<=$b->{maxlat} && $minlat >=$b->{minlat} && $maxlon<=$b->{maxlon} && $maxlon>=$b->{minlon}) {
	        if ($lon < $b->{minlon}) {
    	            $maxlon = $b->{minlon};
		} else { 
		    $minlat = $b->{maxlat};
		}
	    }
            if ($maxlat<=$b->{maxlat} && $maxlat >=$b->{minlat} && $maxlon<=$b->{maxlon} && $maxlon>=$b->{minlon}) {
	        if ($lon < $b->{minlon}) {
    	            $maxlon = $b->{minlon};
		} else { 
		    $maxlat = $b->{minlat};
		}
	    }
	}
	my @bbox = ($minlon,$minlat,$maxlon,$maxlat);
	$self->useNetdata(@bbox);
    }
    
    sub findNode {
        my ($self,$lat,$lon) = @_;
	my $node = undef;
	my $distance=$infinity;
	
	for my $n (keys %$nodes) {
	        my $d = $self->distanceCoor($lat,$lon,$$nodes{$n}->{lat},$$nodes{$n}->{lon});
		if ($d < $distance) {
		    $distance=$d;
		    $node = $n;
		}
	}
	return $node;
    }

    sub fetchNode {
	my ($self,$node) = @_;
	my $lat = $$nodes{$node}->{lat};
	my $lon = $$nodes{$node}->{lon};
	$self->fetchCoor($lat,$lon);
    }

    sub calc_h_score {
        my $self = shift;
        my $x = shift;
        my $y = shift;
    
        my $d=$self->distance($x,$y);
        return defined($vehicle) ? $d *3.6/$profiles{$vehicle}->{maxspeed} : $d;
    }

    sub wrong_direction {
    my ($self,$x,$y,$w,$onew) = @_;
    my @nd = @{$$ways{$w}{nd}};
    
    foreach my $n (@nd) {
	if ($n->{ref} == $y) {
	    return ($onew ne "rev");
	}
	if ($n->{ref} == $x) {
	    return ($onew eq "rev");
	}
    }
    die "nodes not found in wrong direction $x $y $w\n";
    }

    sub cost {
    my $self=shift;
    my $x = shift;
    my $y = shift;
    
    my $d = $dist->{$x}->{$y};
    $d = $dist->{x}->{$y} = $dist->{$y}->{$x} = $self->distance($x,$y) unless defined($d);
    return $d unless defined($vehicle);
    my $speed = $profiles{$vehicle}->{maxspeed};
    my $w = $way->{$x}->{$y};
    my $hw = $$ways{$w}->{tag}->{highway};
    $hw = "unclassified" if !defined($hw) && defined($$ways{$w}->{tag}->{route}) && $$ways{$w}->{tag}->{route} eq "ferry";
    my $cw = $$ways{$w}->{tag}->{cycleway};
    my $fa = $$ways{$w}->{tag}->{foot};
    my $ca = $$ways{$w}->{tag}->{bicycle};
    my $onew = $$ways{$w}->{tag}->{oneway};
    my $access = $$ways{$w}->{tag}->{access};

    if (defined($$ways{$w}->{maxspeed})) {
	$speed = $$ways{$w}->{maxspeed} if $$ways{$w}->{maxspeed} < $speed;
    } elsif (defined($highways{$hw}->{speed})) {
	my $defspeed = $highways{$hw}->{speed};
	$speed = $defspeed if $defspeed < $speed;
    } else {
	$self->saveOSMdata();
        print "Geen snelheid voor $hw op weg $w\n";
	return $infinity;
    }
    my $cost = $d * 3.6 / $speed;
    my $extracost = $profiles{$vehicle}->{allowed}->{$hw}->{extracost};
    $extracost = 0 unless defined $extracost;
	

    if ($vehicle eq "foot") {
        if (defined($fa)) {
	     return $infinity if $fa eq "no";
	} elsif (defined($access)) {
	    return $infinity if $access eq "no";
	} elsif (!defined($profiles{$vehicle}->{allowed}->{$hw})) {
            return $infinity;
	}
    } elsif ($vehicle eq "bicycle") {
        if (defined($ca)) {
	     return $infinity if $ca eq "no";
	} elsif (defined($access)) {
	    return $infinity if $access eq "no";
	} elsif (!defined($profiles{$vehicle}->{allowed}->{$hw})) {
            return $infinity;
	}
	$extracost = 0 if defined($cw);
	$extracost += 5 if defined($$nodes{$y}->{traffic_calming});
	if (defined($onew)) {
	    if (!defined($cw) or $cw ne "opposite") {
		return $infinity if $self->wrong_direction($x,$y,$w,$onew);
	    }
	}
	if (defined($$nodes{$y}->{highway}) and defined($highways{$$nodes{$y}->{highway}}->{extracost})) {
	    $extracost += $highways{$$nodes{$y}->{highway}}->{extracost};
	}
    } elsif ($vehicle eq "car") {
        return $infinity if defined($access) and $access eq "no";
	$extracost += 10 if defined($$nodes{$y}->{traffic_calming});
	return $infinity if defined($onew) and $self->wrong_direction($x,$y,$w,$onew);
        return $infinity unless (defined $profiles{$vehicle}->{allowed}->{$hw});
	if (defined($$nodes{$y}->{highway}) and defined($highways{$$nodes{$y}->{highway}}->{extracost})) {
	    $extracost += $highways{$$nodes{$y}->{highway}}->{extracost};
	}
    } else {
        die "Onbekend voertuig\n";
    }
	
#	print "$x $y $cost $extracost\n";
    return $cost * (100.0 +$extracost)/100.0;
    }
    
    sub neighbours {
        my $self = shift;
	my $x = shift;
	
	return keys(%{$$way{$x}});
    }
    
    sub getway {
        my $self = shift;
	my $p1 = shift;
	my $p2 = shift;
	
	return $way->{$p1}->{$p2};
    }
    
    sub getways {
        my $self = shift;
	my $n = shift;
	
	my @ways = ();
	for my $n1 (keys(%{$$way{$n}})) {
	    push @ways,$way->{$n}->{$n1};
	}
	return @ways;
    }
}

1;
