#! /usr/bin/perl -w

use strict;

my $USERAGENT="Mozilla/4.04 [en] (MFvL;I)";
my @MAAND=('','JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC');

use LWP::UserAgent;
use HTTP::Cookies;
use strict;

my $cookie_jar=HTTP::Cookies->new(FILE=>"$ENV{HOME}/.cookies_aex",AutoSave =>1);

my $date="";

open OUT,"| iconv -f ISO8859-1 -t UTF-8 >bsn/data/aex_$$.bsn" or die "Kan niet openen\n";

my @codes=(1,24,3,18,2,41,42,43,44,45,46,50,11,47,48,19,13);
#@codes=(8);
my $stdurl="http://www.aex.nl/scripts/marktinfo/koersrubriek.asp?taal=nl&rubriek=";
	   my $num;

foreach $num (@codes) {
#    print $stdurl.$num,"\n";
    my $ua=new LWP::UserAgent;
    
    $ua->agent($USERAGENT);
#    $ua->proxy('http','http://mikel:80/');
    
    my $req = new HTTP::Request GET => $stdurl.$num;
    $cookie_jar->add_cookie_header($req);
#    print "\nRequest Headers\n===========\n";
#    print $req->headers_as_string;
    my $res = $ua->request($req);
    
    if ($res->is_success) {
#	print "\nResponse Headers\n===========\n";
#	print $res->headers_as_string;
	$cookie_jar->extract_cookies($res);
        my @lines=split("\n",$res->content);
        foreach (@lines) {
	    chomp;
#	    print OUT;
#	    print OUT "\n";
	    if (/<TABLE/) {
		/.*per (\d+) (...) (\d+).*/;
		my $dag=$1;
		my $jaar=($3 %100);
		my $maand=$MAAND[0];
		my $i;
		for ($i=1;$i<=12;$i++) {
		    $maand=$i if $MAAND[$i] eq uc($2);
		}
		my $newdat=sprintf "%2.2d-%s-%2.2d",$dag,uc($2),$jaar;
		print OUT "0/1/",uc($newdat),"\n" if $newdat ne $date;
		$date=$newdat;
		next;
	    }

	    next unless /up.gif/ or /dn.gif/ or /eq.gif/;
	    s/&nbsp;/0/g;
	    s/<T[HR] [^>]*>//g;
	    s/<A [^>]*.//g;
	    s/<\/A>//g;
	    s/<IMG [^>]*>//g;
	    s/<\/T[DH]>//g;
	    s/<TD[^>]*>/;/g;
	    s/ *;/;/g;
	    s/;<\/TR>//;
	    s/[AByX\+\-];/;/g;
	    s/[AByX\+\-]$//;

            next if /Stock;Current/;
	    next if /copyright/;
	    my ($stock,$current,$diff,$time,$bied,$laat,$volume,$high,$low,$open)=split(";",$_);
	    $open=~s/(.*\d+).*/$1/;
	    $current=~s/(.*\d+).*/$1/;
	    $open=~s/,//g;
	    $high=~s/,//g;
	    $low=~s/,//g;
	    $current=~s/,//g;
	    print OUT join("|",9,30,uc($stock),$volume,$open,$high,$low,$current),"\n"
        }
    } else {
        print "Bad luck this time\n";
    }
}
close OUT;
