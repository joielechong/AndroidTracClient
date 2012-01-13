#! /usr/bin/perl -w

use strict;
use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=fietspad");
my $sthupd = $dbh->prepare("SELECT addcall(?,?,?)");

while (1==1) {
    
    open LINK, "</usr/local/apache2/logs/proclink.fifo" or die "proclink cannot open fifo\n";
    
    while (<LINK>) {
#    print STDERR ;
	
	my ($host, $date,$url) = /^([a-f0-9\.:]+ )?\[(.*)\] \/linkhttp\/(.*)$/;
	$host="-" unless $host;
        $host=~s/ *$//;
#    print STDERR $date, "-", $url,"\n";
	next unless defined($url);
	next if $url eq "";
	
	$url = "http://".$url;
	$sthupd->execute($date,$host,$url);
    }
    
    close LINK;
    
    sleep 1;
}
