#! /usr/bin/perl -w

use strict;
use XML::Simple;
use LWP::UserAgent;
use LWP::ConnCache;
use HTTP::Cookies;
use Data::Dumper;

my @proxies = (
#   '189.70.29.113:80',
    '150.188.31.2:3128',
    '148.233.239.23:80',
    '84.255.246.20:80',
#   '189.109.46.210:3128',
    '207.250.81.2:3128',
    '217.58.153.172:8080',
    '202.54.61.99:8080',
    '200.174.85.195:3128',
    '190.90.27.30:8080',
    '67.228.115.150:80',
    '190.199.230.74:3128',
    '219.134.186.106:3128',
    '63.220.6.34:80',
    '119.70.40.101:8080',
    '211.115.185.42:80',
    '89.248.194.212:3128',
    '202.171.26.146:8080',
    '83.236.237.163:3128',
    '64.125.136.28:80',
    '69.162.118.246:3128',
    '200.61.25.1:8080',
    '189.53.181.7:80',
    '94.76.201.172:3128',
    '202.142.145.138:8080',
    '189.123.117.246:8080',
    '200.14.96.57:80',
    '200.54.145.98:8080',
    '58.240.237.32:80',
    '203.160.1.94:80',
    '60.12.227.209:3128',
    '203.160.1.121:80',
    '212.102.0.104:80',
    '61.172.246.180:80',
    '82.115.86.162:3128',
    '189.39.115.185:3128',
    '200.30.101.20:8080',
    '203.160.1.75:80',
    '88.191.80.237:3128',
    '89.212.253.19:8080',
    '64.251.57.198:8080',
    '212.44.145.21:80',
    '200.139.78.114:3128',
    '217.23.180.98:3128',
    '66.63.165.9:3128',
    '218.24.180.12:80',
    '77.73.162.21:3128',
    '202.111.189.159:3128',
    '77.244.218.34:3128',
    '190.139.101.154:8080',
    '77.71.0.149:8080',
    '189.50.116.113:8080',
    '218.56.64.210:8080',
    '94.84.179.4:3128',
    '190.97.144.194:6588',
    '118.220.175.207:80',
    '200.35.163.212:3128',
    '80.148.22.116:8080',
    '61.172.249.94:80',
    '86.61.211.47:8080',
    '216.101.231.130:8000',
    '222.247.62.195:8080',
    '193.171.32.6:80',
    '202.98.23.116:80',
    '202.98.23.114:80',
    '124.40.121.7:80',
    '201.20.89.10:3128',
    '203.160.1.85:80',
    '59.120.244.23:3128',
    '196.219.18.34:80',
    '212.143.227.248:3128',
    '125.41.181.59:8080',
    '98.243.10.242:80',
    '200.148.230.217:3128',
    '195.54.22.74:8080',
    '221.6.62.90:8080',
    '189.45.48.7:8080',
    '200.174.85.195:3128',
    '200.204.154.29:6588',
    '209.17.186.25:8080',
    '217.23.68.2:8080',
    '211.115.185.44:80',
    '92.63.49.201:8080',
    '190.27.218.105:8080',
    '77.242.233.102:8080',
    '199.193.13.202:80',
    '211.99.188.218:80',
    '203.160.1.112:80',
    '89.207.211.66:80',
    '203.162.183.222:80',
    '202.152.27.130:8080',
    '124.81.224.174:8080',
    '199.71.215.50:3128',
    '122.226.12.28:3128',
    '123.233.121.164:80',
    '59.36.98.154:80',
    '199.71.213.7:3128',
    '77.242.33.5:80',
    '98.192.125.23:80',
    '190.254.85.211:3128',
    '94.23.47.56:3128',
    '83.2.212.9:8080',
    '219.134.186.106:3128',
    '217.23.180.98:3128',
    '120.143.250.8:80',
    '190.199.230.74:3128',
    '202.95.128.126:3128',
    '69.162.118.246:3128',
    '217.70.61.54:3128',
    '80.68.95.142:3128',
    '200.204.154.29:6588',
    '192.116.226.69:8080',
    '66.63.165.7:3128',
    '61.172.246.180:80',
    '148.233.239.23:80',
    '190.139.101.154:8080',
    '211.115.185.42:80',
    '212.1.95.50:8080',
    '62.134.53.182:3128',
    '203.160.001.103:80',
    '66.90.234.124:8080',
    '189.53.181.7:80',
    '60.12.227.209:3128',
    '200.30.101.2:8080',
    '61.172.249.96:80',
    '211.115.185.41:80',
    '221.192.132.194:80',
    '200.45.199.189:8080',
    '81.187.204.161:6588',
    '77.244.218.34:3128',
    '89.248.194.212:3128',
    '63.220.6.34:80',
    '94.76.201.172:3128',
    '203.162.183.222:80',
    '203.160.1.75:80',
    '77.242.233.44:8080',
    '59.36.98.154:80',
    '202.98.23.116:80',
    '203.99.131.186:8080',
    '189.127.143.70:3128',
    '189.50.116.113:8080',
    '195.54.22.74:8080',
    '189.56.61.33:3128',
    '218.242.239.61:8080',
    '61.172.249.94:80',
    '118.220.175.207:80',
    '200.179.88.180:3128',
    '202.152.27.130:8080',
    '86.61.211.47:8080',
    '202.248.42.25:80',
    '80.148.17.149:80',
    '212.44.145.21:80',
    '190.27.218.105:8080',
    '202.111.189.159:3128',
    '98.192.125.23:80',
    '203.160.1.121:80',
    '189.50.119.1:8080',
    '150.188.31.2:3128',
    '209.17.186.25:8080',
    '212.143.227.248:3128',
    '98.243.10.242:80',
    '202.71.98.201:3128',
    '59.39.145.178:3128',
    '77.73.162.21:3128',
    '200.199.25.178:3128',
    '82.115.86.162:3128',
    '69.197.153.46:3128',
    '200.148.230.217:3128',
    '200.61.25.1:8080',
    '201.222.99.12:3128',
    '84.255.246.20:80',
    '66.63.165.9:3128',
    '81.18.116.70:8080',
    '218.101.6.204:80',
    '88.191.80.237:3128',
    '207.250.81.2:3128',
    '211.115.185.44:80',
    '58.65.240.10:3128',
    '203.160.1.112:80',
    '202.171.26.146:8080',
    '212.102.0.104:80',
    '110.8.253.100:80',
    '59.120.244.23:3128',
    '79.170.43.72:3128',
    '122.226.12.28:3128',
    '203.160.1.66:80',
    '200.174.85.193:3128',
    '119.235.25.242:8080',
    '121.97.128.19:3128',
    '190.102.206.48:8080',
    '141.85.118.1:80',
    '84.92.192.146:3128',
    '119.70.40.102:8080',
    '201.73.45.70:3128',
    '58.56.147.42:3128',
    '213.180.131.135:80',
    '62.168.41.61:3128',
    '202.98.23.114:80',
    '221.6.62.90:8080',
    '213.180.131.135:80',
    '203.160.1.85:80',
    '87.66.29.96:80',
    '60.253.114.30:3128',
    '202.98.23.114:80',
    '211.215.17.56:80',
    '67.228.115.150:80',
    '64.125.136.28:80',
    '62.168.41.61:3128',
    '58.56.147.42:3128',
    '87.66.29.96:80',
    '60.253.114.30:3128',
    '203.160.1.85:80',
    '211.215.17.56:80',
    '221.6.62.90:8080',
    '64.125.136.28:80',
    '67.228.115.150:80'
    );

my @xmlsrc=( 
	     "http://www.flitspaal.nl/poi_flitspalen.xml",
	     "http://www.bruxelles5.info/POI/poi_bruxelles5.xml",
	     "http://www.goedkooptanken.nu/tomtom/pois.xml",
	     "http://flitsservice.com/poi_edit_fs.xml"
	     );

my @groups=(
	     	['standaard_installatie1'],
	     	["Radars Belges", 'Radars Francais'],
	     	['Nederland'],
	     	['Flitsservice.nl;Actuele mobiele controles']
	   );

my @usernames=('michiel@van-loon.xs4all.nl',
	       "",
	       "",
	       "");

my @passwords=('mikel02',
	       "",
	       "",
	       "");

my $cache = LWP::ConnCache->new;

my $proxy=$proxies[int(rand(1+$#proxies))];
print STDERR "Proxy used = $proxy\n";
my $ua = LWP::UserAgent->new;
$ua->agent("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7");
$ua->proxy(['http', 'ftp'], "http://".$proxy."/");
#$ua->proxy(['http', 'ftp'], 'http://67.69.254.254:80/');
$ua->no_proxy('flitspaal.nl','bruxelles5.info','goedkooptanken.nu','bnet.be','navifriends.de','navifriends.com','flitsservice.nl');
$ua->conn_cache($cache);

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
    my $url = shift;
# Create a request
    my $req = HTTP::Request->new(GET => $url);
# Pass request to the user agent and get a response back

    while ($#_ >= 0) {
        my $key = shift;
        my $value = shift;
        $req->header($key=>$value);
    }

    return $ua->request($req);
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
            $res = get_url($lastmodified);
	    my $rmstring = $res->content;
	    chomp($rmstring);
	    print "$description $lmstring $rmstring\n";
	    my ($dag_remote, $tijd_remote)=split(" ",$rmstring);
	
	    return if ($dag_remote < $dag_local);
	    return if ($dag_remote == $dag_local) && ($tijd_remote <= $tijd_local);
	    $res = get_url($url);
	} else {
	    $res = get_url($url,'if_modified_since'=>$gmtijd);
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
}

sub is_integer      { $_[0] =~ /^[+-]?\d+$/       }

#print $#xmlsrc,"\n";

my $start=0;
my $eind=$#xmlsrc;

my $arg = shift;
if (defined($arg) && is_integer($arg)) {
	$start=$arg;
	$eind=$arg;
}

for (my $i=$start;$i<=$eind;$i++) {
    print $xmlsrc[$i],"\n";
    my $req = HTTP::Request->new(GET => $xmlsrc[$i]);
    $req->header(UA_CPU => 'x86');
    #print Dumper($req);
    my $res = $ua->request($req);
    #print Dumper($res);
    my $xmlin = XMLin($res->content);
    #print Dumper($xmlin);
    #print ref($xmlin->{poi}),"\n";
    if (ref($xmlin->{poi}) eq "HASH") {
	process_poi($ua,$xmlin->{poi},$i);
    } elsif (ref($xmlin->{poi}) eq "ARRAY") {
	for (my $j=0;$j<=$#{$xmlin->{poi}};$j++) {
	    process_poi($ua,$xmlin->{poi}->[$j],$i);
	}
    }
}

if ($start != $eind) {

# www.navifriends.de 

    my $cookie_jar = HTTP::Cookies->new();
    my $nfreq = HTTP::Request->new(GET => 'http://www.navifriends.de/');
    my $nfres = $ua->request($nfreq);
    
#print Dumper($nfres);
    
    $cookie_jar->extract_cookies($nfres);
    print "\n\n================\n",$cookie_jar->as_string,"\n";
    
    $nfreq = HTTP::Request->new(POST => 'http://www.navifriends.com/phpbbForum/ucp.php?mode=login');
    $cookie_jar->add_cookie_header($nfreq);
    $ua->cookie_jar($cookie_jar);
    
    $nfreq->content_type('application/x-www-form-urlencoded');
    $nfreq->content('username=mfvl&password=mikel02&login=Anmelden');
    
    $nfres = $ua->request($nfreq);
    
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
    
#print Dumper($nfres);
    
    $cookie_jar->extract_cookies($nfres);
    print "\n\n================\n",$cookie_jar->as_string,"\n";
    
    my $content = $nfres->content;
    
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
    
#print Dumper($nfres);
    
    $cookie_jar->extract_cookies($nfres);
    print "\n\n================\n",$cookie_jar->as_string,"\n";
    
    $content = $nfres->content;
    
    open X,">pois-Blitzer.zip";
    print X $content;
    close X;

    # radars.bnet.be

#    $nfreq = HTTP::Request->new(GET=>"http://radars.bnet.be/servlets/radarsfixes/getFile?ext=ov2&lim=a");
    $nfreq = HTTP::Request->new(POST => 'http://flits.bnet.be/servlets/flitspalen/getFlitspalen');
    $cookie_jar->add_cookie_header($nfreq);
    $ua->cookie_jar($cookie_jar);
    
    $nfreq->content_type('application/x-www-form-urlencoded');
    $nfreq->content('type=1&lim=&cond=1');
    $nfres = $ua->request($nfreq);

    $content = $nfres->content;

    open X,">radarsfixes.zip";
    print X $content;
    close X;
}
