#! /usr/bin/perl -w

{
	package FondsenDB;
	use DBI;
	use Exporter;
	use Data::Dumper;
	use POSIX qw /mktime strftime/;
	
	@ISA = qw(Exporter);
	use strict;


	BEGIN {
		$FondsenDB::VERSION = "0.1";
	}
	
	my $outputfile = "/web/www/dailystocks.txt";
 
	sub new {
		my $this = shift;
        my $class = ref($this) || $this;
        my $self = {};
        bless $self, $class;
        $self->initialize();
        return $self;
    }

	sub initialize {
		my $self = shift;
		
		$self->{debug} = 0;
		$self->{dryrun} = 0;
		$self->{dbh} = DBI->connect("dbi:Pg:dbname=koersdata",{RaiseError=>1});
		$self->{sth1}=$self->{dbh}->prepare("SELECT * FROM nieuwkoersinfo(?,?,?,?,?,?,?)");
		$self->{sth2}=$self->{dbh}->prepare("copy (select naam,slot,substr(current_time(0),1,5) as time,datum,prev,open,hoog,laag,volume from koersen_vandaag) to '$outputfile' with csv force quote naam;");
	}
	
	sub storeKoers {
		my $self = shift;
		
		$_[6] = 0 unless defined($_[6]);
		print join(",",@_),"\n";
		return if $self->{dryrun};
		my ($name,$time,$last,$open,$high,$low,$vol,$prev) = @_;
#		print Dumper(\@_);
		$vol = 0 unless defined($vol);
		my $date=strftime("%F",localtime($time));
		$self->{sth1}->execute(uc($name),$date,$vol,$open,$high,$low,$last);
	}
	
	sub exportKoersen {
		my $self = shift;
		
		$self->{sth2}->execute;
	}
};

1
