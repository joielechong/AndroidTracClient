{
    package OSM::Map;
    
    use strict;
    use vars qw(@ISA $VERSION);
    
    use LWP::UserAgent;
    use Data::Dumper;
    use XML::LibXML::Reader;
    use XML::Simple;
    use Geo::Distance;
    
    require Exporter;
    @ISA = qw(Exporter);
    require OSM::Map::Db;
    
    my $getmapcmd;
    my $getwaycmd;
    my $getnodecmd;
    my $getrelcmd;
    my $infinity;
    my $geo;
    my $ua;
    my $PI;
    my $dbh;
    
    my $vehicle;
    my %highways;
    my %profiles;
    
    sub initRoute {
        my $self = shift;
	$vehicle = shift;
        $self->removetempways();
        $self->removetempnodes();
	if (defined($vehicle)) {
	    die "Geen profile voor $vehicle\n" unless defined($profiles{$vehicle});
	}
    }
    
    sub XXXnode {
        my $self = shift;
	my $n = shift;
	return $self->{nodes}->{$n};
    }
    
    sub XXXway {
	my $self = shift;
	my $n = shift;
	return $self->{ways}->{$n};
    }
    
    sub XXXgetnodes {
        my ($self,$w) = @_;
        
        return @{$self->{ways}->{$w}->{nd}};
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
       
        $dbh = OSM::Map::Db->new("osm.sqlite");
	
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
    }
    
    sub distanceCoor {
        my ($self,$lat1,$lon1,$lat2,$lon2) = @_;
        return $geo->distance('meter',$lon1,$lat1,$lon2,$lat2);
    }
    
    sub distance {
        my ($self,$n1,$n2) = @_; 
	my ($lat1,$lon1) = $dbh->getCoor($n1);
	my ($lat2,$lon2) = $dbh->getCoor($n2);
        return $geo->distance('meter',$lon1,$lat1=>$lon2,$lat2);
    }
    
    sub inboundCoor {
        my $self = shift;
        return $dbh->inboundCoor(@_);
    }
    
    sub inboundNode {
        my ($self,$node) = @_;
        return $dbh->inboundNode($node);
    }
    
    sub processElem {
        my $self = shift;
        my $elem = shift;
        my $xmlname = $elem->{xmlname};
        my $id = $elem->{id};
#       print Dumper $elem;
        if ($xmlname eq 'node') {
            my $version = -1;
            if (my @row = $dbh->checkNode($id)) {
                $version = $row[0];
                $dbh->delNode($id) if ($elem->{version} > $version);
                print "Nieuwe versie voor node $id, versie = $version\n" if $version > -1 and $elem->{version} > $version;
            }
            if ($elem->{version} > $version) {
                unless ($dbh->insertNode($id,$elem->{lat},$elem->{lon},$elem->{version}) ) {
                    printf STDERR "probleem insertNode id=%d versions %d %d: %s\n",$id,$version,$elem->{version},$dbh->errstr ;
                    die;
                }
            }
        } elsif ($xmlname eq 'way') {
            if (exists($elem->{tag}->{highway}) || (exists($elem->{tag}->{route}) and $elem->{tag}->{route} eq "ferry")) {
                my $version = -1;
                if (my @row = $dbh->checkWay($id)) {
                    $version = $row[0];
                    $dbh->delWay($id) if ($elem->{version} > $version);
                }
                if ($elem->{version} > $version) {
                    print "Nieuwe versie voor weg $id, versie = $version\n" if $version > -1;
                    $dbh->insertWay($id,$elem->{version});
                    my $nds = $#{$elem->{nd}};
                    for(my $i=0;$i<=$nds;$i++) {
                        printf "probleem insertnd id=%d seq=%d ref=%d: %s\n",$id,$i,$elem->{nd}->[$i]->{ref},$dbh->errstr unless $dbh->insertNd($id,$i,$elem->{nd}->[$i]->{ref});
                    }
                }
            } else {
                delete $elem->{tag};
            }
        } elsif ($xmlname eq 'relation') {
            my $version = -1;
            if (my @row = $dbh->checkRelation($id)) {
                $version = $row[0];
                $dbh->delRelation($id) if ($elem->{version} > $version);
            }
            if ($elem->{version} > $version) {
                print "Nieuwe versie voor relatie $id, versie = $version\n" if $version > -1;
                $dbh->insertRelation($id,$elem->{version});
                my $membs = $#{$elem->{member}};
                for(my $i=0;$i<=$membs;$i++) {
                    my $memb = $elem->{member}->[$i];
                    printf "probleem insertmemb id=%d seq=%d ref=%d: %s\n",$id,$i,$memb->{ref},$self->{dbh}->errstr unless $dbh->insertMemb($id,$i,$memb->{type},$memb->{ref},$memb->{role});
                }
            }
        } elsif ($xmlname eq 'bounds') {
            $dbh->insertBound($elem->{minlat},$elem->{maxlat},$elem->{minlon},$elem->{maxlon});
        }
        foreach my $k (keys %{$elem->{tag}}) {
            $dbh->insertTag($id,$k,$elem->{tag}->{$k});
        }
    }
    
    sub processXMLNode {
        my $self = shift;
        my $elem;
        my $reader = shift;
        $elem->{depth} = $reader->depth;
        $elem->{xmlname} = $reader->name;
        $elem->{nodeType} = $reader->nodeType;
        if ($reader->hasAttributes()) {
            $reader->moveToFirstAttribute();
            do {
#               printf "%d %d %s %d %s\n",$reader->depth,$reader->nodeType,$reader->name,$reader->isEmptyElement,$reader->value;
                $elem->{$reader->name} = $reader->value;
            } while ($reader->moveToNextAttribute());
        }
        return $elem;
    }
    
    sub importRelation {
        my ($self,$relation,$notlocal) = @_;
        
        my $file="map_rel_$relation.osm";
        my $url =sprintf("$getrelcmd",$relation);
        $self->loadFile($file,$url,$notlocal);
    }
    
    sub importBbox {
        my ($self,$bbox) = @_;
        
        my $file="map_bbox_$bbox.osm";
        my $url = $getmapcmd.$bbox;
        $self->loadFile($file,$url);
    }
    
    sub loadFile {
        my ($self,$file,$url,$notlocal) = @_;
        
        if ((!(defined($notlocal) || $notlocal)) && defined($file) && open OLD, "<maps/$file") {
            close OLD;
        } else {
            my $data = $self->fetchUrl($url);
            if (ref($data) ne "HTTP::Response") {
                printf STDERR "URL %s is niet geldig\n",$url;
                return;
            }
            my $content = $data-> content;
	    if (open NEW,">maps/$file") {
	        print NEW $content;
	        close NEW;
	    }
        }
   	$self->importOSMfile("maps/$file");
    }    
   
    sub importOSMfile {
        my $self = shift;
        my $f = shift;
        
        my $fd;
        if ($f=~m/\.gz$/) {
            my $cmd = "zcat $f";
            print $cmd,"\n";
            open $fd,"$cmd |" or die "Kan $cmd niet uitvoeren\n";
	} else { 
	    print "$f\n";
	    open $fd,"<$f" or die "Kan file $f niet lezen\n";
	}
	binmode $fd;
	my $doc = new XML::LibXML::Reader(FD =>$fd);
	my $currelem = undef;
	my $i = 0;
	
        $dbh->begin_work;
	while ($doc->read()) {
	    my $elem = $self->processXMLNode($doc);
	    next if $elem->{nodeType} == 14 or $elem->{nodeType} == 15;
	    if ($elem->{depth} == 1) {
		$self->processElem($currelem) if defined $currelem;
		$currelem = $elem;
		$currelem->{nd} = [] if $currelem->{xmlname} eq "way";
		$currelem->{member} = [] if $currelem->{xmlname} eq "relation";
	    } elsif (defined($currelem) && $elem->{depth} == 2) {
		if ($elem->{xmlname} eq "tag") {
		    $currelem->{tag}->{$elem->{k}} = $elem->{v};
		} elsif ($elem->{xmlname} eq "nd") {
		    push @{$currelem->{nd}}, {'ref'=>$elem->{ref}};
		} elsif ($elem->{xmlname} eq "member") {
		    push @{$currelem->{member}}, {type => $elem->{type},ref=>$elem->{ref},role=>$elem->{role}};
		}
	    }
#          print Dumper $elem unless $elem->{nodeType} == 14 or $elem->{nodeType} == 15 or $elem->{depth} == 0;
	    $i++;
	    if (($i%5000) == 0) {
		printf "%d nodes, %d ways %d relations %d bounds\n",$dbh->getCounts();
	    }
	    
	}
	$self->processElem($currelem) if defined $currelem;
	$doc->finish;
	close $fd;
	$dbh->commit;
	printf "%d nodes, %d ways %d relations %d bounds\n",$dbh->getCounts();
    }
    
    sub postprocess {
        my $self = shift;
        print "Postprocessing\n";
        my $rrr = $dbh->imcompleteRelations();
        foreach my $r (@$rrr) {
            $self->importRelation($r);
        }
        $dbh->do("DELETE FROM tag WHERE k IN ('created_by','source') or k like 'AND%' or k like '3dshapes%'");
        $dbh->do("DELETE FROM tag WHERE v IN ('0','no','NO','false','FALSE') AND k IN ('bridge','tunnel','oneway')");
        $dbh->do("UPDATE tag set v='yes' WHERE v in ('1','true','TRUE') AND k IN ('bridge','tunnel','oneway')");
        $dbh->do("UPDATE tag set v='rev' WHERE v = '-1' and k='oneway'");
	$dbh->do("INSERT INTO neighbor (way,id1,id2) SELECT DISTINCT way,id1,id2 FROM nb");
        $dbh->do("INSERT INTO admin (id,name,level,minlat,maxlat,minlon,maxlon) SELECT id,name,level,minlat,maxlat,minlon,maxlon FROM admintmp WHERE NOT id in (SELECT id from admin)");
        $dbh->do("DELETE FROM bucket WHERE NOT node in (SELECT ref FROM nd)");
	$dbh->do("UPDATE node set processed=1 WHERE NOT processed");
	$dbh->do("UPDATE way set processed=1 WHERE NOT processed");
	$dbh->do("UPDATE relation set processed=1 WHERE NOT processed");
	$dbh->do("UPDATE bound set processed=1 WHERE NOT processed");
    }
        
    sub pnpoly {
        my $self = shift;
	my $nvert = shift;
	my $vertxy = shift;
	my $testx = shift;
	my $testy = shift;
	
	my ($i,$j,$c);
	$c=0;
	$j=$nvert-1;
	for ($i=0;$i<$nvert;$j=$i++) {
            if ( (($$vertxy[$i]->[0]>$testy) != ($$vertxy[$j]->[0]>$testy)) &&
		 ($testx < ($$vertxy[$j]->[1]-$$vertxy[$i]->[1]) * ($testy-$$vertxy[$i]->[0]) / ($$vertxy[$j]->[0]-$$vertxy[$i]->[0]) + $$vertxy[$i]->[1]) ) {
                $c = !$c;
	    }
	}
	return $c;
    }

    sub findLocation {
        my ($self,$node) = @_;
        my $locstr = "";
	my ($lat,$lon) = $dbh->getCoor($node);
	
        my $admins = $dbh->adminNode($node);
        foreach my $row (@$admins) {
            my @row=@$row;
            my $id=$row[0];
#	    my $latar = $dbh->getLatarr($id);
#	    my $lonar = $dbh->getLonarr($id);
            my $latlonar = $dbh->getLatLonarr($id);
	    my $c=$self->pnpoly(1+$#{$latlonar},$latlonar,$lon,$lat);
	    $locstr .= sprintf(" %s(%d)",$row[1],$row[2]) if $c;
	}
        return $locstr;
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
            sleep 5*$retry;
	}
	while ($retry < 5 && $result->code == 500);
	return -1;
    }
    
    sub fetchCoor {
	my ($self,$lat,$lon,$small) = @_;
	my $delta = 0.025;
	$delta = 0.01 if defined($small);
	my $minlon=$lon-$delta;
	my $maxlon=$lon+$delta;
	my $minlat=$lat-$delta;
	my $maxlat=$lat+$delta;
	for my $b (@{$self->{bounds}}) {
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
	$self->importBbox(join(",",@bbox));
	$self->postprocess();
    }
    
    sub removetempnodes {
        $dbh->do("DELETE FROM node WHERE id<0");
    }
    
    sub removetempways {
        $dbh->do("DELETE FROM way WHERE id<0");
    }
    
    sub tempnode {
        my ($self,$lat,$lon) = @_;
        
        my $nodeid = $dbh->minID();
        $nodeid = 0 if $nodeid > 0;
        
        my $elem;
        $nodeid--;
        $elem->{xmlname} = 'node';
        $elem->{id} = $nodeid;
        $elem->{lat} = $lat;
        $elem->{lon} = $lon;
        $elem->{version} = 1;
        $self->processElem($elem);
        
	print "Created node $nodeid, $lat,$lon\n";
        return $nodeid;
    }
    
    sub tempway {
        my $self = shift;
        my @nds = @_;
        
        my $wayid = $dbh->minID();
        $wayid = 0 if $wayid > 0;
        
        my $elem;
        $wayid--;
        $elem->{xmlname} = 'way';
        $elem->{id} = $wayid;
        $elem->{version} = 1;
        $elem->{nd} = [];
        foreach my $n (@nds) {
            push @{$elem->{nd}},{'ref'=>$n};
        }
        $elem->{tag}->{highway} = "service";
        $self->processElem($elem);        
	print "Created way $wayid: nodes: ",join(", ",@nds),"\n";
        $dbh->insertNb($nds[0],$nds[1],$wayid);
        $dbh->insertNb($nds[1],$nds[2],$wayid) if defined($nds[2]);
        return $wayid;
    }
    
    sub findNode {
        my ($self,$lat,$lon,$maxdist) = @_;
	my $node = undef;
	my $distance=$infinity;
        my ($x2,$y2);
        
        my $nn = $self->tempnode($lat,$lon);
        my $nds = $dbh->loadBucket($nn);
	
	for my $n (@$nds) {
	    my $d = $self->distanceCoor($lat,$lon,$$n[1],$$n[2]);
	    if ($d < $distance) {
		$distance=$d;
		$node = $$n[0];
                $x2 = $$n[2];
                $y2 = $$n[1];
	    }
	}
        
	return $node if (defined($maxdist) && ($distance <= $maxdist));
	
        my $nb = $self->neighbours($node);
        my $madeway = 0;
        my $x1=$lon;
        my $y1=$lat;
        for my $n (@$nb) {
            my ($y3,$x3) = $dbh->getCoor($n);
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
                    $self->tempway($nn,$nt);
                    $self->tempway($nt,$node);
                    $self->tempway($nt,$n);
                    $madeway = 1;
                }
            }
        } 
        $self->tempway($node,$nn) if $madeway == 0;
        return $nn;
    }
    
    sub fetchNode {
	my ($self,$node) = @_;
	$self->fetchCoor($dbh->getCoor($node));
    }
    
    sub calc_h_score {
        my ($self,$x,$y) = @_;
	
        my $d=$self->distance($x,$y);
        return defined($vehicle) ? $d *3.6/$profiles{$vehicle}->{avgspeed} : $d;
    }
    
    sub wrong_direction {
	my ($self,$nodex,$nodey,$ww,$onew) = @_;
	my @nd = @{$ww->{nd}};
	
	foreach my $n (@nd) {
	    if ($n->{ref} == $nodey->{id}) {
		return ($onew ne "rev");
	    }
	    if ($n->{ref} == $nodex->{id}) {
		return ($onew eq "rev");
	    }
	}
	die "nodes not found in wrong direction ".$nodex->{id}." ".$nodey->{id}." ".$ww->{id}."\n";
    }
    
    sub curvecost {
        my ($self,$ndx,$ndy,$ndp) = @_;
        return 0 unless defined($ndp);
 	
        return 0 unless (defined($$ndx{lon}) and defined($$ndx{lat}));
        return 0 unless (defined($$ndy{lon}) and defined($$ndy{lat}));
        return 0 unless (defined($$ndp{lon}) and defined($$ndp{lat}));
        
        my $dx1 = $$ndy{lon}-$$ndx{lon};
        my $dy1 = $$ndy{lat}-$$ndx{lat};
        my $dx2 = $$ndx{lon}-$$ndp{lon};
        my $dy2 = $$ndx{lat}-$$ndp{lat};
        my $h1 = 180 * atan2($dy1,$dx1) / $PI;
        my $h2 = 180 * atan2($dy2,$dx2) / $PI;
        my $dh = abs($h1-$h2);
	$dh = 360-$dh if $dh > 180;
        return 0 if $dh < 45;
        return 5 if $dh < 60;
        return 10 if $dh < 90;
        return 50 if $dh <120;
        return 100 if $dh <150;
#       print "curve $p $x $y $dx1 $dy1 $dx2 $dy2 $h1 $h2 $dh\n";
        return 2000;
    }
    
    sub direction {
        my ($self,$n1,$n2) = @_;
        my ($lat1,$lon1) = $dbh->getCoor($n1);
        my ($lat2,$lon2) = $dbh->getCoor($n2);
        
        my $dx1 = $lon2-$lon1;
        my $dy1 = $lat2-$lat1;
        return 180 * atan2($dx1,$dy1) / $PI;
    }
    
    sub cost {
        my ($self,$x,$y,$prevnode) = @_;
	
	my $d = $dbh->getDistance($x,$y);
	return $d unless defined($vehicle);
	
	my $speed = $profiles{$vehicle}->{maxspeed};
        my $w=$dbh->getWay($x,$y);
        my $ww = $dbh->loadWay($w);
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
	
        my $nodey = $dbh->loadNode($y);
	
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
               my $nodex = $dbh->loadNode($x);
		return $infinity if $self->wrong_direction($nodex,$nodey,$ww,$onew);
	    }
	    if (exists($$nodey{highway}) and exists($highways{$$nodey{highway}}->{extracost})) {
		$extracost += $highways{$$nodey{highway}}->{extracost};
	    }
	    $extracost += 5 if exists($$nodey{traffic_calming});
	} elsif ($vehicle eq "car") {
	    return $infinity if $ma eq "no";
	    return $infinity if $access eq "no" and $ma ne "yes";
	    return $infinity if (!exists($profiles{$vehicle}->{allowed}->{$hw}));
#	    return $infinity if defined($onew) and $self->wrong_direction($nodex,$nodey,$ww,$onew);
            my $nodex = $dbh->loadNode($x);
            my $nodep = $dbh->loadNode($prevnode);
	    if (defined($onew)) {
		if ($self->wrong_direction($nodex,$nodey,$ww,$onew)) {
		    return $infinity ;
		}
            }
	    if (exists($$nodey{highway}) and exists($highways{$$nodey{highway}}->{extracost})) {
		$extracost += $highways{$$nodey{highway}}->{extracost};
	    }
	    $extracost += 10 if defined($$nodey{traffic_calming});
            $extracost += $self->curvecost($nodex,$nodey,$nodep);
	} else {
	    die "Onbekend voertuig\n";
	}
	return $infinity if $access eq "no";
	
	$extracost += $profiles{$vehicle}->{barrier}->{type}->{$$nodey{barrier}} if exists($$nodey{barrier}) and exists($profiles{$vehicle}->{barrier}->{type}->{$$nodey{barrier}});
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
	my $node = shift;
	
        return $dbh->getNb($node);
    }
    
    sub loadway {
        my $self = shift;
	my $p1 = shift;
	my $p2 = shift;
	
	return $dbh->loadWay($p1,$p2);
    }
    
    sub getway {
        my $self = shift;
	my $p1 = shift;
	my $p2 = shift;
	
	return $dbh->getWay($p1,$p2);
    }
    
    sub getways {
        my $self = shift;
	my $n = shift;
	return @{$dbh->getWays($n)};
    }
    
    sub dist { 
	my $self = shift;
        return $dbh->getDistance(@_);
    }
}


1;
