{
  package ETCS::Canape;
  require Exporter;
  @ISA = qw(ETCS::Structure);
  use Data::Dumper;
  use strict;
  
  BEGIN {
    $ETCS::Canape::VERSION = "0.3";
  }
  
  sub new {
    my $self = {};
    $self->{MessageType}='CanapeMessage';
    bless $self;
    return $self;
  }
  
  sub connectionRequest {
    my ($self,$radionumber) = @_;
    
    $self->{Connection} = 'ConnectionRequest';
    $self->{RadioNumber} = $radionumber;
  }

  sub connectionConfirmation {
    my $self = shift;
    
    $self->{Connection} = 'ConnectionConfirmation';
  }

  sub connectionFailure {
    my $self = shift;
    
    $self->{Connection} = 'ConnectionFailure';
  }

  sub disconnectRequest {
    my $self = shift;
    
    $self->{Connection} = 'DisconnectionRequest';
  }

  sub resetConnection {
    my $self = shift;
    
    $self->{Connection} = 'ResetConnection';
  }
  
  sub processCanapeLine {
    my $string = shift;
    
    my $input =/^(\d+\:\d+\:\d+\,\d+) VariableCan(...) \$ (.*)$/;
    my $tijd = $1;
    my $can=$2;
    my $hexstring = $3;
    my $msglen;
    
    if ($can == 896) {
      my @hex = map (hex,split (" ",$hexstring));
      my $hex1 = join("",split(" ",$hexstring));
      my $bits = unpack("B*",pack("H*",$hex1));
      my $self;
    
      if ($hex[1] == 0xE) {
	$self = new();
	$self->{BitMessage} = $bits;
	$self->{HexMessage} = $hex1;
        $self->{Balise} = ETCS::Balise::new();
          
        print "$tijd\n" if $ETCS::Debug::Log;
        $self->{Timestamp} = hex(substr($hex1,8,4));
        $self->{Antenna} = $hex[6];
        $self->{Location}->{Nominal} = hex(substr($hex1,14,8))/128.0;
        $self->{Location}->{Upper}   = hex(substr($hex1,22,8))/128.0;
        $self->{Location}->{Lower}   = hex(substr($hex1,30,8))/128.0;
          
        $self->{Tijd} = ($tijd);
      
        my $balsize = hex(substr($hex1,50,4));
        my $balmesg = substr($bits,8*27,$balsize);
        print "Balise header\n" if $ETCS::Debug::Log;
        $self->{Balise}->setMessage($balmesg);
        return $self;
      } elsif ($hex[1] == 0x14) {
	$self = new();
        $self->{Tijd} = $tijd;
        print "$tijd\n" if $ETCS::Debug::Log;
        my $type = $self->{Type} = $hex[2];
        $self->{Radio} = [hex(substr($hex1,6,2)),hex(substr($hex1,8,2)),hex(substr($hex1,10,2))];
        if ($type == 0) {
          $self->connectionConfirmation();
        } elsif ($type == 1) {
          $self->connectionFailure();
        } elsif ($type == 2) {
          $self->{MessageFromRBC} = ETCS::MessageFromRBC::new();
          $self->{MessageFromRBC}->setMessageType("MessageFromRBC");
          $msglen=unpack("N",pack("B32",substr($bits,48,32)));
	  $self->{MessageFromRBC}->setMessage(substr($bits,48+32,$msglen));
        } else {
  	print "Onbekend bericht type: $type\n";
  	getc();
        }
        return $self;
      } elsif ($hex[1] == 0x16) {
  	print "$tijd\n$bits\n";
        getc();
	$self = new();
        $self->{Tijd} = $tijd;
        my $type = $self->{Type} = $hex[2];
        $self->{Radio} = [hex(substr($hex1,6,2)),hex(substr($hex1,8,2)),hex(substr($hex1,10,2))];
  	if ($type == 2) {
	  my $self->{MessageFromRBCEmergency} = ETCS::MessageFromRBC::new();
          $self->{MessageFromRBCEmergency}->setMessageType("MessageFromRBCEmergency");
          $msglen=unpack("N",pack("B32",substr($bits,48,32)));
	  $self->{MessageFromRBCEmergency}->setMessage(substr($bits,48+32,$msglen));
  	} else {
  	  print "Onbekend bericht type: $type\n";
  	  getc();
  	}
        return $self;
      } elsif ($hex[1] == 0x15) {
  	print "$tijd\n" if $ETCS::Debug::Log;
	$self = new();
        $self->{Tijd} = $tijd;
        my $type = $self->{Type} = $hex[2];
        $self->{Radio} = [hex(substr($hex1,6,2)),hex(substr($hex1,8,2)),hex(substr($hex1,10,2))];
  	if ($type == 0) {
  	  my $length = $hex[6];
  	  my $radionumber=substr($hex1,14,2*$length);
          $self->connectionRequest($radionumber);
	} elsif ($type == 1) {
          $self->disconnectRequest();
	} elsif ($type == 2) {
          $self->resetConnection();
	} elsif ($type == 3) {
	  $self->{MessageToRBC} = ETCS::MessageToRBC::new();
          $self->{MessageToRBC}->setMessageType("MessageToRBC");
          $msglen=unpack("N",pack("B32",substr($bits,48,32)));
	  $self->{MessageToRBC}->setMessage(substr($bits,48+32,$msglen));
	} else {
	  print "Onbekend bericht type: $type\n";
	  getc();
        }
        return $self;
      }
    }
    return undef;
  }
}

1;