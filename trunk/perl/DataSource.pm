{
	package DataSource;
	require Exporter;
	@ISA = qw(Exporter);

	use Data::Dumper;
	use POSIX qw {mktime strftime};
	use LWP::UserAgent;
	use LWP::ConnCache;
	use HTTP::Cookies;
	use File::Temp;
	use HTML::TagParser;

	BEGIN {
		$DataSource::VERSION = "0.1";
		our $cookie_jar=HTTP::Cookies->new(FILE=>"$ENV{HOME}/.cookies_aex",AutoSave =>1);
		our $fh = undef;
	}

	sub laad_EuroNext {
		return DataSource::EuroNext->new();
	}

	sub laad_Yahoo {
		return DataSource::Yahoo->new();
	}

	sub laad_YahooUK {
		return DataSource::YahooUK->new();
	}

	sub laad_OHRA {
		return DataSource::OHRA->new();
	}

	sub laad_ASR {
		return DataSource::ASR->new();
	}

	sub new {
		my $this = shift;
		my $method = shift;
	
		no strict 'refs';
		my $proc = "laad_".$method;
		if (defined(&$proc)) {
			$ds = &$proc;
			return $ds;
		}

		use strict 'refs';
        my $class = ref($this) || $this;
        my $self = {};
        bless $self, $class;
		
        $self->initialize();
        return $self;
    }

	sub initialize {
		my $self = shift;
		$self->{cache} = LWP::ConnCache->new;
		$self->{ua} = LWP::UserAgent->new;
		$self->{ua}->agent($self->userAgent);
		$self->{ua}->conn_cache($cache);
		$self->{skipList} = undef;
	}
	
	sub opencsvlog {
		my $logfile = shift;
		
		return open $fh,">$logfile";
	}
	
	sub outputKoers {
		my $self = shift;
		
		$_[6] = 0 unless defined($_[6]);
		my ($name,$time,$last,$open,$high,$low,$vol,$prev) = @_;
#		print Dumper(\@_);
		$vol = 0 unless defined($vol);
		my $date=strftime("%F,%R",localtime($time));
		print $fh uc($name).",$last,$date,$prev,$open,$high,$low,$vol\n";
	}
	
	sub closecsvlog {
		close $fh;
		$fh = undef;
	}	
	
	sub userAgent {
		return "Mozilla/4.04 [en] (MFvL;I)";
	}

	sub fetchURL {
		my $self = shift;
		my $url = shift;
		my $req = HTTP::Request->new(GET=>$url);
		$cookie_jar->add_cookie_header($req);
		$self->{ua}->cookie_jar($cookie_jar);
		while ($#_ >= 0) {
			my $key = shift;
			my $value=shift;
			$req->header($key=>$value);
		}
		my $res = $self->{ua}->request($req);
		$cookie_jar->extract_cookies($res);
		$self->{'content'} = $res->content;
	}
	
	sub inSkipList {
		my $self=shift;
		my $symbol = shift;

		return defined($self->{skipList}->{$symbol});
	}

	sub process {
	}
}

{
	package DataSource::CSV;
	require Exporter;
	@ISA = qw(DataSource);

	use Data::Dumper;
	use Text::CSV_XS;
	
	BEGIN {
		$DataSource::CSV::VERSION = "0.1";
	}

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
		$self->SUPER::initialize();
		$self->{'sepchar'} = ",";
	}	
}

{
	package DataSource::EuroNext;
	require Exporter;
	@ISA = qw(DataSource::CSV);

	use strict;
	use POSIX qw {mktime strftime};
	use File::Temp;
	use Text::CSV_XS;
	use Data::Dumper;

	BEGIN {
		$DataSource::EuroNext::VERSION = "0.1";
	}

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
		$self->SUPER::initialize();
		$self->{'sepchar'} = "\t";
		$self->{skipList} ={'ROLPR'=>1,
							'UNIA'=>1,
							'UNCC7'=>1};
	}
	
	sub process {
		my $self = shift;
		my $fdbh = shift;
		
#	    print substr($self->{'content'},0,100),"\n";
		return if ($self->{'content'} =~ /^<html/);
		my $csv = Text::CSV_XS->new({sep_char => $self->{'sepchar'}});
		
		my $fh=new File::Temp();
		print $fh $self->{'content'};
		seek($fh,0,SEEK_SET);
		my $inheader = 1;
		while (!eof($fh)) {
			if ($inheader) {
				my $dummy = <$fh>;
				chomp($dummy);
				$dummy =~ s/\r//;
				$inheader = 0 if length($dummy) == 0;
			} else {
				$csv->column_names($csv->getline($fh));
				while (my $hr = $csv->getline_hr($fh)) {
#					print Dumper($hr);
					next if $self->inSkipList($hr->{Symbol});
					my $ds = $hr->{'Date - time (CET)'};
					next if $ds eq '-';
#					print $ds,"\n";
					my ($date,$time) = split(" ",$ds);
					my ($hour,$minut) = split(":",$time);
					my ($day,$month,$year) = split("/",$date);
					my $time_t = POSIX::mktime(0,$minut,$hour,$day,$month-1,$year+100);
					$fdbh->storeKoers($hr->{"Instrument's name"},$time_t,$hr->{Last},$hr->{"Day First"},$hr->{"Day High"},$hr->{"Day Low"},$hr->{"Volume"},'N/A');
					$self->outputKoers($hr->{"Instrument's name"},$time_t,$hr->{Last},$hr->{"Day First"},$hr->{"Day High"},$hr->{"Day Low"},$hr->{"Volume"},'N/A');
				}
			}
		}
		close $fh;
	}
}

{
	package DataSource::Yahoo;
	require Exporter;
	@ISA = qw(DataSource::CSV);
	use strict;
	use POSIX qw {mktime strftime};
	use File::Temp;
	use Text::CSV_XS;
	use Data::Dumper;


	BEGIN {
		$DataSource::Yahoo::VERSION = "0.1";
	}

sub max{
    my $a=shift;
    my $b=shift;

    return ($a>$b?$a:$b);
}

sub min{
    my $a=shift;
    my $b=shift;

    return ($a<$b?$a:$b);
}

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
		$self->SUPER::initialize();
	}
	
	sub parse_time {
		my $self=shift;
		my $datum = shift;
		my $timestr = shift;
		my ($month,$day,$year) = split("/",$datum);
		my $time = substr($timestr,0,length($timestr)-2);
		$time =~ s/ //g;
		my $timeoff = substr($timestr,length($timestr)-2,2);
		my ($hour,$minut) = split(":",$time);
		my $time_t = POSIX::mktime(0,$minut,$hour,$day,$month-1,$year-1900);
		$time_t += 3600*12 if $timeoff eq 'pm' && $hour ne '12';
		$time_t += 3600*6; # offset for USA
		return $time_t;
	}
	
	sub process {
		my $self = shift;
		my $fdbh = shift;
		my @fields = ("NAME","LAST","TIME","DATE","PREV","OPEN","HIGH","LOW","VOL");
		my $csv = Text::CSV_XS->new({sep_char => $self->{'sepchar'}});
    
		my $fh=new File::Temp();
		print $fh $self->{'content'};
		seek($fh,0,SEEK_SET);
		$csv->column_names(@fields);
		while (my $hr = $csv->getline_hr($fh)) {
#			print Dumper($hr);
#			print $hr->{DATE}," ",$hr->{TIME},"\n";
			my $time_t = $self->parse_time($hr->{DATE},$hr->{TIME});
			$hr->{VOL} = 0 if $hr->{VOL} eq "N/A";
			$hr->{OPEN} = $hr->{LAST} if $hr->{OPEN} eq "N/A";
			$hr->{HIGH} = max($hr->{OPEN},$hr->{LAST}) if $hr->{HIGH} eq "N/A";
			$hr->{LOW} = min($hr->{OPEN},$hr->{LAST}) if $hr->{LOW} eq "N/A";
			$hr->{PREV} = $hr->{LAST} if $hr->{PREV} eq "N/A";

			$fdbh->storeKoers($hr->{NAME},$time_t,$hr->{LAST},$hr->{OPEN},$hr->{HIGH},$hr->{LOW},$hr->{VOL},$hr->{PREV});
			$self->outputKoers($hr->{NAME},$time_t,$hr->{LAST},$hr->{OPEN},$hr->{HIGH},$hr->{LOW},$hr->{VOL},$hr->{PREV});
		}
		close $fh;
	}
}

{
	package DataSource::YahooUK;
	require Exporter;
	@ISA = qw(DataSource::Yahoo);
	
	BEGIN {
		$DataSource::YahooUK::VERSION = "0.1";
	}

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
		$self->SUPER::initialize();
	}

	sub userAgent {
		return "Mozilla/4.05 [en] (MFvL;I)";
	}
	sub parse_time {
		my $self=shift;
		my $datum = shift;
		my $timestr = shift;
		my ($month,$day,$year) = split("/",$datum);
		my $time = substr($timestr,0,length($timestr)-2);
		$time =~ s/ //g;
		my $timeoff = substr($timestr,length($timestr)-2,2);
		my ($hour,$minut) = split(":",$time);
		my $time_t = POSIX::mktime(0,$minut,$hour,$day,$month-1,$year-1900);
		$time_t += 3600*12 if $timestr eq "PM" && $hour ne '12';
		$time_t += 3600; # offset for Western Europe 
		return $time_t;
	}
	
}

{
	package DataSource::ASR;
	require Exporter;
	@ISA = qw(DataSource);
	use strict;
	use POSIX qw {mktime strftime};
	use HTML::TagParser;
	use Data::Dumper;
	
	BEGIN {
		$DataSource::ASR::VERSION = "0.1";
	}

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
		$self->SUPER::initialize();
	}

	sub process {
		my $self = shift;
		my $fdbh = shift;
	
		my $html = HTML::TagParser->new( $self->{'content'} );
	
#	print Dumper $html;
	
		my @classes=('FieldName date','ListTableFieldValue','ListTableFieldValueRight');;
	
		my @list = $html->getElementsByClassName('FieldName date');
		my $datum1 = $list[0]->innerText;
		my $datum2 = $list[1]->innerText;
		my @fondslist = $html->getElementsByClassName('ListTableFieldValue');
		my @koerslist = $html->getElementsByClassName('ListTableFieldValueRight');
	
		my $nfonds=$#fondslist;
		my $nkoers=$#koerslist;
	
		for (my $i=0;$i<=$nfonds;$i++) {
			my $f = $fondslist[$i]->innerText;
			my $k1 = $koerslist[$i*2]->innerText;
			my $k2 = $koerslist[$i*2+1]->innerText;
			$k1 =~ s/^....//;
			$k2 =~ s/^....//;
			$k1 =~ s/\.//;
			$k2 =~ s/\.//;
			$k1 =~ s/,/./;
			$k2 =~ s/,/./;
			my ($day,$month,$year) = split("-",$datum1);
			my $time_t = POSIX::mktime(0,0,0,$day,$month-1,$year-1900);
			$fdbh->storeKoers($f,$time_t,$k1,$k1,$k1,$k1,0,'N/A') unless $k1 eq "-";
			($day,$month,$year) = split("-",$datum2);
			$time_t = POSIX::mktime(0,0,0,$day,$month-1,$year-1900);
			$fdbh->storeKoers($f,$time_t,$k2,$k2,$k2,$k2,0,'N/A') unless $k2 eq "-";
			$self->outputKoers($f,$time_t,$k2,$k2,$k2,$k2,0,'N/A') unless $k2 eq "-";
		}
	}
}

{
	package DataSource::OHRA;
	require Exporter;
	@ISA = qw(DataSource);

	use strict;
	use POSIX qw {mktime strftime};
	use HTML::TagParser;
	use Data::Dumper;
	
	BEGIN {
		$DataSource::OHRA::VERSION = "0.1";
	}

sub max{
    my $a=shift;
    my $b=shift;

    return ($a>$b?$a:$b);
}

sub min{
    my $a=shift;
    my $b=shift;

    return ($a<$b?$a:$b);
}

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
		$self->SUPER::initialize();
	}

	sub process {
		my $self = shift;
		my $fdbh = shift;
	
		my $html = HTML::TagParser->new( $self->{'content'} );
    
		my %fondsen;
    
		my @fondslist = $html->getElementsByTagName('td');
		my $nfonds=$#fondslist;
		my $datum;
		my $tijd;
  
		for (my $i=10;$i<=$nfonds;$i+=6) {
			my $f = $fondslist[$i]->innerText;
			if ($f =~ /Beursmedia/) {
				$datum = substr($f,0,10);
				$tijd = substr($f,16,5);
				foreach my $ff (keys %fondsen) {
					$fondsen{$ff}->{datum}=$datum unless defined($fondsen{$ff}->{datum});
					$fondsen{$ff}->{tijd}=$tijd unless defined($fondsen{$ff}->{tijd});
				}
				$i+=2;
			} else {
				my $o=$fondslist[$i+1]->innerText;
				my $s=$fondslist[$i+2]->innerText;
				my $p=$fondslist[$i+3]->innerText;
				$o =~ s/\.//;
				$s =~ s/\.//;
				$p =~ s/\.//;
				$o =~ s/,/./;
				$s =~ s/,/./;
				$p =~ s/,/./;
				next if $s eq "";
		
				$fondsen{$f}->{open}=$o;
				$fondsen{$f}->{slot}=$s;
				$fondsen{$f}->{prev}=$p;
				$fondsen{$f}->{hoog}=max($s,$o);
				$fondsen{$f}->{laag}=min($s,$o);
			}
		}
    
		foreach my $ff (keys %fondsen) {
			my ($day,$month,$year) = split("-",$fondsen{$ff}->{datum});
			my ($hour,$minut) = split(":",$fondsen{$ff}->{tijd});
			my $time_t = POSIX::mktime(0,$minut,$hour,$day,$month-1,$year-1900);
			$fdbh->storeKoers($ff,$time_t,$fondsen{$ff}->{slot},$fondsen{$ff}->{open},$fondsen{$ff}->{hoog},$fondsen{$ff}->{laag},0,$fondsen{$ff}->{prev});
			$self->outputKoers($ff,$time_t,$fondsen{$ff}->{slot},$fondsen{$ff}->{open},$fondsen{$ff}->{hoog},$fondsen{$ff}->{laag},0,$fondsen{$ff}->{prev});
		}
	}
}

1
