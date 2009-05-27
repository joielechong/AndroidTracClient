#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;

my @Fixed =('Name','E-mail','Notes');
my @Repeat=('Description','Email','IM','Phone','Mobile','Pager','Fax','Company','Title','Other','Address');

{
    package Entry;

    use Data::Dumper;

    my $nsec=0;
    my @sections;
    my %vertaal = ('Werk'=>'Work',
		   'Prive'=>'Personal',
		   'Overig'=>'Other',
		   'Anders'=>'Other');


    sub get_section {
	my $self = shift;
	my $typold = shift;
	my $type;
	my $i;

	return undef unless defined($typold);

	if ($typold eq 'Mobiel') {
	    if ($self->{nsec} == 0) {
		$type = 'Business';
	    } elsif (defined($self->{'E-mail'})) {
		for ($i=0;$i<$self->{nsec};$i++) {
		    return $i if (defined($self->{sections}->[$i]->{Email}) && ($self->{'E-mail'} eq $self->{sections}->[$i]->{Email}));
		}
		$type = 'Business';
	    } else {
		return 0;
	    }
	} else {
	    $type =$vertaal{$typold};
	}
        for ($i=0;$i<$self->{nsec};$i++) {
	    return $i if $type eq $self->{sections}->[$i]->{Description};
	}
	$self->{sections}->[$self->{nsec}]->{Description}=$type;
	$nsec = $self->{nsec}+1 if $self->{nsec} >= $nsec;
	return $self->{nsec}++;
    }

    sub new {
	my $class = shift;
	my $self = {};
	my $company;
	my $function;
	bless $self;

	$self->{'Name'}  = shift;
	$self->{'Notes'} = shift;
	$self->{'E-mail'} = undef;
	$self->{nsec} = 0;
	$self->{sections} = [];

	$company = shift;
	if (defined($company)) {
	    my $sec = $self->get_section('Werk');
	    $self->{sections}->[$sec]->{Company} = $company;
	}

	$function = shift;
	if (defined($function)) {
	    my $sec = $self->get_section('Werk');
	    $self->{sections}->[$sec]->{Title} = $function;
	}

	return $self;
    }

    sub getField {
	my $self = shift;
	my $field = shift;
	my $sec = shift;
	my $retval = undef;
	
	if (defined($sec)) {
	    if ($sec > $self->{nsec}) {
		$retval = undef;
	    } else {
		$retval = $self->{sections}->[$sec-1]->{$field};
	    }
	} else {
	    $retval = $self->{$field};
	}
	if (($field eq 'Email') && defined($retval) && defined($self->{'E-mail'}) && ($retval eq ($self->{'E-mail'}))) {
	    $retval = undef;
	}
	if (defined($retval)) {
	    $retval = '"'.$retval.'"';
	} else {
	    $retval = '';
	}
	return $retval;
    }

    sub setStandardMail {
	my $self = shift;
	my $email = shift;
	$self->{'E-mail'} = $email;
	my $type = shift;
	my $sec = $self->get_section($type);
	if (defined($self->{sections}->[$sec]->{'Email'})) {
	    $self->{sections}->[$sec]->{'Email'} .= "; $email";
	} else {
	    $self->{sections}->[$sec]->{'Email'} = $email;
	}
    }

    sub setMoreMail {
	my $self = shift;
	my $email = shift;
	my $type = shift;

	my $sec = $self->get_section($type);
	if (defined($sec)) {
	    if (defined($self->{sections}->[$sec]->{'Email'})) {
		$self->{sections}->[$sec]->{'Email'} .= "; $email";
	    } else {
		$self->{sections}->[$sec]->{'Email'} = $email;
	    }
	} else {
	    print "$type niet herkend vooor $email\n";
	}
    }
    
    sub setNAW {
	my $self = shift;
	my $row = shift;
	my $sec = $self->get_section($row->{adr_type});
	my $string;
	$string  =$row->{straat} if defined($row->{straat});
	$string .=(", ".$row->{postcode}) if defined($row->{postcode});
	$string .=(" ".$row->{stad}) if defined($row->{stad});
	$string .=(", ".$row->{land}) if defined($row->{land});
	$self->{sections}->[$sec]->{'Address'}=$string;
    }

    sub setFax {
	my $self = shift;
	my $row = shift;
	my $sec = $self->get_section($row->{fax_type});
	if (defined($self->{sections}->[$sec]->{'Fax'})) {
	    $self->{sections}->[$sec]->{'Fax'} .= (";".$row->{number});
	} else {
	    $self->{sections}->[$sec]->{'Fax'}=$row->{number};
	}
    }

    sub setPhone {
	my $self = shift;
	my $row = shift;
	my $sec = $self->get_section($row->{tel_type});
	my $veld = 'Phone';
	$veld = 'Mobile' if $row->{tel_type} eq 'Mobiel';

	if (defined($self->{sections}->[$sec]->{$veld})) {
	    $self->{sections}->[$sec]->{$veld} .= (";".$row->{number});
	} else {
	    $self->{sections}->[$sec]->{$veld}=$row->{number};
	}
    }

    sub shouldSave {
	my $self = shift;

	return defined($self->{'E-mail'}) || ($self->{nsec} > 0);
    }

    sub getNsec {
	return $nsec;
    }

}


my $dbh = DBI->connect("dbi:Pg:dbname=mfvl host=van-loon.xs4all.nl");
my $sthMail = $dbh->prepare("SELECT mailaddress,type,priority from mail where contact_id=? order by priority");
my $sthPhone = $dbh->prepare("SELECT * from telephone where contact_id=?");
my $sthFax = $dbh->prepare("SELECT * from fax where contact_id=?");
my $sthNAW = $dbh->prepare("SELECT * from naw where contact_id=?");

my $contacten = $dbh->selectall_hashref("SELECT * FROM contacts WHERE inipaq","id");
my %contacts;

foreach my $id (keys %$contacten) {
    my $record = $contacten->{$id};
#    print "$id",Dumper($record);
    my $rec = Entry->new($record->{cn},'id='.$record->{id},$record->{company},$record->{function});

    my $rc = $sthMail->execute($record->{id});
    my @rows;
    if ($sthMail->rows > 0) {
	@rows = $sthMail->fetchrow_array;
	$rec->setStandardMail($rows[0],$rows[1]);
	while (@rows = $sthMail->fetchrow_array) {
	    $rec->setMoreMail($rows[0],$rows[1]);
	}
    }

    $rc = $sthNAW->execute($record->{id});
    while (my $row = $sthNAW->fetchrow_hashref) {
	$rec->setNAW($row);
    }

    $rc = $sthPhone->execute($record->{id});
    while (my $row = $sthPhone->fetchrow_hashref) {
	$rec->setPhone($row);
    }

#    $rc = $sthFax->execute($record->{id});
#    while (my $row = $sthFax->fetchrow_hashref) {
#	$rec->setFax($row);
#    }

#    print Dumper($rec);
    $contacts{$id} = $rec if $rec->shouldSave;
}

#print Dumper(\%contacts);

my $string = join(",",@Fixed);

my $maxsec=Entry::getNsec();
for (my $i=1;$i<=$maxsec;$i++){
    foreach (@Repeat) {
	$string .= sprintf(",Section %d - %s",$i,$_);
    }
}

print $string,"\n";

foreach my $i (keys %contacts) {
    my $rec = $contacts{$i};
    my @fields=();

    foreach (@Fixed) {
	push @fields,$rec->getField($_);
    }

    for (my $j=1;$j<=$maxsec;$j++) {
	foreach(@Repeat) {
	    push @fields,$rec->getField($_,$j);
	}
    }
    print join(",",@fields),"\n";
}
