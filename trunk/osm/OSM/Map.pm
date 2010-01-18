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
    my $bounds;
    my $dist;
    my $way;
    
    sub setVehicle {
        my $self = shift;
	$vehicle = shift;
    }
    
    sub node {
        my $this = shift;
	my $n = shift;
	return $$nodes{$n};
    }
    
    sub way {
	my $this = shift;
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
        return exists($w->{tag}->{highway});
    }
    
    sub distance {
        my $self = shift;
        my $n1 = shift;
        my $n2 = shift;
        return $geo->distance('meter',$$nodes{$n1}->{lon},$$nodes{$n1}->{lat}=>$$nodes{$n2}->{lon},$$nodes{$n2}->{lat});
    }
    
    sub procesdata {
        my $this = shift;
	my $doc = shift;

        $nodes = $doc->{node};
        $ways = $doc->{way};
	$bounds = $doc->{bounds};
#        print Dumper($ways);
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
    }
    
    sub saveOSMdata {
        my $doc;
	$doc->{node}=$nodes;
	$doc->{way}=$ways;
	$doc->{bounds} = $bounds;
	if (open NEW,">saveddata.osm") {
	    print NEW XMLout($doc, KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
	    close NEW;
	}
    }

    sub loadOSMdata {
        my $this = shift;
	my $data = shift;
        return XMLin($data, ForceArray=>['tag'],KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
    }

    sub useLocaldata {
        my $this =  shift;
	my $filename = shift;
	$filename = "map.osm" unless defined $filename;
	my $doc = $this->loadOSMdata($filename);
        $this->procesdata($doc);
    }
    
    sub useNetdata {
        my $this =  shift;
	my @bbox = @_;
        return -1  if $#bbox != 3 ;
    
        my $url = $getmapcmd.join(",",@bbox);
        print "url = $url\n";
        my $req = HTTP::Request->new(GET =>$url);
        my $result = $ua->request($req);
        print "Data is binnengehaald\n";
        return -1 unless $result->code == 200;
	if (open NEW,">newmap.osm") {
	    print NEW $result->content;
	    close NEW;
	}
	my $doc = $this->loadOSMdata($result->content);
        $this->procesdata($doc);
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
    my $cw = $$ways{$w}->{tag}->{cycleway};
    my $fa = $$ways{$w}->{tag}->{foot};
    my $ca = $$ways{$w}->{tag}->{bicycle};
    my $onew = $$ways{$w}->{tag}->{oneway};
    my $access = $$ways{$w}->{tag}->{access};

    if (defined($$ways{$w}->{maxspeed})) {
	$speed = $$ways{$w}->{maxspeed} if $$ways{$w}->{maxspeed} < $speed;
    } else {
	my $defspeed = $highways{$hw}->{speed};
	$speed = $defspeed if $defspeed < $speed;
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
	if (defined($$nodes{$y}->{highway})) {
	    $extracost += $highways{$$nodes{$y}->{highway}};
	}
    } elsif ($vehicle eq "car") {
        return $infinity if defined($access) and $access eq "no";
	$extracost += 10 if defined($$nodes{$y}->{traffic_calming});
	return $infinity if defined($onew) and $self->wrong_direction($x,$y,$w,$onew);
        return $infinity unless (defined $profiles{$vehicle}->{allowed}->{$hw});
	if (defined($$nodes{$y}->{highway})) {
	    $extracost += $highways{$$nodes{$y}->{highway}};
	}
    } else {
        die "Onbekend voertuig\n";
    }
	
    if (defined($$nodes{$y}->{highway}) and $$nodes{$y}->{highway} eq 'traffic_signals') {
	$extracost += $highways{$$nodes{$y}->{highway}};
    }
    if (defined($$nodes{$y}->{highway})) {
	$extracost += $highways{$$nodes{$y}->{highway}};
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
