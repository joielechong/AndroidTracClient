#! /usr/bin/perl -w

use strict;
use DateTime;
use DateTime::Duration;
use Encode;
use Data::Dumper;

{
    package Jaarplan;
    
    use strict;
    use	base 'Spreadsheet::DataFromExcel';
    use Data::Dumper;
    
    sub clean {
	my $d = shift;
	$d = '' unless defined $d;
	$d =~ s/^\[ ]+//;
	$d =~ s/[ ]+$//;
	$d =~ s/[\000-\037]//g;
	chomp($d);
	return $d;
    }
    
    sub loaddata {
	my $self = shift;
	my $curdag=undef;
	my $curcal=-1;
	my $state=0;
	my $data = $self->load($self->{FILE},$self->{WS}) or die $self->error;
	foreach my $entry (@$data) {
#    print Dumper $entry;
	    
	    for (0..5) {
		$entry->[$_]=clean($entry->[$_]);
	    }
	    
	    next if $entry->[0] eq "za/zo";
	    next if $entry->[0] eq "Za/zo";
	    next if $entry->[0] eq "za";
	    next if $entry->[0] eq "Dag";
	    
	    if ($entry->[1] ne '') {
# nieuwe dag
		$curdag = $entry->[1];
		$state = 0;
	    }
	    if ($entry->[3] ne '' || $entry->[2] ne '') {
		if ($state == 0) {
		    $curcal++;
		    $self->{CAL}->[$curcal]->{datum} = $curdag;
		    $self->{CAL}->[$curcal]->{activiteit} = $entry->[3];
		    $self->{CAL}->[$curcal]->{begintijd} = undef;
		    $self->{CAL}->[$curcal]->{eindtijd} = undef;
		    $self->{CAL}->[$curcal]->{begintijd} = $entry->[2] unless $entry->[2] eq '';
		    $self->{CAL}->[$curcal]->{locatie} = $entry->[5];
		    $self->{CAL}->[$curcal]->{deelnemers} = $entry->[4];
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

{
    package Schoolagenda;
    
    use strict;
    use base 'Net::Google::Calendar';
    use Data::Dumper;
    
    our %ClassData= (
	propname => 'http://van-loon.xs4all.nl/calendar/#xls',
	propval => 'xlstocalendar',
	credentials => "/home/mfvl/download/credentials.poi",
	calendarname => 'Schoolagenda'
	);
    
    sub open_calendar {
	my $self = shift;
	
	my $c;
	for ($self->get_calendars) {
	    $c = $_ if $_->title eq $ClassData{calendarname};
	}
	die 'Kan kalender '.$ClassData{calendarname}.' niet vinden' unless defined $c;
	$self->set_calendar($c);
    }
    
    sub get_credentials {
	my $self = shift;
	
	open CRED,"<".$ClassData{credentials} or die "Kan credential file ".$ClassData{credentials}." niet openen: $@\n";
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
	$self->{USER} = undef;
	$self->{PASS} = undef;
	bless($self, $class); 
	$self->get_credentials();
	$self->login($self->{USER},$self->{PASS});
	$self->open_calendar();
	return $self;
    }
    
    sub cleanup {
	my ($self,$startdate,$enddate) = @_;
	for my $tmp ($self->get_events('max-results'=>'100000000','start-min'=>$startdate,'start-max'=>$enddate)) {
	    print $tmp->title,":";
	    my ($name,$value) = $tmp->extended_property;
	    if (defined($name) && defined($value) && $name eq $ClassData{propname} && $value eq $ClassData{propval}) {
		$self->delete_entry($tmp) || print "Fout: kon ".$tmp->id." niet weggooien: $@. Niet";
	    } else {
		print " niet";
	    }
	    print " verwijderd\n";
	}
    }
    
    sub add_entry {
	my ($self,$event) = @_;
	$event->extended_property($ClassData{propname},$ClassData{propval});
	$event->visibility('public');
	$event->status('confirmed');
	$self->SUPER::add_entry($event);
    }
};

{
	package Afspraak;
	
	use strict;
	use base 'Net::Google::Calendar::Entry';
	use DateTime;
	use DateTime::Duration;
	use Data::Dumper;
	
	sub new {
		my $class = shift;        # Class name is in the first parameter
		my $self  = $class->SUPER::new();
		$self->{JAAR} = undef;
		$self->{MAAND} = undef;
		$self->{DAG} = undef;
		bless($self,$class);
		return $self;
	}

	sub datum {
		my ($self,$jaar,$maand,$dag= @_;
		$self->{JAAR} = $jaar;
		$self->{MAAND} = $maand;
		$self->{DAG} = $dag;
	}
};

my $file = shift;
my $jaar = shift;
my $maand = shift;

die "Aanroep verkeerd\n" unless defined($file) and defined($jaar) and defined($maand);

my $ws = sprintf("%4.4d-%2.2d",$jaar,$maand);
my $sp = Jaarplan->new($file,$ws);
my $gcal = Schoolagenda->new();

print "Oude entries verwijderen\n";
my $startdate = DateTime->new(year=>$jaar,month=>$maand,day=>1,time_zone=>'Europe/Amsterdam');
my $enddate = $startdate + DateTime::Duration->new(months=>1,seconds=>-1);
$gcal->cleanup($startdate,$enddate);

foreach my $e (@{$sp->cal}) {
    my $event = Afspraak->new();
		$event->datum($jaar,$maand,$e->{datum});
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
#    print Dumper $event;
    my $tmp = $gcal->add_entry($event);
    die "Couldn't add event: $@\n" unless defined $tmp;
    print $event->title.": toegevoegd\n";
}
