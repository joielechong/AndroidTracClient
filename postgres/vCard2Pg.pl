#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Data::Dumper;
use DBI;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT contact_id FROM mail where mailaddress = ?");
my $sth2 = $dbh->prepare("SELECT contact_id FROM telephone where number = normtel(?) and list");
my $sth3 = $dbh->prepare("SELECT cn FROM contacts where id = ?");
my $sth4 = $dbh->prepare("SELECT mailaddress FROM mail WHERE contact_id=?");
my $sth5 = $dbh->prepare("SELECT number FROM telephone WHERE contact_id=?");
my $sth6 = $dbh->prepare("INSERT INTO mail (contact_id,mailaddress) VALUES(?,?)");
my $sth7 = $dbh->prepare("INSERT INTO telephone (contact_id,number) VALUES(?,?)");
my $sth99= $dbh->prepare("SELECT ?,mail.ids[gs_ser] as ma FROM (SELECT ARRAY[?]) as mail(ids),generate_series(1,?) as gs_ser EXCEPT SELECT contact_id,mailaddress FROM mail where contact_id=?");

my @types = qw(ADR BDAY EMAIL FN N ORG TEL TITLE URL);

while (my $file = shift) {
    my $address_book = Text::vCard::Addressbook->new({'source_file' => $file,});
    
    foreach my $vcard ($address_book->vcards()) {
	my $contact_id;
	my @phones;
	my @emails;
	
	my $fullname = $vcard->fullname();
	my $title = $vcard->title();
	my $bday = $vcard->bday();
	my $url = $vcard->url;
	
	print "Got card for $fullname";
	my $nodes = $vcard->get('email');
	if (defined($nodes)) {
	    foreach my $md (@$nodes) {
		if (defined($md->value)) {
#		    print $md->types(),' ',$md->value,"\n";
		    push @emails,lc($md->value);
		    unless (defined($contact_id)) {
			$sth1->execute(lc($md->value));
			if (my @row=$sth1->fetchrow_array()) {
			    $contact_id=$row[0];
			}
		    }
		}
	    }
	}
	$nodes = $vcard->get('tel');
	if (defined($nodes)) {
	    foreach my $md (@$nodes) {
		if (defined($md->value)) {
#		    print $md->types(),' ',$md->value,"\n";
		    push @phones,$md->value;
		    unless (defined($contact_id)) {
			$sth2->execute($md->value);
			if (my @row=$sth2->fetchrow_array()) {
			    $contact_id=$row[0];
			}
		    }
		}
	    }
	}

	if (defined($contact_id)) {
	    my $process = 0;
	    print " $contact_id ";
	    $sth3->execute($contact_id);
	    if (my @row=$sth3->fetchrow_array()) {
		print ' ',$row[0];
		if (lc($row[0]) ne lc($vcard->fullname())) {
		    print " !!DIFF!! Verwerken? (J/N): ";
		    my $answer = lc(getc());
		    $process = 1 if $answer eq 'j'; 
		} else {
		    $process = 1;
		}
	    } else {
		print ' ??? Toevoegen? (J/N)';
		    my $answer = lc(getc());
		    $process = 1 if $answer eq 'j'; 
		    die "direct gestopt\n" if $answer eq "q";
	    }
	    if ($process) {
		print " Nu verwerken";
		my $mas = "'".join("','",@emails)."'";
		my $count = $#emails + 1;
		$sth99->execute($contact_id,$mas,$count,$contact_id);
		while (my @row=$sth99->fetchrow_array()) {
			print "Nieuwe emails: ",join(",",@row),"\n";
		}
	    }
	    print "\n";
	} else {
	    print "\n";
	}
    }
}
