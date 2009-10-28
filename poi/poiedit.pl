#! /usr/bin/perl -w

use lib "/home/mfvl/lib/perl/";
use strict;
use ProxyList;
use XML::Simple;
use LWP::UserAgent;
use LWP::ConnCache;
use HTTP::Cookies;
use Data::Dumper;

my @xmlsrc=( 
    "http://www.flitspaal.nl/poi_flitspalen.xml",
    "http://www.bruxelles5.info/POI/poi_bruxelles5.xml",
    "http://www.goedkooptanken.nu/tomtom/pois.xml",
#    "http://flitsservice.com/poi_edit_fs.xml"
    );

my @groups=(
    ['standaard_installatie1'],
    ["Radars Belges", 'Radars Francais'],
    ['Nederland'],
#    ['Flitsservice.nl;Actuele mobiele controles']
    );

my @usernames=('michiel@van-loon.xs4all.nl',
	       "",
	       "",
#	       ""
				);

my @passwords=('mikel02',
	       "",
	       "",
#	       ""
				 );

my $cache = LWP::ConnCache->new;

sub set_proxy {
    my $ua = shift;
    
    my $proxy=ProxyList::get_proxy();
#    print STDERR "Proxy used = $proxy\n";
    $ua->proxy(['http', 'ftp'], "http://".$proxy."/");
    $ua->no_proxy('flitspaal.nl','bruxelles5.info','goedkooptanken.nu','bnet.be','navifriends.de','navifriends.com','flitsservice.nl','alertegps.fr','poi66.com','alertegps.com');
}

sub _ingroup {
    my $group = shift;
    my $index = shift;
    return grep(/^$group$/,@{$groups[$index]});
}

sub ingroup {
    my $groups = shift;
    my $index = shift;
    
    if (ref($groups) eq "") {
	return _ingroup($groups,$index);
    } else {
	for (my $i=0;$i<=$#$groups;$i++) {
	    if (_ingroup($groups->[$i],$index))
	    {
		return 1;
	    }
	}
	return 0;
    }
}

sub get_url {
    my $ua = shift;
    my $url = shift;

#    print STDERR "get_url: $url\n";
# Create a request
    my $req = HTTP::Request->new(GET => $url);
# Pass request to the user agent and get a response back
    
    while ($#_ >= 0) {
        my $key = shift;
        my $value = shift;
        $req->header($key=>$value);
    }
    
    my $result = $ua->request($req);
#    print STDERR $result->status_line,"\n";
    return $result;
}

sub process_poi {
    my $ua = shift;
    my $poi = shift;
    my $index = shift;
#    print Dumper($poi);
    
    if (ingroup($poi->{group},$index)) {
        my $format ="ov2";
        $format = $poi->{format} if defined($poi->{format});
        my $description = $poi->{description};
	my $url = $poi->{url};
	if (defined($poi->{authorization})) {
	    my $username=$usernames[$index];
	    my $password=$passwords[$index];
	    $url =~s/%Username%/$username/;	
	    $url =~s/%Password%/$password/;    
	}
	
	my $filename = "$description.$format";
	my @stat = stat($filename);
        my $lmstring = "20050101 000000";
        my $gmtijd = "Sat Jan 01 00:00:00 2005";
        if ($#stat >= 0) {
            my $mtime=$stat[9];
            my @tijd = gmtime($mtime);
            $gmtijd = gmtime($mtime);
            $lmstring = sprintf("%4.4d%2.2d%2.2d %2.2d%2.2d%2.2d",$tijd[5]+1900,$tijd[4]+1,$tijd[3],$tijd[2],$tijd[1],$tijd[0]);
        }
        my ($dag_local, $tijd_local)=split(" ",$lmstring);
	
	my $res;
	if (defined($poi->{lastmodified})) {
	    my $lastmodified = $poi->{lastmodified};
            $res = get_url($ua,$lastmodified);
	    my $rmstring = $res->content;
	    chomp($rmstring);
	    print "$description $lmstring $rmstring\n";
	    my ($dag_remote, $tijd_remote)=split(" ",$rmstring);
	    
	    return if ($dag_remote < $dag_local);
	    return if ($dag_remote == $dag_local) && ($tijd_remote <= $tijd_local);
	    $res = get_url($ua,$url);
	} else {
	    $res = get_url($ua,$url,'if_modified_since'=>$gmtijd);
	    print "$description $lmstring ".$res->{_rc}."\n";
	}	    
# Check the outcome of the response
	return unless ($res->is_success);
	my $newdata = $res->content;
	my $oldfile = "$filename.bak";
	
	unlink($oldfile);
	rename($filename,$oldfile);
	open NIEUW,">$filename" or die "Kan $filename niet aanmaken\n";
	print NIEUW $newdata;
	close NIEUW;	
    }
    
# Image file ook ophalen dan lijken we echter
    
    my $url = $poi->{image};
    if (defined($url) && $url =~ /http:/) {
	if (defined($poi->{authorization})) {
	    my $username=$usernames[$index];
	    my $password=$passwords[$index];
	    $url =~s/%Username%/$username/;	
	    $url =~s/%Password%/$password/;    
	}
	get_url($ua,$url);
    }	
    
    
}

sub is_integer      { $_[0] =~ /^[+-]?\d+$/       }

my $ua = LWP::UserAgent->new;
$ua->agent("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7");
$ua->conn_cache($cache);
set_proxy($ua);

my $start=0;
my $eind=$#xmlsrc;

my $arg = shift;
if (defined($arg) && is_integer($arg)) {
    $start=$arg;
    $eind=$arg;
}

my $retry=0;

for (my $i=$start;$i<=$eind;$i++) {
    print $xmlsrc[$i],"\n";
    my $req = HTTP::Request->new(GET => $xmlsrc[$i]);
    $req->header(UA_CPU => 'x86');
    #print Dumper($req);
    my $res = $ua->request($req);
#    print STDERR "Request geeft ".$res->status_line."\n";
#    print STDERR $res->as_string;
    unless ($res->is_success) {
	if ($i == 3 && $retry <5) {
	    set_proxy($ua);
	    $i--;
	    $retry++;
	}
	next;
    }
#    print "**".$res->content."\n";
    unless ( $res->content =~ /<?xml/) {
	if ($i == 3 && $retry <5) {
	    set_proxy($ua);
	    $i--;
	    $retry++;
	}
	next;
    }
    #print Dumper($res);
    my $xmlin;
    eval {
	$xmlin = XMLin($res->content);
	#print Dumper($xmlin);
	#print ref($xmlin->{poi}),"\n";
	if (ref($xmlin->{poi}) eq "HASH") {
	    process_poi($ua,$xmlin->{poi},$i);
	} elsif (ref($xmlin->{poi}) eq "ARRAY") {
	    for (my $j=0;$j<=$#{$xmlin->{poi}};$j++) {
		process_poi($ua,$xmlin->{poi}->[$j],$i);
	    }
	} ;
    }
}

if ($start != $eind) {
    my ($cookie_jar,$nfreq,$nfres,$content);
    $cookie_jar = HTTP::Cookies->new();
	
# www.poi66.com
$nfreq = HTTP::Request->new(POST => 'http://www.poi66.com/maps/export');
    $cookie_jar->add_cookie_header($nfreq);
    $ua->cookie_jar($cookie_jar);
    
    $nfreq->content_type('application/x-www-form-urlencoded');
    $nfreq->content('abbrev_be=1&abbrev_de=1&abbrev_nl=1&addcity=1&addcountry=0&album=flits&ext=asc&button=Download+ASC');
    $nfres = $ua->request($nfreq);
    
    if ($nfres->is_success) {
	$cookie_jar->extract_cookies($nfres);
	print "\n\n================\n",$cookie_jar->as_string,"\n";
	$content = $nfres->content;
	
	my @lines = split("\n",$content);
	my %speeds;
	
	foreach (@lines) {
		chomp;
		next if /^;/;
		s/^ *//;
		s/ *, */,/g;
		my ($lon,$lat,$desc) = split(",",$_);
		$desc =~ s/\"//g;
		$desc =~ m/\[(\d+)\] *(.*)/;
		my $speed = $1;
		my $rest = $2;
		unless (defined($speed)) {
		    $desc =~ m/\[(rood)\] *(.*)/;
		    $speed = $1;
		    $rest = $2;
		}
		unless (exists($speeds{$speed})) {
			$speeds{$speed} = ();
		}
		push @{$speeds{$speed}},"$lon,$lat,\"$lon $lat $rest\"";
	}
	
	foreach my $speed (keys(%speeds))
	{
		open P,">poi66_$speed.asc" or die "Kan poi66_$speed,asc niet openen: $@\n";
		print P join("\n",@{$speeds{$speed}});
		close P;
	}
	
    }

	# www.navifriends.de 
    
    $nfreq = HTTP::Request->new(GET => 'http://www.navifriends.de/');
    $nfres = $ua->request($nfreq);
    
#print Dumper($nfres);
    if ($nfres->is_success) {
	$cookie_jar->extract_cookies($nfres);
	print "\n\n================\n",$cookie_jar->as_string,"\n";
	
	$nfreq = HTTP::Request->new(POST => 'http://www.navifriends.com/phpbbForum/ucp.php?mode=login');
	$cookie_jar->add_cookie_header($nfreq);
	$ua->cookie_jar($cookie_jar);
	
	$nfreq->content_type('application/x-www-form-urlencoded');
	$nfreq->content('username=mfvl&password=mikel02&login=Anmelden');
	
	$nfres = $ua->request($nfreq);
	if ($nfres->is_success) {
	    
#print Dumper($nfres->);
	    
	    $cookie_jar->extract_cookies($nfres);
	    print "\n\n================\n",$cookie_jar->as_string,"\n";
	    
	    my $refresh = $nfres->header('refresh');
	    print "\nRefresh: $refresh\n";
	    
	    $refresh =~ m/(.*);url=(http:.*)$/;
	    my $delay = $1;
	    my $refstr = $2;
	    
	    sleep($delay);
	    $nfreq = HTTP::Request->new(GET => $refstr);
	    $cookie_jar->add_cookie_header($nfreq);
	    $ua->cookie_jar($cookie_jar);
	    $nfres = $ua->request($nfreq);
	    if ($nfres->is_success) {
		
#print Dumper($nfres);
		
		$cookie_jar->extract_cookies($nfres);
		print "\n\n================\n",$cookie_jar->as_string,"\n";
		
		$content = $nfres->content;
		
		my $pos = index($content,'index.php?Nickname=');
		my $m1 = rindex($content,'"',$pos);
		my $m2 = index($content,'"',$pos);
		
		my $poiurl = substr($content,$m1+1,$m2-$m1-1);
		print $poiurl,"\n";
		
		$nfreq = HTTP::Request->new(GET => $poiurl);
		$nfreq->header('referer' => $refstr);
		$cookie_jar->add_cookie_header($nfreq);
		$ua->cookie_jar($cookie_jar);
		$nfres = $ua->request($nfreq);
		if ($nfres->is_success) {
		    
#print Dumper($nfres);
		    
		    $cookie_jar->extract_cookies($nfres);
		    print "\n\n================\n",$cookie_jar->as_string,"\n";
		    
		    $nfreq = HTTP::Request->new(POST => 'http://www.navifriends.com/nfpois/download.php');
		    $nfreq->header('referer' => $refstr);
		    $nfreq->content_type('application/x-www-form-urlencoded');
		    $nfreq->content('Land=komplett&B1=zusammenstellen');
		    $cookie_jar->add_cookie_header($nfreq);
		    $ua->cookie_jar($cookie_jar);
		    $nfres = $ua->request($nfreq);
		    if ($nfres->is_success) {
			
#print Dumper($nfres);
			
			$cookie_jar->extract_cookies($nfres);
			print "\n\n================\n",$cookie_jar->as_string,"\n";
			
			$content = $nfres->content;
			
			$m1 = index($content,'download1.php?Datei=');
			$m2 = index($content,'>',$m1);
			
			my $poizip = substr($content,$m1,$m2-$m1);
			print $poizip,"\n";
			
			$nfreq = HTTP::Request->new(GET => "http://www.navifriends.com/nfpois/$poizip");
			$nfreq->header('referer' => $refstr);
			$cookie_jar->add_cookie_header($nfreq);
			$ua->cookie_jar($cookie_jar);
			$nfres = $ua->request($nfreq);
			if ($nfres->is_success) {
			    
#print Dumper($nfres);
			    
			    $cookie_jar->extract_cookies($nfres);
			    print "\n\n================\n",$cookie_jar->as_string,"\n";
			    
			    $content = $nfres->content;
			    
			    open X,">pois-Blitzer.zip";
			    print X $content;
			    close X;
			}
		    }
		}
	    }
	}
    }
# radars.bnet.be
    
#    $nfreq = HTTP::Request->new(GET=>"http://radars.bnet.be/servlets/radarsfixes/getFile?ext=ov2&lim=a");
    $nfreq = HTTP::Request->new(POST => 'http://flits.bnet.be/servlets/flitspalen/getFlitspalen');
    $cookie_jar->add_cookie_header($nfreq);
    $ua->cookie_jar($cookie_jar);
    
    $nfreq->content_type('application/x-www-form-urlencoded');
    $nfreq->content('type=1&lim=&cond=1');
    $nfres = $ua->request($nfreq);
    
    if ($nfres->is_success) {
	$cookie_jar->extract_cookies($nfres);
	print "\n\n================\n",$cookie_jar->as_string,"\n";
	$content = $nfres->content;
	
	open X,">radarsfixes.zip";
	print X $content;
	close X;
    }
    
# www.alertegps.fr
    
    $nfreq = HTTP::Request->new(GET => 'http://www.alertegps.com/down_file_zip.asp?id_matos=21&nomfichier=Mio.zip&log=False');
    $cookie_jar->add_cookie_header($nfreq);
    $ua->cookie_jar($cookie_jar);
    
    $nfres = $ua->request($nfreq);
    
    if ($nfres->is_success) {
	$cookie_jar->extract_cookies($nfres);
	print "\n\n================\n",$cookie_jar->as_string,"\n";
	$content = $nfres->content;
	
	open X,">Mio.zip";
	print X $content;
	close X;
    }

}
