#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Data::Dumper;
use DBI;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT contact_id FROM mail WHERE mailaddress = ?");
my $sth2 = $dbh->prepare("SELECT contact_id FROM telephone WHERE number = normtel(?)");
my $sth3 = $dbh->prepare("SELECT cn FROM contacts WHERE id = ?");
my $sth4 = $dbh->prepare("SELECT mailaddress FROM mail WHERE contact_id=?");
my $sth5 = $dbh->prepare("SELECT number FROM telephone WHERE contact_id=?");
my $sth6 = $dbh->prepare("INSERT INTO mail (contact_id,mailaddress) VALUES(?,?)");
my $sth7 = $dbh->prepare("INSERT INTO telephone (contact_id,number) VALUES(?,?)");

my @types = qw(ADR BDAY EMAIL FN N ORG TEL TITLE URL);

while (my $file = shift) {
    my $address_book = Text::vCard::Addressbook->new({'source_file' => $file,});
    
    foreach my $vcard ($address_book->vcards()) {
	my $contact_id;
	my @phones;
	my @emails;
	
	my $fullname = $vcard->fullname();
	my $company = $vcard->org();
	my $title = $vcard->title();
	my $bday = $vcard->bday();
	my $url = $vcard->$url;
	
	print "Got card for $fullname";
	my $nodes = $vcard->get('email');
	if (defined($nodes)) {
	    foreach my $md (@$nodes) {
		if (defined($md->value)) {
#		    print $md->types(),' ',$md->value,"\n";
		    push @email,lc($md->value);
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
		if ($row[0] ne $fullname) {
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
	    }
	    print "Nu verwerken" if $process;
	    print "\n";
	} else {
	    print "\n";
	}
    }
}
