#! /usr/bin/perl -w

use strict;
use Spreadsheet::DataFromExcel;
use Net::Google::Calendar;
use DateTime;
use DateTime::Duration;
use Data::Dumper;

my $file = shift;
my $jaar = shift;
my $maand = shift;
my ($user,$pass);

die "Aanroep verkeerd\n" unless defined($file) and defined($jaar) and defined($maand);

my $startdate = DateTime->new(year=>$jaar,month=>$maand,day=>1,time_zone=>'Europe/Amsterdam');
my $enddate = $startdate + DateTime::Duration->new(months=>1,seconds=>-1);
print "$startdate\n$enddate\n"; 

print $file,"\n";

OPEN CRED "</home/mfvl/download/credentials.poi" or die "Kan credential file niet openen: $@\n";
while (<CRED>) {
	my ($key,$val) = split("=");
    if ($key eq "username") {
      $user = $val;
    }
    if ($key eq "password") {
      $pass = $val;
    }    
}
close CRED;


my $ws = sprintf("%4.4d-%2.2d",$jaar,$maand);

my $sp = Spreadsheet::DataFromExcel->new();
my $data = $sp->load($file,$ws) or die $sp->error;
my @cal;
my %ce;
my $curdag=undef;
my $curcal=-1;
my $state=0;

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
    $entry->[5]='' unless defined $entry->[5];
    if ($entry->[3] ne '' || $entry->[2] ne '') {
	if ($state == 0) {
	    $curcal++;
	    $cal[$curcal]->{datum} = $curdag;
	    $cal[$curcal]->{activiteit} = $entry->[3];
	    chomp($cal[$curcal]);
	    $cal[$curcal]->{begintijd} = undef;
	    $cal[$curcal]->{eindtijd} = undef;
	    $cal[$curcal]->{begintijd} = $entry->[2] unless $entry->[2] eq '';
	    $cal[$curcal]->{locatie} = $entry->[5];
	    $state = 1;
	} elsif ($state == 1) {
	    $cal[$curcal]->{eindtijd} = $entry->[2] unless $entry->[2] eq '';
	    $cal[$curcal]->{activiteit} .= ' '.$entry->[3];
	    chomp($cal[$curcal]->{activiteit});
	    $cal[$curcal]->{locatie} .= ' '.$entry->[5];
	    chomp($cal[$curcal]->{locatie});
	    $state=0;
	}
    } else {
	$state = 0;
    }
}
#print Dumper \@cal;


my $gcal = Net::Google::Calendar->new();
$gcal->login($user,$pass);

my $c;
for ($gcal->get_calendars) {
    $c = $_ if $_->title eq 'Schoolagenda';
}

die 'Kan kalender niet vinden' unless defined $c;

$gcal->set_calendar($c);

print "Oude entries verwijderen\n";
for my $tmp ($gcal->get_events('max-results'=>'100000000','start-min'=>$startdate,'start-max'=>$enddate)) {
    print $tmp->title,"\n";
    $gcal->delete_entry($tmp) || print "Kon ".$tmp->id." niet weggooien: $@\n";
}
print "=========================\nNu nieuwe toevoegen\n";

foreach my $e (@cal) {
    my $event = Net::Google::Calendar::Entry->new();
    print Dumper $e;
    $event->title($e->{activiteit});
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
	print $starttime->strftime("%F %T"),$uur,$minuut,"\n";
	print $endtime->strftime("%F %T"),$uur,$minuut,"\n";
	$event->when($starttime,$endtime);
    } else {
	my $starttime = DateTime->new(year=>$jaar,month=>$maand,day=>$e->{datum});
	$event->when($starttime,$starttime,1);
    }
    $event->location($e->{locatie}) if defined($e->{locatie});
		$event->extended_property('http://van-loon.xs4all.nl/calendar/'=>'xlstocalendar');
    $event->visibility('public');
    $event->status('confirmed');
    $gcal->add_entry($event);
}
