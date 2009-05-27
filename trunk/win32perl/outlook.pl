#! /usr/bin/perl -w

use strict;
use IO::Handle;
use WIN32;
use Win32::OLE qw(in with);
use Win32::OLE::NLS qw(:LOCALE :DATE);
use Data::Dumper;
#BEGIN { $ENV{PATH} = '/usr/ucb:/bin' }
use Socket;
use Carp;
use URI::Escape;

$Data::Dumper::Maxdepth=1;

my $EOL = "\015\012";
my $SafeContact;
my $SafeMail;
my $objOL;
my $olNS;
my $currentFolder;
my $connected = 0;

sub logmsg { print "$0 $$: @_ at ", scalar localtime, "\n" }

sub printHash {
    my $handle = shift;
    my $h=shift;
#    for my $i (keys(%$h)) {
#	print $handle $i," -- ",$h->{$i},"\n" if defined($h->{$i});
#    }
    print $handle Dumper($h),"\n";
}

sub printStdHeader {
    my $handle = shift;
    my $title = shift;

    print $handle "HTTP/1.0 200 OK",$EOL,"Content-type: text/html; charset=iso_8859_1",$EOL,"Server: OutlooktoHTML V0.1",$EOL,$EOL,"<html><head><title>$title</title></head><body><h1>$title</h1>",$EOL;
}

sub listItems {
    my $handle = shift;
    my $folder = shift;
    my $i;

    my $items=$folder->{'Items'};

    if ($items->{'Count'} > 0) {
	print $handle "<h2>Overzicht van items</h2><table><tr><th><Nr</th><th>Type</th><th>Naam</th></tr>",$EOL;
	for ($i=1;$i<=$items->{'Count'};$i++) {
	    my $item=$items->item($i);
#	print $handle "Item $i:"$EOL;
#	printHash($handle,$item));
	    if ($item->{'Class'} == 40) {
		$SafeContact->{'Item'} = $item;
		print $handle "<tr><td align=right>$i</td><td>Contact</td><td><a href=?item=$i>",$$SafeContact{FileAs},"</a></td></tr>\n",$EOL;
	    } elsif ($item->{'Class'} == 43) {
		$SafeMail->{'Item'} = $item;
#	    printHash($handle,$SafeMail);
		print $handle "<tr><td align = right>$i</td><td>Mail</td><td><a href=?item=$i>",$$SafeMail{Subject},"</a></td></tr>",$EOL;
	    } else {
		print $handle "<tr><td align = right>$i</td><td>class=",$$item{Class},"</td><td></td></tr>",$EOL;
	    }
	}
	print $handle "</table>",$EOL;
    }
}

sub showMailItem {
    my $handle = shift;
    my $item = shift;

    $SafeMail->{'Item'} = $item;
    printStdHeader($handle,$SafeMail->{Subject});

    print $handle "<b>From: </b>",$SafeMail->{SenderName}," (",$SafeMail->{SenderEmailAddress},")<br/>",$EOL;
    
    for (my $i=1;$i<=$SafeMail->{Recipients}->Count;$i++) {
	my $rec=$SafeMail->{Recipients}->item($i);
	print $handle "<b>To: </b>" if $rec->{Type} == 1;
	print $handle "<b>Cc: </b>" if $rec->{Type} == 2;
	print $handle $rec->{Name}," (",$rec->{Address},")<br/>",$EOL;
    }
    my $datum=$SafeMail->{SentOn};
    print $handle "<b>Sent: </b>",$datum->Date("d-MMM-yyyy")," ",$datum->Time("HH:mm:ss"),"<br/>",$EOL;
    $datum=$SafeMail->{ReceivedTime};
    print $handle "<b>Received: </b>",$datum->Date("d-MMM-yyyy")," ",$datum->Time("HH:mm:ss"),"<br/>",$EOL;
    print $handle "<p><pre>",$SafeMail->{Body},$EOL,"</pre>",$EOL;

    my $attm = $SafeMail->{Attachments};
    if ($attm->{Count} > 0) {
	print $handle "<h2>Attachments</h2><table><tr><th>Nr</th><th>Naam</th><th>Lengte</th></tr>",$EOL;
	for (my $i;$i<=$attm->{Count};$i++) {
	    my $att=$attm->item($i);
	    print $handle "<tr><td align=right>$i</td><td>",$att->{FileName},"</td><td align=right>$att->{Size}</td></tr>",$EOL;
	}
	print $handle "</table>",$EOL;


	for (my $i;$i<=$attm->{Count};$i++) {
	    print $handle "Dump $i;",$EOL;
	    my $att=$attm->item($i);
	    print $handle "<p><pre>",$EOL;
	    printHash($handle,$att);
	    print $handle "</pre>",$EOL;
	}
    }
    
#    print $handle "<pre>",$EOL;
#    printHash($handle,$SafeMail);
#    print $handle "</pre>",$EOL;
}

sub showContactItem {
    my $handle = shift;
    my $item = shift;

    $SafeContact->{Item} = $item;
    printStdHeader($handle,$SafeContact->{FileAs});
    print $handle "<pre>",$EOL;
    printHash($handle,$SafeContact);
    print $handle "</pre>",$EOL;
}

sub showItem {
    my $handle = shift;
    my $folder = shift;
    my $itemnr = shift;

    my $item = $folder->{Items}->item($itemnr);

    if ($item->{Class} == 40) {
	showContactItem($handle,$item);
    } elsif ($item->{Class} == 43){
	showMailItem($handle,$item);
    }
    print $handle "</body></html",$EOL;
}

sub listFolders {
    my $handle = shift;
    my $folder = shift;
    my $i;

    my $f=$folder->{'Folders'};

    if ($f->{'Count'} > 0) {
	print $handle "<h2>Overzicht van folders</h2><table><tr><th>Naam</th><th>Aantal items</th><th>Aantal folders</th></tr>\n",$EOL;
	for ($i=1;$i<=$f->{'Count'};$i++) {
#	print $handle "Folder $i:"$EOL;
	    my $ff = $f->item($i);
#	printHash($handle,$f->item($i));
	    print $handle "<tr><td><A href=\"",$ff->{'Name'},"/\">";
	    print $handle $ff->{'Name'},"</a></td><td align=right>";
	    print $handle $ff->{'Items'}->{'Count'},"</td><td align=right>";
	    print $handle $ff->{'Folders'}->{'Count'},"</td></tr>",$EOL;
	}
	print $handle "</table>\n",$EOL;
    }
}

sub selecteerFolder {
    my $handle=shift;
    my $newfolder=shift;
    my $folder = $currentFolder;
    my $i;

    my $f=$folder->{'Folders'};

    for ($i=1;$i<=$f->{'Count'};$i++) {
#	print $handle "Folder $i:"$EOL;
	my $ff = $f->item($i);
	if ($newfolder eq $ff->{'Name'}) {
#	    printHash($handle,$ff);
	    $currentFolder = $ff;
	    return 1;
	}
    }
    return 0;
}

sub rootFolder {
    $currentFolder = $olNS;
}

sub connOutlook {
    $connected=0;
    $objOL = Win32::OLE->new('Outlook.Application','Quit');
    if ($objOL) {
	$connected = 1;
	$SafeContact=Win32::OLE->CreateObject("Redemption.SafeContactItem");
	$SafeMail=Win32::OLE->CreateObject("Redemption.SafeMailItem");
	$olNS = $objOL->GetNameSpace("MAPI");
	rootFolder();
    }
    return $connected;
}

sub disconOutlook {
    $objOL->quit();
    $connected = 0;
}

my $port = shift || 5001;
my $proto = getprotobyname('tcp');

($port) = $port =~ /^(\d+)$/                        or die "invalid port";

Win32::OLE->Option(Warn=>0);

socket(Server, PF_INET, SOCK_STREAM, $proto)        || die "socket: $!";
setsockopt(Server, SOL_SOCKET, SO_REUSEADDR,
	   pack("l", 1))   || die "setsockopt: $!";
bind(Server, sockaddr_in($port, INADDR_ANY))        || die "bind: $!";
listen(Server,SOMAXCONN)                            || die "listen: $!";

logmsg "server started on port $port";

my $paddr;
$connected = 0;

CONN: for ( ; $paddr = accept(Client,Server); close Client) {
    my($port,$iaddr) = sockaddr_in($paddr);
    my $name = gethostbyaddr($iaddr,AF_INET);
    
    autoflush Client 1;
    logmsg "connection from $name [",inet_ntoa($iaddr), "]at port $port";
    
    READ: while (<Client>) {
	
	chomp;
	s/\r//;
	s/^[ \t]+//;
	s/[ \t]+$//;

	my ($opdracht,@parameters)=split(' ');
	my $parameters=join(' ',@parameters);
	my $action="showfolder";
	my $item;
	
	if ($opdracht eq "GET") {
	    if (connOutlook() == 0) {
		print Client "HTTP/1.1 500 Server Error",$EOL;
		close Client;
		last READ;
	    }
	    rootFolder();
	    $parameters =~ s: HTTP/\d+\.\d+::;
	    my @path = split('/',uri_unescape($parameters));
	    foreach my $p (@path) {
#		print Client "\"",$p,"\"",$EOL;
		if (substr($p,0,6) eq "?item=") {
		    $item=substr($p,6);
		    if ($item > 0 and $item <= $currentFolder->{Items}->{Count}) {
			$action="showitem";
		    } else {
			print Client "HTTP/1.1 404 Item Error",$EOL;
			last READ;
		    }
		} elsif ($p ne "") {
		    if (selecteerFolder(*Client,$p) == 0) {
			print Client "HTTP/1.1 404 Folder Error",$EOL;
			last READ;
		    }
		}
	    }
	    if ($action eq "showfolder") {
		printStdHeader(*Client,join("/",@path));
		
		listFolders(*Client,$currentFolder);
		listItems(*Client,$currentFolder);
		print Client "</body></html>",$EOL;
		last READ;
	    } elsif ($action eq "showitem") {
		showItem(*Client,$currentFolder,$item);
	    }
	    last READ; 
	}
    }
    close(Client);
    disconOutlook();
}


#Win32::Sleep(60000);
