{
    package OSM::Map;
    
    use strict;
    use vars qw(@ISA $VERSION);
    
    use LWP::UserAgent;
    use Data::Dumper;
    use XML::Simple;
    use Geo::Distance;
    use Storable;
    
    require Exporter;
    @ISA = qw(Exporter Storable);
    
    my $getmapcmd;
    my $getwaycmd;
    my $getnodecmd;
    my $getrelcmd;
    my $infinity;
    my $geo;
    my $ua;
    my $PI;
    
    BEGIN  {
    }
    
    my $vehicle;
    my %highways;
    my %profiles;
    my $nodes;
    my $ways;
    my @bounds;
    my $dist;
    my $way;
    my @admin;
    
    sub initRoute {
        my $self = shift;
	$vehicle = shift;
        $self->removetempways();
        $self->removetempnodes();
	if (defined($vehicle)) {
	    die "Geen profile voor $vehicle\n" unless defined($profiles{$vehicle});
	}
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
    
    sub getnodes {
        my ($self,$w) = @_;
        
        return @{$$ways{$w}->{nd}};
    }
    
    sub new {
        my $this = shift;
	my $conffile = shift;
	my $class = ref($this) || $this;
	my $self = {};
	bless $self, $class;
        $self->{nodes} = undef;
        $self->{ways} = undef;
        $self->{dist} = undef;
        $self->{way} = undef;
        $self->{bounds} = [];
        $self->{admin} = [];
	$self->initialize($conffile);
	return $self;
    }
    
    sub initialize {
        my $self = shift;
	my $conffile = shift;
	
        $OSM::Map::VERSION = "0.1";
        $XML::Simple::PREFERRED_PARSER = "XML::Parser";
        $getmapcmd ="http://api.openstreetmap.org/api/0.6/map?bbox=";
        $getwaycmd ="http://api.openstreetmap.org/api/0.6/way";
        $getrelcmd ="http://api.openstreetmap.org/api/0.6/relation/%s/full";
        $getnodecmd ="http://api.openstreetmap.org/api/0.6/node";
        $infinity = 9999999999;
        $geo=new Geo::Distance;
        $ua = LWP::UserAgent->new;
        $PI = 3.14159265358979;

	$conffile = "astarconf.xml" unless defined $conffile;
	
        my $conf = XMLin($conffile,ForceArray=>['highway','profile'],KeyAttr=>{allowed=>'highway',profile=>'name',highway=>'name'});
#        print Dumper($conf);
        %profiles=%{${$$conf{profiles}}{profile}};
        %highways=%{${$$conf{highways}}{highway}};
	$self->{tempways} = 0;
	$self->{tempnodes} = 0;
        $nodes = $$self{nodes};
        $ways = $$self{ways};
        $dist = $$self{dist};
        $way = $$self{way};
        @bounds = @{$$self{bounds}};
        @admin = @{$$self{admin}};
        $self->{changed} = 0;
    }
    
    sub usable_way {
        my $w = shift;
        return exists($w->{tag}->{highway}) || (exists($w->{tag}->{route}) and $w->{tag}->{route} eq "ferry");
    }
    
    sub distanceCoor {
        my ($self,$lat1,$lon1,$lat2,$lon2,$n) = @_;
	print "$n\n",Dumper $$nodes{$n} unless defined($lat2);
        return $geo->distance('meter',$lon1,$lat1,$lon2,$lat2);
    }
    
    sub distance {
        my $self = shift;
        my $n1 = shift;
        my $n2 = shift;
        return $geo->distance('meter',$$nodes{$n1}->{lon},$$nodes{$n1}->{lat}=>$$nodes{$n2}->{lon},$$nodes{$n2}->{lat});
    }
    
    sub storenewdata {
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
	my $newnodes;
	my $newways;
	my $relations;
	my $bounds;
	
        $newnodes = $doc->{node};
        $newways = $doc->{way};
        $relations = $doc->{relation};
	$bounds = $doc->{bounds};
#        print Dumper($ways);
        my $nrnodes;
	
        $self->{changed} = 1;
	$self->process_relations($relations,$newways,$newnodes);
        
        foreach my $w (keys %$newways) {
            unless (usable_way($newways->{$w})) {
	        delete($$newways{$w});
	        next;
            }
	    delete $$newways{$w}->{user};
	    delete $$newways{$w}->{uid};
	    delete $$newways{$w}->{visible};
	    delete $$newways{$w}->{changeset};
            my $oneway = $newways->{$w}->{tag}->{oneway};
            $oneway = "no" unless defined $oneway;
            $oneway = "yes" if $oneway eq "true";
            $oneway = "yes" if $oneway eq "1";
            $oneway = "rev" if $oneway eq "-1";
            if ($oneway eq "no") {
	        delete $newways->{$w}->{tag}->{oneway} if exists($newways->{$w}->{tag}->{oneway});
            } else {
                $newways->{$w}->{tag}->{oneway} = $oneway;
            }
#           print Dumper($ways->{$w}->{nd});
            $nrnodes = $#{$newways->{$w}->{nd}}+1;
            my ($n1,$n2);
            for (my $i=0;$i<$nrnodes-1;$i++) {
	        $n1 = $newways->{$w}->{nd}->[$i]->{ref};
    	        $n2 = $newways->{$w}->{nd}->[$i+1]->{ref};
	        $way->{$n1}->{$n2}=$w;
	        $way->{$n2}->{$n1}=$w;
            }
        }

        foreach my $n (keys %$newnodes) {
            if (exists($way->{$n})) {
	        delete $$newnodes{$n}->{user};
	        delete $$newnodes{$n}->{changeset};
	        delete $$newnodes{$n}->{version};
	        delete $$newnodes{$n}->{visible};
	        delete $$newnodes{$n}->{uid};
	    } else {
	        delete $$newnodes{$n};
	    }
        }
	$self->storenewdata($newnodes,$newways,$bounds);
    }
    
    sub process_relations {
        my ($self,$relations,$newways,$newnodes) = @_;
    	foreach my $r (keys %$relations) {
            my $type = $$relations{$r}->{tag}->{type};
            if (defined($type)) {
                print "relation $r heeft type $type\n" unless ($type eq "multipolygon") or ($type eq "boundary") or ($type eq "route") or ($type eq "restriction");
            } else {
                print "relation $r heeft geen type\n";
            }
	    next unless defined($type);
	    next unless ($type eq "multipolygon") or ($type eq "boundary");
            my $level = $$relations{$r}->{tag}->{admin_level};
	    next unless defined($level);
            my $name = $$relations{$r}->{tag}->{name};
            next unless defined($name);
            next if exists($admin[$level]->{$name});
#            print Dumper \$$relations{$r};
            print $$relations{$r}->{tag}->{name},' ',$$relations{$r}->{tag}->{type},' ',$level,' ',$$relations{$r}->{tag}->{boundary},"\n";
	    $admin[$level]->{$name}->{type} = $type;
            $admin[$level]->{$name}->{lat} = ();
            $admin[$level]->{$name}->{lon} = ();
	    $admin[$level]->{$name}->{minlon} = 1000;
	    $admin[$level]->{$name}->{minlat} = 1000;
	    $admin[$level]->{$name}->{maxlon} = -1000;
	    $admin[$level]->{$name}->{maxlat} = -1000;
            
            my $doc = $self->loadrelation($r);
            my $ways=$doc->{way};
            my $nodes=$doc->{node};
            my $rel=$doc->{relation}->{$r};
            my @members = @{$rel->{member}};
            for (my $m=0;$m<=$#members;$m++) {
                my $w = $members[$m]->{ref};
                my @nds = @{$$ways{$w}->{nd}};
                for (my $n=0;$n<=$#nds;$n++) {
		    my $lon = $$nodes{$nds[$n]->{ref}}->{lon};
		    my $lat = $$nodes{$nds[$n]->{ref}}->{lat};
#                    print Dumper $$nodes{$nds[$n]->{ref}};
                    push @{$admin[$level]->{$name}->{lat}},$lat;
                    push @{$admin[$level]->{$name}->{lon}},$lon;
		    $admin[$level]->{$name}->{minlon} = $lon if $admin[$level]->{$name}->{minlon} > $lon;
		    $admin[$level]->{$name}->{minlat} = $lat if $admin[$level]->{$name}->{minlat} > $lat;
		    $admin[$level]->{$name}->{maxlon} = $lon if $admin[$level]->{$name}->{maxlon} < $lon;
		    $admin[$level]->{$name}->{maxlat} = $lat if $admin[$level]->{$name}->{maxlat} < $lat;
                }
            }
#            print Dumper \@admin;
	}
    }
    
    sub pnpoly {
        my $self = shift;
	my $nvert = shift;
	my $vertx = shift;
	my $verty = shift;
	my $testx = shift;
	my $testy = shift;

	my ($i,$j,$c);
	$c=0;
	$j=$nvert-1;
	for ($i=0;$i<$nvert;$j=$i++) {
            if ( (($$verty[$i]>$testy) != ($$verty[$j]>$testy)) &&
	          ($testx < ($$vertx[$j]-$$vertx[$i]) * ($testy-$$verty[$i]) / ($$verty[$j]-$$verty[$i]) + $$vertx[$i]) ) {
                $c = !$c;
	    }
	}
	return $c;
    }
    
    sub findLocation {
        my ($self,$node) = @_;
        my $locstr = "";
	my $lat = $$nodes{$node}->{lat};
	my $lon = $$nodes{$node}->{lon};
        
        for (my $a=0;$a<=$#admin;$a++) {
            next unless defined($admin[$a]);
            my $r = $admin[$a];
            foreach my $l (keys %{$admin[$a]}) {
    	        next unless ($lat >= $$r{$l}->{minlat} and $lat <= $$r{$l}->{maxlat}) and ($lon >= $$r{$l}->{minlon} and $lon <= $$r{$l}->{maxlon});
                my $nvert = $#{$$r{$l}->{lat}};
                my $c = $self->pnpoly($nvert,$$r{$l}->{lon},$$r{$l}->{lat},$lon,$lat);
                $locstr .= " $l($a)" if $c;
            }
        }
        return $locstr;
    }
    
    sub saveOSMdata {
        my $self = shift;
        my $dbfile = shift;

        return unless $self->{changed};
        $self->removetempways();
        $self->removetempnodes();
        $self->{changed} = 0;
        $self->{nodes} = $nodes;
        $self->{ways} = $ways;
        $self->{dist} = $dist;
        $self->{way} = $way;
        $self->{bounds} = \@bounds;
        $self->{admin} = \@admin;
        $self->nstore($dbfile);
    }
    
    sub fetchUrl {
        my ($self,$url) = @_;
        my $retry = 0;
	my $result;

        print "url = $url\n";
	do {
	    print "retry $retry\n" if $retry > 0;
	    my $req = HTTP::Request->new(GET =>$url);
	    $result = $ua->request($req);
	    if ($result->code == 200) {
		return $result;
	    }
	    $retry++;
	}
	while ($retry < 5 && $result->code == 500);
	return -1;
    }
    
    sub loadway {
        my ($self,$w) = @_;

        my $file = "map_way_$w.osm";
        my $url = sprintf($getwaycmd,$w);
        return $self->loadOSMdata($file,$url);
    }
    
    sub loadrelation {
        my ($self,$w) = @_;

        my $file = "map_rel_$w.osm";
        my $url = sprintf($getrelcmd,$w);
        return $self->loadOSMdata($file,$url);
    }
    
    sub loadOSMdata {
        my ($self,$file,$url) = @_;
        
        my $content;
        if (open OLD, "<maps/$file") {
            close OLD;
            $content = "maps/$file";
        } else {
            my $data = $self->fetchUrl($url);
            $content = $data-> content;
	    if (open NEW,">maps/$file") {
	        print NEW $content;
	        close NEW;
	    }
        }
        return XMLin($content, ForceArray=>['tag','nd','member','way','node','relation'],KeyAttr=>{tag => 'k', way=>'id','node'=>'id',relation=>'id'},ContentKey => "-v");
    }
    
    sub useNetdata {
        my $self =  shift;
	my @bbox = @_;
        return -1  if $#bbox != 3 ;
	
        my $file = "map_bbox_".join("_",@bbox).".osm";
        my $url = $getmapcmd.join(",",@bbox);
        my $doc = $self->loadOSMdata($file,$url);
        $self->procesdata($doc);
    }
    
    sub useLocaldata {
        my $self =  shift;
	my $filename = shift;
	$filename = "map.osm" unless defined $filename;
	my $doc = $self->loadOSMdata($filename,undef);
        $self->procesdata($doc);
    }
    
    sub fetchCoor {
	my ($self,$lat,$lon,$small) = @_;
	my $delta = 0.025;
	$delta = 0.01 if defined($small);
	my $minlon=$lon-$delta;
	my $maxlon=$lon+$delta;
	my $minlat=$lat-$delta;
	my $maxlat=$lat+$delta;
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
    
    sub removetempnodes {
	my $self = shift;
	
	for (my $i=0;$i<$self->{tempnodes};$i++) {
        my $nodeid="TN$i";
	    my $w=$way->{$nodeid};
	    foreach my $n ($self->neighbours($nodeid)) {
	        delete($$way{$n}->{$nodeid}) if exists($$way{$n}->{$nodeid});
	        delete($$dist{$n}->{$nodeid}) if exists($$dist{$n}->{$nodeid});
	    }
	    delete($$way{$nodeid});
	    delete($$dist{$nodeid}) if exists($$dist{$nodeid});
	    delete($$nodes{$nodeid});
	}
	$self->{tempnodes} = 0;
    }
    
    sub removetempways {
	my $self = shift;
	
	for (my $i=0;$i<$self->{tempways};$i++) {
	    my $wayid="WN$i";
	    delete($$ways{$wayid});
	}
	$self->{tempways} = 0;
    }
    
    sub tempnode {
        my ($self,$lat,$lon) = @_;
        my $nr = $self->{tempnodes}++;
        my $nodeid="TN$nr";
        $$nodes{$nodeid}->{lat} = $lat;
        $$nodes{$nodeid}->{lon} = $lon;
	print "Created node $nodeid, $lat,$lon\n";
        return $nodeid;
    }
    
    sub tempway {
        my $self = shift;
        my @nds = @_;
        my $nr = $self->{tempways}++;
        my $wayid="WN$nr";
        $$ways{$wayid}->{nd}=();
        for (my $i=0;$i<=$#nds;$i++){
             $$ways{$wayid}->{nd}->[$i]->{ref}=$nds[$i];
             $way->{$nds[$i-1]}->{$nds[$i]}=$wayid if $i>0;
             $way->{$nds[$i]}->{$nds[$i-1]}=$wayid if $i>0;
        }
        $$ways{$wayid}->{tag}->{highway}="service";
	print "Created way $wayid: nodes: ",join(", ",@nds),"\n";
        return $wayid;
    }
    
    sub findNode {
        my ($self,$lat,$lon,$maxdist) = @_;
	my $node = undef;
	my $distance=$infinity;

	for my $n (keys %$nodes) {
	    my $d = $self->distanceCoor($lat,$lon,$$nodes{$n}->{lat},$$nodes{$n}->{lon},$n);
	    if ($d < $distance) {
		$distance=$d;
		$node = $n;
	    }
	}
        
	return $node if (defined($maxdist) && ($distance <= $maxdist));
	
        my @nb = $self->neighbours($node);
        my $nn = $self->tempnode($lat,$lon);
        my $madeway = 0;
        my $x1=$lon;
        my $y1=$lat;
        my $x2=$$nodes{$node}->{lon};
        my $y2=$$nodes{$node}->{lat};
        for my $n (@nb) {
            my $x3=$$nodes{$n}->{lon};
            my $y3=$$nodes{$n}->{lat};
            my $dx = ($x2-$x3 );
            my $dy = ($y2-$y3);
            my ($a,$b,$lat1,$x4,$y4,$nt);
            if (abs($dy) > abs($dx) || $dx == 0) {
                $a = $dx/$dy;
                $b = $x2 - $a*$y2;
                $y4 = ($y1+$a*($x1-$b))/(1+$a**2);
                if (($y4-$y2)/$dy < 0) {
                    $x4 = $y4*$a+$b;
                    $nt = $self->tempnode($y4,$x4);
                    $self->tempway($nn,$nt,$node);
                    $self->tempway($nn,$nt,$n);
                    $madeway = 1;
                }
            } else {
                $a = $dy/$dx;
                $b = $y2 - $a*$x2;
                $x4 = ($x1+$a*($y1-$b))/(1+$a**2);
                if (($x4-$x2)/$dx < 0) {
                    $y4 = $x4*$a+$b;
                    $nt = $self->tempnode($y4,$x4);
                    $self->tempway($nn,$nt,$node);
                    $self->tempway($nn,$nt,$n);
                    $madeway = 1;
                }
            }
        } 
        $self->tempway($node,$nn) if $madeway == 0;
        return $nn;
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
        return defined($vehicle) ? $d *3.6/$profiles{$vehicle}->{avgspeed} : $d;
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
    
    sub curvecost {
        my ($self,$x,$y,$p) = @_;
        return 0 unless defined($p);
        
        my $dx1 = $$nodes{$y}->{lon}-$$nodes{$x}->{lon};
        my $dy1 = $$nodes{$y}->{lat}-$$nodes{$x}->{lat};
        my $dx2 = $$nodes{$x}->{lon}-$$nodes{$p}->{lon};
        my $dy2 = $$nodes{$x}->{lat}-$$nodes{$p}->{lat};
        my $h1 = 180 * atan2($dy1,$dx1) / $PI;
        my $h2 = 180 * atan2($dy2,$dx2) / $PI;
        my $dh = abs($h1-$h2);
		$dh = 360-$dh if $dh > 180;
        return 0 if $dh < 45;
        return 5 if $dh < 60;
        return 10 if $dh < 90;
        return 50 if $dh <120;
        return 100 if $dh <150;
#		print "curve $p $x $y $dx1 $dy1 $dx2 $dy2 $h1 $h2 $dh\n";
        return 2000;
    }
    
    sub direction {
        my ($self,$n1,$n2) = @_;
        my $dx1 = $$nodes{$n2}->{lon}-$$nodes{$n1}->{lon};
        my $dy1 = $$nodes{$n2}->{lat}-$$nodes{$n1}->{lat};
        return 180 * atan2($dx1,$dy1) / $PI;
    }
    
    sub cost {
        my ($self,$x,$y,$prevnode) = @_;
	
	my $d = $dist->{$x}->{$y};
        unless (defined($d)) {
	    $d = $dist->{$x}->{$y} = $dist->{$y}->{$x} = $self->distance($x,$y);
            unless ((substr($x,0,2) eq 'TN') or (substr($y,0,2) eq 'TN')) {
                $self->{changed} = 1 
            }
        }
	return $d unless defined($vehicle);
	
	my $speed = $profiles{$vehicle}->{maxspeed};
	
	my $w = $way->{$x}->{$y};
	my $ww=$$ways{$w};
	my $wwtag = $$ww{tag};
	
	my ($hw,$access,$cw,$fa,$ca,$onew,$ma);
	if (defined($wwtag)) {
	    $hw = $$wwtag{highway} if exists($$wwtag{highway});
	    $hw = "unclassified" if !defined($hw) && exists($$wwtag{route}) && $$wwtag{route} eq "ferry";
	    $access = $$wwtag{access};
	    $cw = $$wwtag{cycleway};
	    $fa = $$wwtag{foot};
	    $ca = $$wwtag{bicycle};
	    $onew = $$wwtag{oneway};
	    $ma = $$wwtag{motorcar};
	} else {
	    return $infinity;
	}
	return $infinity unless defined($hw);
	$access = "yes" unless defined($access);
	$fa="" unless defined($fa);
	$ca="" unless defined($ca);
	$ma="" unless defined($ma);
	
	if (defined($$wwtag{maxspeed})) {
	    $speed = $$wwtag{maxspeed} if $$wwtag{maxspeed} < $speed;
	} elsif (exists($highways{$hw}->{speed})) {
	    my $defspeed = $highways{$hw}->{speed};
	    $speed = $defspeed if $defspeed < $speed;
	} else {
	    print "Geen snelheid voor $hw op weg $w\n";
	    return $infinity;
	}
	
	return $infinity if !defined($speed) or $speed == 0;
	my $cost = $d * 3.6 / $speed;
	my $extracost = 0;
	$extracost = $profiles{$vehicle}->{allowed}->{$hw}->{extracost} if exists($profiles{$vehicle}->{allowed}->{$hw}) and exists($profiles{$vehicle}->{allowed}->{$hw}->{extracost});
	
	my $nodey = $$nodes{$y};
	
	if ($vehicle eq "foot") {
	    return $infinity if $fa eq "no";
	    return $infinity if $access eq "no" and $fa ne "yes";
	    return $infinity if (!exists($profiles{$vehicle}->{allowed}->{$hw}));
	} elsif ($vehicle eq "bicycle") {
	    return $infinity if $ca eq "no";
	    return $infinity if $access eq "no" and $ca ne "yes";
	    return $infinity if (!exists($profiles{$vehicle}->{allowed}->{$hw})) && !defined($cw);
	    $extracost = 0 if defined($cw);
	    if (defined($onew) and (!defined($cw) or $cw ne "opposite")) {
		return $infinity if $self->wrong_direction($x,$y,$w,$onew);
	    }
	    if (exists($$nodey{highway}) and exists($highways{$$nodey{highway}}->{extracost})) {
		$extracost += $highways{$$nodey{highway}}->{extracost};
	    }
	    $extracost += 5 if exists($$nodey{traffic_calming});
	} elsif ($vehicle eq "car") {
	    return $infinity if $ma eq "no";
	    return $infinity if $access eq "no" and $ma ne "yes";
	    return $infinity if (!exists($profiles{$vehicle}->{allowed}->{$hw}));
#	    return $infinity if defined($onew) and $self->wrong_direction($x,$y,$w,$onew);
	    if (defined($onew)) {
              if ($self->wrong_direction($x,$y,$w,$onew)) {
                return $infinity ;
              }
            }
	    if (exists($$nodey{highway}) and exists($highways{$$nodey{highway}}->{extracost})) {
		$extracost += $highways{$$nodes{$y}->{highway}}->{extracost};
	    }
	    $extracost += 10 if defined($$nodey{traffic_calming});
            $extracost += $self->curvecost($x,$y,$prevnode);
	} else {
	    die "Onbekend voertuig\n";
	}
	return $infinity if $access eq "no";
	my $name=$$wwtag{name};
	my $ref=$$wwtag{ref};
	$ref="" unless defined($ref);
	$name="" unless defined($name);
	
#	print "cost $x $y $d $speed $hw $cost $extracost $ma $access $name $ref\n";
#	print "cost $x $y $d $speed $cost $extracost\n";
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
    
    sub dist { 
	my $self = shift;
	my $n1=shift;
	my $n2=shift;
	return undef unless defined($n1) && defined($n2);
	return $dist->{$n1}->{$n2};
    }
}
1;
