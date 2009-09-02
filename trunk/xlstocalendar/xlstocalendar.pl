#! /usr/bin/perl -w

use strict;
use Net::Google::Calendar;
use DateTime;
use DateTime::Duration;
use Encode;
use Data::Dumper;

{
    package Jaarplan;
    
    use strict;
    use	base 'Spreadsheet::DataFromExcel';
    use Data::Dumper;
    
    sub loaddata {
	my $self = shift;
	my $curdag=undef;
	my $curcal=-1;
	my $state=0;
	my $data = $self->load($self->{FILE},$self->{WS}) or die $self->error;
	foreach my $entry (@$data) {
#    print Dumper $entry;
	    
	    next if $entry->[0] eq "za/zo";
	    next if $entry->[0] eq "Za/zo";
	    next if $entry->[0] eq "za";
	    next if $entry->[0] eq "Dag";
	    
	    if ($entry->[1] ne '') {
# nieuwe dag
		$curdag = $entry->[1];
		$state = 0;
	    }
	    $entry->[2]='' unless defined $entry->[2];
	    $entry->[3]='' unless defined $entry->[3];
	    $entry->[4]='' unless defined $entry->[4];
	    $entry->[5]='' unless defined $entry->[5];
	    $entry->[3]=~ s/^\[ ]+//;
	    $entry->[3]=~ s/[ ]+$//;
	    $entry->[4]=~ s/^[ ]+//;
	    $entry->[4]=~ s/[ ]$//;
	    $entry->[5]=~ s/^[ ]+//;
	    $entry->[5]=~ s/[ ]+$//;
	    $entry->[3]=~ s/[\000-\037]//g;
	    $entry->[4]=~ s/[\000-\037]//g;
	    $entry->[5]=~ s/[\000-\037]//g;
	    if ($entry->[3] ne '' || $entry->[2] ne '') {
		if ($state == 0) {
		    $curcal++;
		    $self->{CAL}->[$curcal]->{datum} = $curdag;
		    $self->{CAL}->[$curcal]->{activiteit} = $entry->[3];
		    chomp($self->{CAL}->[$curcal]->{activiteit});
		    $self->{CAL}->[$curcal]->{begintijd} = undef;
		    $self->{CAL}->[$curcal]->{eindtijd} = undef;
		    $self->{CAL}->[$curcal]->{begintijd} = $entry->[2] unless $entry->[2] eq '';
		    $self->{CAL}->[$curcal]->{locatie} = $entry->[5];
		    chomp($self->{CAL}->[$curcal]->{locatie});
		    $self->{CAL}->[$curcal]->{deelnemers} = $entry->[4];
		    chomp($self->{CAL}->[$curcal]->{deelnemers});
		    $state = 1;
		} elsif ($state == 1) {
		    $self->{CAL}->[$curcal]->{eindtijd} = $entry->[2] unless $entry->[2] eq '';
		    $self->{CAL}->[$curcal]->{activiteit} .= ' '.$entry->[3];
		    chomp($self->{CAL}->[$curcal]->{activiteit});
		    $self->{CAL}->[$curcal]->{locatie} .= ' '.$entry->[5];
		    chomp($self->{CAL}->[$curcal]->{locatie});
		    $self->{CAL}->[$curcal]->{deelnemers} .= ' '.$entry->[4];
		    chomp($self->{CAL}->[$curcal]->{deelnemers});
		    $state=0;
		}
	    } else {
		$state = 0;
	    }
	}
    }
    
    sub new {
	my($class, $file,$ws) = @_;        # Class name is in the first parameter
	my $self  = $class->SUPER::new();
	$self->{FILE} = $file;
	$self->{WS} = $ws;
	$self->{CAL} = ();
	bless($self, $class);          # Say: $self is a $class
	$self->loaddata();
	return $self;
    }
    
    sub cal {
	my $self = shift;
	
	return $self->{CAL};
    }
};

my $propname = 'http://van-loon.xs4all.nl/calendar/#xls';
my $propval = 'xlstocalendar';

{
    package Schoolagenda;
    
    use strict;
    use base 'Net::Google::Calendar';
    use Data::Dumper;

    
    sub open_calendar {
	my $self = shift;
	
	my $c;
	for ($self->get_calendars) {
	    $c = $_ if $_->title eq $self->{AGENDA};
	}
	die 'Kan kalender '.$self->{AGENDA}.' niet vinden' unless defined $c;
	$self->set_calendar($c);
    }
    
    sub get_credentials {
	my $self = shift;
	my $credentials = "/home/mfvl/download/credentials.poi";
	
	open CRED,"<$credentials" or die "Kan credential file niet openen: $@\n";
	while (<CRED>) {
	    my ($key,$val) = split("=");
	    if ($key eq "username") {
		$self->{USER} = $val;
	    }
	    if ($key eq "password") {
		$self->{PASS} = $val;
	    }
	}
	close CRED;
    }
    
    sub new {
	my $class = shift;        # Class name is in the first parameter
	my $self  = $class->SUPER::new();
	$self->{AGENDA} = shift;
	$self->{USER} = undef;
	$self->{PASS} = undef;
	bless($self, $class);          # Say: $self is a $class
	$self->get_credentials();
	$self->login($self->{USER},$self->{PASS});
	$self->open_calendar();
	return $self;
    }
		
		sub cleanup {
			my ($self,$startdate,$enddate,$propname,$propval) = @_;
for my $tmp ($self->get_events('max-results'=>'100000000','start-min'=>$startdate,'start-max'=>$enddate)) {
    print $tmp->title,":";
    my ($name,$value) = $tmp->extended_property;
    if (defined($name) && defined($value) && $name eq $propname && $value eq $propval) {
	$self->delete_entry($tmp) || print "Fout: kon ".$tmp->id." niet weggooien: $@. Niet";
    } else {
	print " niet";
    }
    print " verwijderd\n";
}
			
		}
};

my $file = shift;
my $jaar = shift;
my $maand = shift;

die "Aanroep verkeerd\n" unless defined($file) and defined($jaar) and defined($maand);

my $ws = sprintf("%4.4d-%2.2d",$jaar,$maand);
my $sp = Jaarplan->new($file,$ws);
my $gcal = Schoolagenda->new('Schoolagenda');

print "Oude entries verwijderen\n";
my $startdate = DateTime->new(year=>$jaar,month=>$maand,day=>1,time_zone=>'Europe/Amsterdam');
my $enddate = $startdate + DateTime::Duration->new(months=>1,seconds=>-1);
$gcal->cleanup($startdate,$enddate,$propname,$propval);

foreach my $e (@{$sp->cal}) {
    my $event = Net::Google::Calendar::Entry->new();
#    print Dumper $e;
    $event->title(encode('UTF-8',"2e: ".$e->{activiteit}));
    my $uur = $e->{begintijd};
    my $minuut = undef;
    if (defined($uur)) {
	($uur,$minuut) = split('\.',$uur) if index($uur,'.') >= 0;
	$minuut = 0 unless defined $minuut;
	$minuut = 0 if $minuut eq '';
	$minuut=30 if $minuut eq '3';
	my $starttime = DateTime->new(year=>$jaar,month=>$maand,day=>$e->{datum},hour=>$uur,minute=>$minuut,second=>0,time_zone=>'Europe/Amsterdam');
	my $endtime = $starttime + DateTime::Duration->new(hours=>1);
	if (defined($e->{eindtijd})) {
	    $uur = $e->{eindtijd};
	    $minuut = undef;
	    ($uur,$minuut) = split('\.',$uur) if index($uur,'.') >= 0;
	    $minuut = 0 unless defined $minuut;
	    $minuut = 0 if $minuut eq '';
	    $minuut = 30 if $minuut eq '3';
	    $endtime =  DateTime->new(year=>$jaar,month=>$maand,day=>$e->{datum},hour=>$uur,minute=>$minuut,second=>0,time_zone=>'Europe/Amsterdam');
	}
#	print $starttime->strftime("%F %T"),$uur,$minuut,"\n";
#	print $endtime->strftime("%F %T"),$uur,$minuut,"\n";
	$event->when($starttime,$endtime);
    } else {
	my $starttime = DateTime->new(year=>$jaar,month=>$maand,day=>$e->{datum});
	$event->when($starttime,$starttime,1);
    }
    $event->content(encode('UTF-8',$e->{deelnemers})) if defined $e->{deelnemers};
    $event->location(encode('UTF-8',$e->{locatie})) if defined($e->{locatie});
    $event->extended_property($propname,$propval);
    $event->visibility('public');
    $event->status('confirmed');
#    print Dumper $event;
    my $tmp = $gcal->add_entry($event);
    die "Couldn't add event: $@\n" unless defined $tmp;
    print $event->title.": toegevoegd\n";
}
