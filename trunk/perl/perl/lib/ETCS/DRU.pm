{
    package ETCS::DRU;
    require Exporter;
    @ISA = qw(ETCS::JRU);

    BEGIN {
        $ETCS::DRU::VERSION = "0.5";
#
#    0.5 Kleine bug fix
#    0.4 Date time filtering
#
    }
  
    use Data::Dumper;
    use strict;
    
    sub new {
        my $fh = shift;
        my $header;
    
        my $self = {};
        $self->{FileHandle} = $fh;
        seek $fh,0,0;
        read($fh, $header, 0x2f) or die "Kan JRU header data niet lezen";
        bless $self;
        $self->{reccount} = unpack("v*", substr($header, 0x2c, 2));
        $self->{startrecord} = 0;
        $self->{filterState}=0;
        return $self;
    }
    
    sub seek {
        my ($self,$recordnr) = @_;
        
        $self->{startrecord} = $recordnr;
    }
    
    sub setFilter {
        my $self = shift;
        $self->{filterState} = 1;
        my $mindate = shift;
        my $mintime = shift;
        $self->{filter}->{mintime} = $mindate.$mintime;
        my $maxdate = shift;
        my $maxtime = shift;
        $self->{filter}->{maxtime} = $maxdate.$maxtime;
    }
    
    sub next {
        my $self = shift;
        my $message;
        my $record_data;
        my $msgstat;

        do {
            do {
                my $record_header;
                if (read($self->{FileHandle}, $record_header, 26) != 26) {
                    return undef;
                }
        	# parse record_header
                $self->{record_type} = substr($record_header, 3, 7);
                $self->{record_number} = 1 + substr($record_header, 10, 8) - 1;
                $self->{record_length} = 1 + substr($record_header, 18, 5) - 1;
        
                print "Parsing record ".$self->{record_number}."\n" if $ETCS::Debug::Log;
        	
        	# read record_data
                $record_data = "";
                if ($self->{record_length} > 0) {
        	# read record data
                    read($self->{FileHandle}, $record_data, $self->{record_length}) or die "Kan record data niet lezen";
                }
            } until ($self->{record_number} >= $self->{startrecord});
            my $bitstream = unpack("B*",$record_data);
            if ($self->{record_type} eq "JRU    ") {
                # parse JRU record
                $message = ETCS::JRU::new();
            } elsif ($self->{record_type} eq "DRUETCS") {
                # parse DRU record
                $message = ETCS::DRUETCS::new();
            } else {
                $message = ETCS::Structure::new();
                $message->{MessageType} = 'UnknownMessage';
            }
            $message->setFilter($self->{filter}) if $self->{filterState};
            $msgstat = $message->setMessage($bitstream);
        } while ($msgstat < 0);
        return $message;
    }
}

{
    package ETCS::DRUETCS;
    require Exporter;
    use Data::Dumper;
    @ISA = qw(ETCS::Structure);
    use strict;
  
    BEGIN {
        $ETCS::DRUETCS::VERSION = "0.2";
#
#      0.2 Header afgesplitst
#          Date/time filtering
#
    }

    my @msg_drupacket = ("DRU_NID_PACKET","DRU_L_PACKET","DRU_NID_SOURCE","DRU_M_DIAG","DRU_NID_CHANNEL","L_TEXT","X_TEXT");
    
    sub new {
      my $self={};
    $self->{Header} = ETCS::DRUETCS::Header::new();
      bless $self;
      $self->{MessageType}='DRUMessage';
      return $self;
    }
    
    sub setFilter {
        my $self = shift;
        $self->{filter} = shift;
    }
    
  sub filteredOut {
    my $self = shift;
    
    return 0 unless defined($self->{filter});
    
    my $msghdr = $self->{Header}->{Fields};
    my $msgtime = sprintf("%4.4d%2.2d%2.2d%2.2d%2.2d%2.2d%3.3d",
                          2000+$msghdr->[2]->{Decimal},
                          $msghdr->[3]->{Decimal},
                          $msghdr->[4]->{Decimal},
                          $msghdr->[5]->{Decimal},
                          $msghdr->[6]->{Decimal},
                          $msghdr->[7]->{Decimal},
                          $msghdr->[8]->{Decimal});
    $self->{filter}->{msgtime} = $msgtime;

    return ( ($msgtime < $self->{filter}->{mintime}) || ($msgtime > $self->{filter}->{maxtime}));
  }
    
    sub setMessage {
        my ($self,$bits) = @_;
    
	$self->{Bitstream} = $bits;
	$self->{Hexstream} = unpack("H*",pack("B*",$bits));
        my $bitpos = $self->{Header}->setMessage($bits);

        return -1 if $self->filteredOut();
    
        return $self->process_bits(substr($bits,$bitpos),\@msg_drupacket,0,0);
    }
}

{
    package ETCS::DRUETCS::Header;
    require Exporter;
    use Data::Dumper;
    @ISA = qw(ETCS::Structure);
    use strict;
  
    BEGIN {
        $ETCS::DRUETCS::Header::VERSION = "0.1";
    }

    my @msg_druheader = ("DRU_NID_MESSAGE","DRU_L_MESSAGE","DRU_YEAR","DRU_MONTH","DRU_DAY","DRU_HOUR","DRU_MINUTES","DRU_SECONDS","DRU_TTS","PADDING_T");
    sub new {
      my $self={};
      bless $self;
      return $self;
    }
    
    sub setMessage {
        my ($self,$bits) = @_;
    
	$self->{Bitstream} = $bits;
	$self->{Hexstream} = unpack("H*",pack("B*",$bits));
        return $self->process_bits($bits,\@msg_druheader,0,0);
    }
}
 
1;
