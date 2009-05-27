#! /usr/bin/perl -w

use strict;
use IO::Handle;
use WIN32;
use Win32::OLE qw(in with);
use Win32::OLE::NLS qw(:LOCALE :DATE);
use Data::Dumper;
#BEGIN { $ENV{PATH} = '/usr/ucb:/bin' }
use CGI;
use Carp;
use URI::Escape;

$Data::Dumper::Maxdepth=2;

my $EOL = "\015\012";
my $SafeContact;
my $SafeMail;
my $objOL;
my $olNS;
my $currentFolder;
my $connected = 0;

sub printHash {
    my $q = shift;
    my $h=shift;

#    for my $i (keys(%$h)) {
#	print $handle $i," -- ",$h->{$i},"\n" if defined($h->{$i});
#    }
    print $q->pre(Dumper($h)),"\n";
}

sub listItems {
    my $q = shift;
    my $folder = shift;
    my $i;

    my $items=$folder->{'Items'};

    if ($items->{'Count'} > 0) {
	print $q->h2('Overzicht van items');
	print "<table>";
	print $q->Tr($q->th('Nr'),$q->th('Type'),$q->th('Naam')),"\n";
	for ($i=1;$i<=$items->{'Count'};$i++) {
	    my $item=$items->item($i);
#	print $handle "Item $i:"$EOL;
#	printHash($handle,$item));
	    if ($item->{'Class'} == 40) {
		$SafeContact->{'Item'} = $item;
		print "<tr><td align=right>$i</td><td>Contact</td><td><a href=?item=$i>",$$SafeContact{FileAs},"</a></td></tr>\n",$EOL;
	    } elsif ($item->{'Class'} == 43) {
		$SafeMail->{'Item'} = $item;
#	    printHash($handle,$SafeMail);
		print "<tr><td align = right>$i</td><td>Mail</td><td><a href=?item=$i>",$$SafeMail{Subject},"</a></td></tr>",$EOL;
	    } else {
		print "<tr><td align = right>$i</td><td>class=",$$item{Class},"</td><td></td></tr>",$EOL;
	    }
	}
	print "</table>",$EOL;
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
}

sub listFolders {
    my $q = shift;
    my $folder = shift;
    my $i;

    my $f=$folder->{'Folders'};

    if ($f->{'Count'} > 0) {
	print $q->h2('Overzicht van folders');
	print "<table>";
	print $q->Tr($q->th('Naam'),$q->th('Aantal items'),$q->th('Aantal folders')),"\n";
	for ($i=1;$i<=$f->{'Count'};$i++) {
#	print $handle "Folder $i:"$EOL;
	    my $ff = $f->item($i);
#	printHash($handle,$f->item($i));
	    print "<tr><td><A href=\"",$ff->{'Name'},"/\">";
	    print $ff->{'Name'},"</a></td><td align=right>";
	    print $ff->{'Items'}->{'Count'},"</td><td align=right>";
	    print $ff->{'Folders'}->{'Count'},"</td></tr>",$EOL;
	}
	print  "</table>\n";
    }
}

sub selecteerFolder {
    my $q = shift;
    my $newfolder=shift;
    my $folder = $currentFolder;
    my $i;

#    print $q->p($currentFolder->{Name}),"\n";
    printHash($q,$folder);
    my $f=$folder->{Folders};

    return 0 unless defined($f->{Count});

    for ($i=1;$i<=$f->{Count};$i++) {
	print $q->p("Folder $i:"),"\n";
	my $ff = $f->item($i);
	if ($newfolder eq $ff->{Name}) {
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

Win32::OLE->Option(Warn=>1);
#Win32::OLE->Initialize(Win32::OLE::COINIT_OLEINITIALIZE);
my $q = new CGI;
print $q->header(-X_Warn=>0);
print $q->start_html("Outlook2Web V0.1"),"\n";

my $path=$q->param('path');
my $item=$q->param('item');
my $attnr=$q->param('attnr');

$path="/" unless $path;
$item=-1 unless $item;
$attnr=-1 unless $attnr;

print $q->p("path = $path, item = $item, attnr = $attnr"),"\n";

if (connOutlook() == 0) {
    print $q->h1("Connection to Outlook failed");
    print $q->end_html;
    exit();
} else {
    print $q->p("connection ok"),"\n";
    printHash($q,$olNS);
    exit();
    my @path;
    if ($path eq "/") {
	print $q->p("rootfolder"),"\n";
	$path[0]="";
	rootFolder();
    } else {
	@path = split('/',$path);
	foreach my $p (@path) {
	    print  $q->p($p),"\n";
	    if (selecteerFolder($q,$p) == 0) {
		print $q->h1('404 Folder Error');
		disconOutlook();
		exit();
	    }
	}
    }
    
#    if ($item > 0 and $item <= $currentFolder->{Items}->{Count}) {
#	showItem($q,$currentFolder,$item);
#    } else {
#	print $q->h1(join('/',@path));
#	
#	listFolders($q,$currentFolder);
#	listItems($q,$currentFolder);
#	print $q->end_html();
#    }
    disconOutlook();
}
