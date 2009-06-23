#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Term::ReadKey;
use DBI;
use Data::Dumper;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT co.id,cn,voornaam,achternaam,tussenvoegsel,geboortedatum,company,webpagina,function FROM contacts AS co,catcontact AS cc, categories AS ca WHERE ca.categorie='mobielm' and ca.id=cc.cat_id and cc.contact_id=co.id ORDER BY cn");
my $sth2 = $dbh->prepare("SELECT mailaddress,type,priority FROM mail where contact_id=?");
my $sth3 = $dbh->prepare("SELECT number,tel_type,prio FROM telephone where contact_id=?");

my $addressbook = Text::vCard::Addressbook->new();

$sth1->execute();
while (my @row=$sth1->fetchrow_array()) {
    my $vcard = $addressbook->add_vcard();
    $vcard->fullname($row[1]);
    $vcard->version('2.1');
    my $name = $vcard->add_node({node_type=>'N'});
    $name->given($row[2]) if defined($row[2]);
    if (defined($row[3])) {
	my $family=$row[3];
	$family .= ", ".$row[4] if defined($row[4]);
	$name->family($family);
    } elsif (defined($row[6])) {
	$name->family($row[6]);
    }
    $vcard->bday($row[5]) if defined($row[5]);
    $vcard->url($row[7]) if defined($row[7]);
    $vcard->title($row[8]) if defined($row[8]);
    if (defined($row[6])) {
	my $org=$vcard->add_node({node_type =>'org'});
	$org->name($row[6]);
    }

#    print join(", ",@row),"\n";
    $sth2->execute($row[0]);
    while (my @rowm=$sth2->fetchrow_array()) {
#	print " Mail: ",join(", ",@rowm),"\n";
	my $mail = $vcard->add_node({node_type=>'EMAIL',types=>'INTERNET'});
	$mail->params({'INTERNET'=>1});
	$mail->params({'INTERNET;PREF'=>1}) if defined($rowm[2]) and $rowm[2] == 1;
	$mail->value($rowm[0]);
    }
    $sth3->execute($row[0]);
    while (my @rowt=$sth3->fetchrow_array()) {
#	print " Tel : ",join(", ",@rowt),"\n";
	my $tel = $vcard->add_node({node_type=>'TEL'});
	if (defined($rowt[1])) {
	$tel->params({'HOME'=>1}) if $rowt[1] eq 'Prive';
	$tel->params({'CELL'=>1}) if $rowt[1] eq 'Mobiel';
	$tel->params({'WORK'=>1}) if $rowt[1] eq 'Werk';
	}
	$tel->value($rowt[0]);
    }
}

my $vcf = $addressbook->export();
#$vcf =~ s/TYPE=//g;
print $vcf,"\n";
