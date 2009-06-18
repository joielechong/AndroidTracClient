#! /usr/bin/perl -w

use strict;
use Text::vCard::Addressbook;
use Data::Dumper;
use DBI;

my $dbh=DBI->connect("DBI:Pg:dbname=mfvl");
my $sth1 = $dbh->prepare("SELECT contact_id FROM mail where mailaddress = ?");
my $sth2 = $dbh->prepare("SELECT contact_id FROM telephone where number = normtel(?) and list");
my $sth3 = $dbh->prepare("SELECT cn FROM contacts where id = ?");
#my $sth4 = $dbh->prepare("SELECT mailaddress FROM mail WHERE contact_id=?");
#my $sth5 = $dbh->prepare("SELECT number FROM telephone WHERE contact_id=?");
#my $sth6 = $dbh->prepare("INSERT INTO mail (contact_id,mailaddress) VALUES(?,?)");
#my $sth7 = $dbh->prepare("INSERT INTO telephone (contact_id,number) VALUES(?,?)");

#my @types = qw(ADR BDAY EMAIL FN N ORG TEL TITLE URL);

while (my $file = shift) {
    my $address_book = Text::vCard::Addressbook->new({'source_file' => $file,});
    
    foreach my $vcard ($address_book->vcards()) {
	my $contact_id;
	my @phones;
	my @emails;
	my @faxes;
	
	my $fullname = $vcard->fullname();
	
	print "Got card for $fullname";
	my $nodes = $vcard->get('email');
	if (defined($nodes)) {
	    foreach my $md (@$nodes) {
		if (defined($md->value)) {
#		    print join(',',$md->types()),' ',$md->value,"\n";
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
		    my $number = $md->value;
		    my $type = join(',',$md->types());
		    if (substr($number,0,2) eq '31') {
			$number = '+'.$number;
		    }
		    if (substr($number,0,4) eq '+310') {
			substr($number,0,4) = '+31';
		    }
		    $type='Other' if $type eq '';
		    print "$type $number\n";
		    if ($type eq 'fax') {
			push @faxes,$number;
		    } else {
			push @phones,$number;
			unless (defined($contact_id)) {
			    $sth2->execute($number);
			    if (my @row=$sth2->fetchrow_array()) {
				$contact_id=$row[0];
			    }
			}
		    }
		}
	    }
	}
	
	my $process = 0;
	if (defined($contact_id)) {
	    print " $contact_id ";
	    $sth3->execute($contact_id);
	    if (my @row=$sth3->fetchrow_array()) {
		print ' ',$row[0];
		if (lc($row[0]) ne lc($vcard->fullname())) {
		    print " !!DIFF!! Verwerken? (J/N): ";
		    my $answer = lc(getc());
		    $process = 1 if $answer eq 'j'; 
		    die "direct gestopt\n" if $answer eq "q";
		} else {
		    $process = 1;
		}
	    } // contact_id bestaat altijd al dus geen else
	} else {
		print ' ??? Toevoegen? (J/N)';
		my $answer = lc(getc());
		$process = 1 if $answer eq 'j'; 
		die "direct gestopt\n" if $answer eq "q";
		my $name=$vcard->N;
		print Dumper($name);
		exit();
	}
	if ($process) {
	my $title = $vcard->title();
	my $bday = $vcard->bday();
	my $url = $vcard->url;
		print " Nu verwerken";
		my $count = $#emails + 1;
		if ($count > 0) {
		    my $mas = "'".join("','",@emails)."'";
		    my $sqlcmd= "INSERT INTO mail (contact_id,mailaddress) SELECT $contact_id,mail.ids[gs.ser] as mailaddress FROM (SELECT ARRAY[$mas]) as mail(ids),generate_series(1,$count) as gs(ser) EXCEPT SELECT contact_id,mailaddress FROM mail where contact_id=$contact_id";
		    print "\nQuery = $sqlcmd\n";
		    $dbh->do($sqlcmd);
		}
		$count = $#phones + 1;
		if ($count > 0) {
		    my $tels = "'".join("','",@phones)."'";
		    my $sqlcmd= "INSERT INTO telephone (contact_id,number) SELECT $contact_id,nums.ids[gs.ser] as number FROM (SELECT ARRAY[$tels]) as nums(ids),generate_series(1,$count) as gs(ser) EXCEPT SELECT contact_id,number FROM telephone where contact_id=$contact_id";
		    print "\nQuery = $sqlcmd\n";
		    $dbh->do($sqlcmd);
		}
		$count = $#faxes + 1;
		if ($count > 0) {
		    my $faxs = "'".join("','",@faxes)."'";
		    my $sqlcmd= "INSERT INTO fax (contact_id,number) SELECT $contact_id,nums.ids[gs.ser] as number FROM (SELECT ARRAY[$faxs]) as nums(ids),generate_series(1,$count) as gs(ser) EXCEPT SELECT contact_id,number FROM fax where contact_id=$contact_id";
		    print "\nQuery = $sqlcmd\n";
		    $dbh->do($sqlcmd);
		}
	}
	print "\n";
    }
}
