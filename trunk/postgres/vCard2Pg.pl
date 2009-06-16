#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Data::Dumper;
my @types = qw(ADR BDAY EMAIL FN N ORG TEL TITLE URL);

while (my $file = shift) {
	my $address_book = Text::vCard::Addressbook->new({'source_file' => $file,});

	foreach my $vcard ($address_book->vcards()) {
		print "Got card for " . $vcard->fullname() . "\n";
		my $nodes = $vcard->get('tel');
		if (defined($nodes)) {
			foreach my $md (@$nodes) {
				print $md->types(),' ',$md->value,"\n";
			}
		}
		$nodes = $vcard->get('email');
		if (defined($nodes)) {
			foreach my $md (@$nodes) {
				print $md->types(),' ',$md->value,"\n" if defined($md->value);
			}
		}
	}
}
