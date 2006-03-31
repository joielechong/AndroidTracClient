#! /usr/bin/perl -w

use strict;
use DBI;
use Net::LDAP;
use Net::LDAP::Entry;
use Encode;
use Data::Dumper;

my $debug = 0;

sub check_attr_list {
    my $entry  = shift;
    my $newref = shift;
    my $attr   = shift;
    my $oldref;
    my $retval = 0;

    $oldref = $entry->get_value($attr,asref=>1) if $entry->exists($attr);

    print Dumper($oldref,$newref) if $debug;

    foreach my $ent (@$oldref) {
      my $temp = grep {$_ eq $ent} @$newref;
      print "oud in nieuw : ",$ent," ",$temp,"\n" if $debug;
      if ($temp == 0) {
        $entry->delete($attr => ["$ent"]);
        $retval = 1;
      }
    }
    foreach my $ent (@$newref) {
      my $temp = grep {$_ eq $ent} @$oldref;
      print "nieuw in oud : ",$ent," ",$temp,"\n" if $debug;
      if ($temp == 0) {
        $entry->add($attr => "$ent");
        $retval = 1;
      } 
    }
    return $retval;
}

sub check_attrs {
    my $entry   = shift;
#    my $newval  = encode("utf8",shift);
    my $newval  = shift;
    my $attr    = shift;
    my $oldval;

    if ($entry->exists($attr)) {
	$oldval = $entry->get_value($attr);
	if ($oldval eq $newval) {
	    return 0;
	}
	$entry->replace($attr => $newval);
    } else {
	$entry->add($attr => $newval);
    }
    return 1;
}

sub check_attr {
    my $entry = shift;
    my $href  = shift;
    my $dbfld = shift;
    my $attr  = shift;
    my $newval;
    my $oldval;

    if ($href->{$dbfld}) {
	return check_attrs($entry,$href->{$dbfld},$attr);
    } else {
	if ($entry->exists($attr)) {
	    $entry->delete($attr => []) if $entry->exists($attr);
	} else {
	    return 0;
	}
    }
    return 1;
}

sub print_ref {
    my $id = shift;
    my $href = shift;
    
    if ($debug != 0) {
	print "$id ==========================\n";
	foreach my $key (keys %$href) {
	    print $key," => ",$href->{$key},"\n" if $href->{$key};
	}
    }
}

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl") or die "$@";
my $sth_contact=$dbh->prepare("select * from contacts order by cn");
my $sth_naw=$dbh->prepare("select * from  naw where contact_id=?");
my $sth_tel=$dbh->prepare("select * from  telephone where contact_id=? and tel_type=?");
my $sth_mail=$dbh->prepare("select * from  mail where contact_id=?");
my $sth_fax=$dbh->prepare("select * from  fax where contact_id=?");
my $sth_kind=$dbh->prepare("select * from  relaties where contact_id=? and relatie='Kind'");
my $sth_echt=$dbh->prepare("select * from  relaties where contact_id=? and relatie='Echtgenoot'");
my $sth_cat=$dbh->prepare("select * from  catcontact,categories  where contact_id=? and cat_id=categories.id");

my $ldap = Net::LDAP->new("van-loon.xs4all.nl") or die "$@";
my $mesg = $ldap->bind("cn=root,o=vanloon",password=>"mikel02");
$mesg->code && die $mesg->error;

my $entry;

$sth_contact->execute();
while (my $href = $sth_contact->fetchrow_hashref) {
    my $do_update = 0;
    print_ref("MAIN",$href);

    my $dn;
    if ($href->{'cn'}) {
	$dn="cn=".$href->{'cn'}.",ou=outlook,o=vanloon";
	$mesg =$ldap->search(base=>"ou=outlook,o=vanloon",filter=>"(&(objectclass=*)(cn=".$href->{'cn'}."))",attrs=>["*","modifytimestamp","createtimestamp"]);
    }
    $mesg->code && die $mesg->error;

    if ($mesg->count == 0) {
	$entry = Net::LDAP::Entry->new;
	$entry->dn($dn);
	$do_update=1;
    } else {
	$entry = $mesg->entry(0);
    }
    
    $do_update |= check_attr($entry,$href,'cn','cn');
    $do_update |= check_attr($entry,$href,'uid','uid',);
    $do_update |= check_attr($entry,$href,'password','userpassword',);
    $entry->add(objectclass => "abookperson") unless $entry->exists('objectclass');

    if ($href->{'achternaam'} ) {
	$do_update |= check_attr($entry,$href,'achternaam','sn');
    } else {
	$do_update |= check_attrs($entry,' ','sn');
    }
    $do_update |= check_attr($entry,$href,'company','o');
    $do_update |= check_attr($entry,$href,'voornaam','givenname');
    $do_update |= check_attr($entry,$href,'fileas','displayname');
    $do_update |= check_attr($entry,$href,'webpagina','labeleduri');
    
    $sth_naw->execute($href->{'id'});
    while (my $href1 = $sth_naw->fetchrow_hashref) {
      print_ref("NAW", $href1);

      my $addressstring = $href1->{'straat'}."\$".$href1->{'postcode'}."  ".$href1->{'stad'}."\$".$href1->{'land'};
	$do_update |= check_attrs($entry,$addressstring,'homePostalAddress') if $href1->{'adr_type'} eq 'Prive';
	$do_update |= check_attrs($entry,$addressstring,'postalAddress') if $href1->{'adr_type'} eq 'Werk';
    }

    $sth_echt->execute($href->{'id'});
    while (my $href1 = $sth_echt->fetchrow_hashref) {
      print_ref("ECHT", $href1);

      $do_update |= check_attr($entry,$href1,'naam','spouse');
    }

    $sth_kind->execute($href->{'id'});
    my @kindlist=();
    while (my $href1 = $sth_kind->fetchrow_hashref) {
	print_ref("KIND", $href1);
	push @kindlist,$href1->{'naam'};
    }
    $do_update |= check_attr_list($entry,\@kindlist,'child');

    $sth_tel->execute($href->{'id'},'Prive');
    my @privlist=();
    while (my $href1 = $sth_tel->fetchrow_hashref) {
	print_ref("TELP", $href1);
	push @privlist,$href1->{'number'};
    }
    $do_update |= check_attr_list($entry,\@privlist,'homephone');

    $sth_tel->execute($href->{'id'},'Werk');
    my @worklist=();
    while (my $href1 = $sth_tel->fetchrow_hashref) {
	print_ref("TELW", $href1);
	push @worklist,$href1->{'number'};
    }
    $do_update |= check_attr_list($entry,\@worklist,'telephonenumber');

    $sth_tel->execute($href->{'id'},'Mobiel');
    my @moblist=();
    while (my $href1 = $sth_tel->fetchrow_hashref) {
	print_ref("TELP", $href1);
	push @moblist,$href1->{'number'};
    }
    $do_update |= check_attr_list($entry,\@moblist,'mobile');

    $sth_fax->execute($href->{'id'});
    my @faxlist=();
    while (my $href1 = $sth_fax->fetchrow_hashref) {
      print_ref("FAX", $href1);
      push @faxlist,$href1->{'number'};
    }
    $do_update |= check_attr_list($entry,\@faxlist,'facsimiletelephonenumber');

    $sth_mail->execute($href->{'id'});
    my @maillist=();
    while (my $href1 = $sth_mail->fetchrow_hashref) {
      print_ref("MAIL", $href1);
      push @maillist,$href1->{'mailaddress'};
    }
    $do_update |= check_attr_list($entry,\@maillist,'mail');

    $sth_cat->execute($href->{'id'});
    my @catlist=();
    while (my $href1 = $sth_cat->fetchrow_hashref) {
      print_ref("CAT", $href1);
      push @catlist,$href1->{'categorie'};      
    }
    $do_update |= check_attr_list($entry,\@catlist,'businesscategory');

    if ($do_update) {
      $entry->dump;
	$mesg = $entry->update($ldap, encode=>'base64');
	$mesg->code && die $mesg->error;
    }
}

$dbh->disconnect();
$ldap->unbind();
