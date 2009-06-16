#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Data::Dumper;
use DBI;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT contact_id FROM mail where mailaddress = ?");
my $sth2 = $dbh->prepare("SELECT contact_id FROM telephone where number = normtel(?)");
my $sth3 = $dbh->prepare("SELECT cn FROM contacts where id = ?");

my @types = qw(ADR BDAY EMAIL FN N ORG TEL TITLE URL);

while (my $file = shift) {
    my $address_book = Text::vCard::Addressbook->new({'source_file' => $file,});
    
    foreach my $vcard ($address_book->vcards()) {
	my $contact_id;
	print "Got card for " . $vcard->fullname();
	my $nodes = $vcard->get('email');
	if (defined($nodes)) {
	    foreach my $md (@$nodes) {
		if (defined($md->value)) {
#		    print $md->types(),' ',$md->value,"\n";
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
	    print " $contact_id ";
	    $sth3->execute($contact_id);
	    if (my @row=$sth3->fetchrow_array()) {
		print ' ',$row[0];
		if ($row[0] ne $vcard->fullname) {
		    print " !!DIFF!!";
		}
	    } else {
		print ' ???';
	    }
	    print "\n";
	} else {
	    print "\n";
	}
    }
}
