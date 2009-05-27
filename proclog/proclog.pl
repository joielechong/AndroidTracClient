#! /usr/bin/perl -w

use strict;
use Pg;
use Net::hostent;
use Socket;

my %browsers;
my %requests;
my %hosts;
my %referers;
my $result;

sub query {
    my $conn = shift;
    my $request = shift;
#    print $request,"\n";
    my $result=$conn->exec($request);
    print $request,"\n",$result->resultStatus," ",$result->ntuples,"\n" if $result->resultStatus==7;
    return $result;
}

my $conn=Pg::connectdb("dbname=logfiles") or die "Kan logfiles db niet openen";
my $status=$conn->status;
die("Logfiles db error") if $status ;

    query($conn,"delete from logfile where server=1 and logfilestamp in (select max(logfilestamp) from logfile where server=1);");
    query($conn,"delete from logfile where server=2 and logfilestamp in (select max(logfilestamp) from logfile where server=2);");

$result=query($conn,"select * from browsers;");
while (my @rows=$result->fetchrow()) {
    $browsers{$rows[1]}=$rows[0];
}

$result=query($conn,"select * from requests;");
while (my @rows=$result->fetchrow()){
    $requests{$rows[1]}=$rows[0];
}

$result=query($conn,"select * from hosts;");
while (my @rows=$result->fetchrow()) {
    $hosts{$rows[1]}=$rows[0] if $rows[1];
    $hosts{$rows[2]}=$rows[0] if $rows[2];
}

$result=query($conn,"select * from urls;");
while (my @rows=$result->fetchrow()) {
    $referers{$rows[1]} = $rows[0];
}

open PIJP,"find /var/log/ -type f -name \"*_access.log*\" | ";
while (<PIJP>) {
    my $file = $_;
    my $server=0;
    my $ts;

    $server=1 if $file =~ /.*httpd_access.*/;
    $server=2 if $file =~ /.*fietspad_access.*/;

    m/.*_access.log.(\d+).gz$/;
    if ($1) {
	$ts = $1;
    } else {
	m/.*access.log.(\d+)$/;
	$ts=$1;
    }

    $result=query($conn,"select count(id) from logfile where server=$server and logfilestamp=$ts;");
    my $count=$result->getvalue(0,0);
    next if $count >0;

    if (/.*.gz$/) {
	open INP,"zcat $file | " or die "$file is er niet";
    } else {
	open INP,"< $file" or die "$file is er niet";
    }

#    query($conn,"delete from  logfile where server=$server and logfilestamp=$ts;");

    print $file;

    while (<INP>) {
#	print;
	chomp;

	my $remotehost;
	my $logname;
	my $username;
	my $datetime;
	my $request;
	my $status;
	my $size;
	my $referer="-";
	my $browsername="";
	/^([^ ]+) ([^ ]+) ([^ ]+) \[([^\]]+)\] \"([^\"]+)\" (\d+) ([^ ]+) \"([^\"]+)\" \"([^\"]+)\"/;
	if ($1) {
	    $remotehost=$1;
	    $logname=$2;
	    $username=$3;
	    $datetime=$4;
	    $request=$5;
	    $status=$6;
	    $size=$7;
	    $referer=$8;
	    $browsername=$9;
	} else{
	    /^([^ ]+) ([^ ]+) ([^ ]+) \[([^\]]+)\] \"([^\"]+)\" (\d+) ([^ ]+)/;
	    $remotehost=$1;
	    $logname=$2;
	    $username=$3;
	    $datetime=$4;
	    $request=$5;
	    $status=$6;
	    $size=$7;
	}
	$size=0 if $size eq "-";

	next if ($request =~ /CONNECT .*/);
	next if ($request =~ /GET http:\/\/.*/);
	next if ($request =~ /POST http:\/\/.*/);
	next if ($request =~ /HEAD http:\/\/.*/);
	next if ($request =~ /GET ftp:\/\/.*/);
	next if ($request =~ /POST ftp:\/\/.*/);
	next if ($request =~ /HEAD ftp:\/\/.*/);
	next if ($request =~ /.*stats4all.*/);


	$request =~ s:/cgi-bin/red.cgi?url=http.//:/linkhttp/:;
	$request =~ s: HTTP/...::;

	$logname="" unless $logname;
	$username="" unless $username;
	$username="" if $username eq "-";
	$logname="" if $logname eq "-";

#
#  Verwerk hostname
#

	my $hostid = $hosts{$remotehost};
	unless ($hostid) {
	    if ($remotehost =~ m/^\d+\.\d+\.\d+\.\d+$/) {
		print $remotehost,"\n";
		my $h=gethost($remotehost);
		if ($h) {
		    print $h->name,": ",inet_ntoa($h->addr)," ",join(", ", @{$h->aliases});
		    print "\n";
		    query($conn,"insert into hosts (ip,hostname) values ('$remotehost','".$h->name."');");
		    $result=query($conn,"select * from hosts where hostname='".$h->name."';");
		    $hostid=$result->getvalue(0,0);
		    $hosts{$h->name}=$hostid;
		    $hosts{$remotehost} = $hostid;
		} else {
		    query($conn,"insert into hosts (ip) values ('$remotehost');");
		    $result = query($conn,"select * from hosts where ip='$remotehost';");
		    $hostid=$result->getvalue(0,0);
		    $hosts{$remotehost}=$hostid;
		}
	    } else {
		print $remotehost,"\n";
		query($conn,"insert into hosts (hostname) values ('$remotehost');");
		$result = query($conn,"select * from hosts where hostname='$remotehost';");
		$hostid=$result->getvalue(0,0);
		$hosts{$remotehost}=$hostid;
	    }
	}
#
# Verwerk browsernaam
#

	    
	my $browsertext = $browsername;
	$browsertext =~ s/\'/\\\'/g;
	my $browserid = $browsers{$browsername};
	unless ($browserid) {
	    print $browsername,"\n";
	    query($conn,"insert into browsers (browsername) values ('$browsertext');");
	    $result = query($conn,"select id from browsers where browsername='$browsertext';");
	    $browserid=$result->getvalue(0,0);
	    $browsers{$browsername} = $browserid;
	}

#
# Verwerk request
#
	my $requesttext = $request;
	$requesttext =~ s/\'/\\\'/g;
	my $requestid=$requests{$request};
	unless ($requestid) {
	    print $request,"\n";
	    query($conn,"insert into requests (request) values ('$requesttext');");
	    $result = query($conn,"select id from requests where request='$requesttext';");
	    $requestid=$result->getvalue(0,0);
	    $requests{$request} = $requestid;
	}

#
# Verwerk referer
#
	my $referertext = $referer;
	$referertext =~ s/\'/\\\'/g;
	my $refererid = $referers{$referer};
	unless ($refererid) {
	    print $referer,"\n";
	    query($conn,"insert into urls (url) values ('$referertext');");
	    $result = query($conn,"select id from urls where url='$referertext';");
	    $refererid=$result->getvalue(0,0);
	    $referers{$referer} = $refererid;
	}

# 
# Sla op
#

	my $cmd=
	    "insert into logfile (host_id,logname,username,time,request_id,status,bytes,referer_id,browser_id,logfilestamp,server) " .
	    "values ($hostid,'$logname','$username','$datetime',$requestid,$status,$size,$refererid,$browserid,$ts,$server);";
	$result=query($conn,$cmd);
    }
	
    close INP;
}
close PIJP;
