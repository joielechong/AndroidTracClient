{
    package ETCS::DRU_bombardier;
    require Exporter;
    @ISA = qw(ETCS::JRU);

    BEGIN {
        $ETCS::DRU_bombardier::VERSION = "0.01";
#
#    0.01 Initial version
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
        read($fh, $header, 0x20) or die "Kan Bombardier JRU header data niet lezen";
#        print unpack("H*",$header),"\n";
        bless $self;
        $self->{reccount} = 0; #unpack("V*", pack("H*",unpack("H*",substr($header,0x2c))."00"));
        $self->{startrecord} = 0;
        $self->{filterState}=0;
        $self->{nid_message_wanted} = undef;
        $self->{processing} = 1;
        
        ETCS::JRU::SetSubset27v224();
        print "Druk op enter\n"; getc();
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
        $self->{processing} = 1;
    }
    
    sub setFilterMessage {
        my $self = shift;
        $self->{nid_message_wanted} = shift;
        $self->{processing} = 0;
    }
    
    sub stopProcessing {
        my $self = shift;
        $self->{processing} = 0;
    }
    
    sub startProcessing {
        my $self = shift;
        $self->{processing} = 1
    }
    
    sub next {
        my $self = shift;
        my $message;
        my $record_data;
        my $msgstat;
        $self->{record_number} = 0;

        do {
            do {
                my $record_header;
                if (read($self->{FileHandle}, $record_header, 27) != 27) {
                    return undef;
                }
        	# parse record_header
                $self->{record_type} = unpack("C",substr($record_header,0,1));
                $self->{record_number} += 1;
                $self->{record_length} = unpack("n",substr($record_header,1,2))>>5;
#                print "header: type=" . $self->{record_type} . " number=" . $self->{record_number} . " length=" . $self->{record_length} . "\n";

		my $record_extra = "";                
                if ($self->{record_length} > 27) {
                    my $extra_size = $self->{record_length} - 27;
#                    print "extra_size = " . $extra_size  . "\n";
                    if (read($self->{FileHandle}, $record_extra, $extra_size) != $extra_size) {
                    	print "Read error at 92";
                        return undef;
                    }
        	}
        
                print "Parsing record ".$self->{record_number}."\n" if $ETCS::Debug::Log;
        	
        	# read record_data
                $record_data = "";
                if ($self->{record_length} > 0) {
        	    # read record data
                    $record_data = $record_header . $record_extra;
                }
            } until ($self->{record_number} >= $self->{startrecord});
            
            if (defined($self->{nid_message_wanted})) {
                my $nid_message = unpack("C",substr($record_data,0,1));
                if ($nid_message == $self->{nid_message_wanted}) {
                    $message = ETCS::JRU::new();
                    $self->{processing} = 1;
                }  
            }

            if ($self->{processing}) {
                my $bitstream = unpack("B*",$record_data);
                # parse JRU record
                $message = ETCS::JRU::new();
                $message->setFilter($self->{filter}) if $self->{filterState};
                $msgstat = $message->setMessage($bitstream);
            } else {
                $msgstat = -1;
            }
        } while ($msgstat < 0);
        return $message;
    }
}
 
1;
