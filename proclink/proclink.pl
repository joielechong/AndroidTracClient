#! /usr/bin/perl -w

use strict;
use DBI;

my $dbh=DBI::connect("dbi:Pg:dbname=fietspad") or &error;

my $sthup=$dbh->prepare("UPDATE content SET calls=calls+1,lastcal=?,last_visitor=? WHERE url=?");
my $sthin=$dbh->prepare("INSERT INTO content (url,calls,last_visitor,last_call) VALUES(?,1,?,?);

while (1==1) {
    
    open LINK, "</usr/local/apache2/logs/proclink.fifo" or die "proclink cannot open fifo\n";
    
    while (<LINK>) {
#    print STDERR ;
	
	my ($host, $date,$url) = /^([0-9\.]+ )?\[(.*)\] \/linkhttp\/(.*)$/;
	$host="-" unless $host;
        $host=~s/ *$//;
	$url = "http://".$url;
#    print STDERR $date, "-", $url,"-",$host"\n";
my $result=$sthup->execute($url,$host,$date);

	my $count = $result->rows;
	
#    print STDERR "UPDATE 1 url = $url, count = $count\n";
	
	if ($count == 0) {
	    my $url1 = $url."/";
	    $result = $sthup->update($url1,$host,$date);
	    my $count = $result->rows;
#    print STDERR "UPDATE 2 url = $url, count = $count\n";
	    if ($count == 0) {
	    $result=$sthin($host,$date,$url);
#	print STDERR "INSERT result = ",$result->rows\n";
	    }
	}
    }
    
    close LINK;
    
    sleep 1;
}
