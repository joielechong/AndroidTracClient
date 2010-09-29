{
    package ETCS::DRU_Ansaldo;
    require Exporter;
    @ISA = qw(ETCS::JRU);
    our %codeermsg = ( 'MESSAGE_FROM_BALISE' => 6,
		       'GENERAL_MESSAGE' => 1,
		       'BALISE_GROUP_ERROR' => 12,
		       'CHANGEMENT_DE_MODE' => 254,
		       'CHANGEMENT_DE_NIVEAU' => 253,
		       'DRIVER_ACTIONS' => 11,
		       'ETALONNAGE_AUTO_ODOMETRIE ' => 252,
		       'ETALONNAGE_AUTO_ODOMETRIE' => 252,
		       'EVENTS' => 5,
		       'INFORMATIONS_VITESSES_DISTANCE' => 255,
		       'MESSAGE_DATA_ENTRY_COMPLETED' => 255,
		       'MESSAGE_EMERGENCY_BRAKE_STATE' => 3,
		       'MESSAGE_SERVICE_BRAKE_STATE' => 4,
		       'MESSAGE_TO_RBC' => 10,
		       'MESSAGE_FROM_RBC' => 9,
		       'PLAIN TEXT MESSAGE' => 16
		       );
    
    use ETCS::Structure qw(%ertms_fields encode_ansaldofield);
    
    BEGIN {
        $ETCS::DRU_Ansaldo::VERSION = "0.01";
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
        $header = readline($fh);
        $header = readline($fh);
        $header = readline($fh);
	my @fields = split(';',$header);
        bless $self;
        $self->{reccount} = 0; 
        $self->{startrecord} = 0;
        $self->{filterState}=0;
        $self->{nid_message_wanted} = undef;
        $self->{processing} = 1;
        $self->{defined_fields} = ();
        @{$self->{defined_fields}} = @fields;
        $self->{record_number} = 0;
        ETCS::Structure::SetAnsaldo();
        ETCS::JRU::SetAnsaldo();
	
        return $self;
    }
    
    sub next {
        my $self = shift;
        my $message;
        my $record_data;
        my $msgstat;
	my $line;
	
        do {
            do {
		unless (defined($line=readline($self->{FileHandle}))) {
                    return undef;
                }
                $self->{record_number} += 1;
            } until ($self->{record_number} >= $self->{startrecord});
	    
	    $line =~ s/\";\"/MAGIC191059/g;    # vang de contructie .....;";";...... af
	    my $x_text_bewaar=undef;           # goede verwerking van X_TEXT, deze kan ook een ; bevatten. 
	    if ($line =~ /;\"(X_TEXT=.*)\"$/) {
		$x_text_bewaar = $1;
		$line =~ s/;\"X_TEXT=.*$//;
#		print "Bewaar X_TEXT: $x_text_bewaar\n";
	    }
		
  	    my @fields=split(';',$line);
#      	     print Dumper(@fields);
	    map (s/MAGIC191059/;/ , @fields);
	    push @fields,$x_text_bewaar if defined($x_text_bewaar);
	    
	    my $fldcnt=0;
	    my $bitstream="";
	    my $fieldnames=$self->{defined_fields};
	    my $jru_nid_message = $codeermsg{$fields[0]};
	    
	    unless (defined($jru_nid_message)) {
		print "Onbekend bericht ".$fields[0]."\n";
		$message = ETCS::JRU::new();
		$message->{MessageType} = "SKIP: Onbekend bericht";
		return $message;
	    }
	    
	    if ($jru_nid_message == 255) {
#        	   print "Bericht niet ondersteund ".$fields[0]."\n";
		$message = ETCS::JRU::new();
		$message->{MessageType} = "SKIP: ".$fields[0];
		return $message;
	    }
	    
	    $bitstream .= encode_ansaldofield("JRU_NID_MESSAGE=".$jru_nid_message);
	    my $l_message=$fields[1];
	    $bitstream .= encode_ansaldofield("L_MESSAGE=".$fields[1]);
	    my ($dag,$maand,$jaar) = split('/',$fields[2]);
	    $bitstream .= encode_ansaldofield("JRU_YEAR=".$jaar);
	    $bitstream .= encode_ansaldofield("JRU_MONTH=".$maand);
	    $bitstream .= encode_ansaldofield("JRU_DAY=".$dag);
	    my ($uur,$min,$sec,$tts) = split(":",$fields[3]);
	    $bitstream .= encode_ansaldofield("JRU_HOUR=".$uur);
	    $bitstream .= encode_ansaldofield("JRU_MINUTES=".$min);
	    $bitstream .= encode_ansaldofield("JRU_SECONDS=".$sec);
	    $bitstream .= encode_ansaldofield("JRU_TTS=".($tts/50));
	    my $qscale;
	    if ($fields[4] == '1') {
		$qscale=1;
	    } elsif ($fields[4] == '10') {
		$qscale=2;
	    } elsif ($fields[4] == '0.1') {
		$qscale=0;
	    } else {
		$qscale=3;
	    }
	    $bitstream .= encode_ansaldofield("Q_SCALE=".$qscale);
	    if ($fields[5] eq "Unknown") {
		$bitstream .= unpack("B24",pack("H*","ffffff"))
        	} else {
        	    $bitstream .= encode_ansaldofield("NID_C=".$fields[5]);;
        	    $bitstream .= encode_ansaldofield("NID_BG=".$fields[6]);
        	}
	    $bitstream .= encode_ansaldofield("D_LRBG=".($fields[7]/$fields[4]));
	    $bitstream .= encode_ansaldofield("Q_DIRLRBG=".$fields[8]); 
	    $bitstream .= encode_ansaldofield("Q_DLRBG=".$fields[9]); 
	    $bitstream .= encode_ansaldofield("L_DOUBTOVER=".($fields[10]/$fields[4]));
	    $bitstream .= encode_ansaldofield("L_DOUBTUNDER=".($fields[11]/$fields[4]));
	    $bitstream .= encode_ansaldofield("V_TRAIN=".($fields[12]/5));
	    $bitstream .= encode_ansaldofield("JRU_DRIVER_ID=Unknown");      # DRIVER_ID altijd unknown
	    $bitstream .= encode_ansaldofield("NID_OPERATIONAL=".$fields[17]);
	    $bitstream .= encode_ansaldofield("M_LEVEL=".$fields[18]);
	    $bitstream .= encode_ansaldofield("M_MODE=".$fields[19]);
	    
	    for (my $i=20;$i<=$#fields;$i++) {
		$bitstream .= encode_ansaldofield($fields[$i]);
	    }
	    
	    print $jru_nid_message,' ',int($fields[1]),' ',$fields[2],' ',$bitstream,"\n"  if $ETCS::Debug::Log;
	    
	    my $l=8*$l_message;
	    $record_data = pack("B".$l,$bitstream);               
	    
	    print "Parsing record ".$self->{record_number}."\n" if $ETCS::Debug::Log;
	    
	    # read record_data
            
            if (defined($self->{nid_message_wanted})) {
                my $nid_message = unpack("C",substr($record_data,0,1));
                if ($nid_message == $self->{nid_message_wanted}) {
                    $message = ETCS::JRU::new();
                    $self->{processing} = 1;
                }  
            }
	    
            if ($self->{processing}) {
                $bitstream = unpack("B*",$record_data);
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
