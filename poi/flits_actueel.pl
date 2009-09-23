#! /usr/bin/perl -w

use strict;
use DBI;
use CGI;
use LWP::UserAgent;
use Data::Dumper;

my $keyfile='/home/mfvl/download/google_maps_key.txt';

#my $google_key="key=ABQIAAAAhN9BXMs5QuEWNPELVVQK9RSoE5om2NR-mWZGYVWTmUqo7KwJSRRu77NO5gbQQbn3w2uh8WrOsG4P3A";
#my $google_key="key=ABQIAAAAhN9BXMs5QuEWNPELVVQK9RTC2vSp1HLyQT_5vNq3YAhjaSLHtBQLRJzXPKG0mSRDG-gOdSibmV8hWQ";
my $google_key = undef;
my $table="pois.posten";

my $reverseURL = "http://maps.google.com/maps/geo?oe=utf8&sensor=false&output=csv";


open KEY, "<$keyfile" or die "Kan %keyfile niet openen: $@\n";
my $key = <KEY>;
close KEY;
chomp $key;

$google_key = "key=$key";
$reverseURL .= "&$google_key";

my $ua=LWP::UserAgent->new();

my $q=new CGI();
$q->autoEscape(0);
print $q->header();
print $q->start_html("De nieuwste flitsers");
my $ww=$q->param("woonwerk");
if (defined ($ww) && ($ww == 1)) {
    $table="pois.woonwerk";
    $q->param(-name=>'woonwerk',-value=>'0');
    print $q->a({href=>$q->url(-relative)."?woonwerk=0"},"Alles"),"\n";
} else {
    $q->param(-name=>'woonwerk',-value=>'1');
    print $q->a({href=>$q->url(-relative)."?woonwerk=1"},"Alleen woonwerk"),"\n";
}

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl");
my $sth=$dbh->prepare("SELECT lat,lon,file FROM (SELECT lat,lon,file FROM $table WHERE (file='Flitsservice.asc' and updated) or (date(update_date)=date(now()) and (land is NULL or land='NL')) ORDER BY update_date DESC LIMIT 25) as xxx ORDER BY lat,lon");
$sth->execute;
print $q->start_table({border=>1});
print $q->Tr($q->th(['id','lat','lon','file','locatie'])),"\n";
my $url;
my $allurl = "http://maps.google.com/staticmap?maptype=mobile&size=512x512&$google_key&sensor=false&markers=";

my $i=0;
my $translate="0123456789abcdefghijklmnopqrstuvwxyz";
while (my ($lat,$lon,$file) = $sth->fetchrow_array) {
    my $c=substr($translate,$i,1);;
    $url = "http://maps.google.com/staticmap?markers=$lat,$lon,bluef&center=$lat,$lon&span=0.25,0.25&maptype=mobile&size=512x512&$google_key&sensor=false";
    $allurl .= "$lat,$lon,blue$c%7C";
    my $locurl=$reverseURL."&q=$lat,$lon";
    my $res = $ua->get($locurl);
    my ($code,$acc,$locatie) = split(',',$res->content,3);
    $locatie="Fout. code = $code" unless ($code == 200);
    $locatie =~ s/^\"//;
    $locatie =~ s/\"$//;
    print $q->Tr($q->td($c),$q->td($lat),$q->td($lon),
		 $q->td($q->a({href=>$url},$file)),$q->td($locatie)),"\n";
    $i++;
    select(undef,undef,undef,0.100);
}
print $q->end_table;

print $q->hr;

$allurl = substr($allurl,0,length($allurl)-3);
print $q->p($q->a({href=>$allurl}, "Allemaal op 1 kaart"));
print $q->end_html();
