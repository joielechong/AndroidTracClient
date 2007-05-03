{
  package ETCS::RBC;
  use Exporter;
  use Data::Dumper;
  @ISA = qw(ETCS::Structure);
  use strict;
  
  BEGIN {
  $ETCS::RBC::VERSION = "0.3";
  }
  
   
  sub new {
    my $self= {};
    bless $self;
    return $self;
  }
  
  sub setMessageType {
    my ($self,$value) = @_;
    
    $self->{MessageType} = $value;
    return  $value;
  }
}

{
  package ETCS::MessageFromRBC;
  use Exporter;
  use Data::Dumper;
   
  @ISA = qw(ETCS::RBC);
  use strict;
  
  my @msg_tracknames;
  my @msg_trackfields;
  
  BEGIN {
    $ETCS::MessageFromRBC::VERSION = "0.3";
#
# 0.3 hersynchronisatie na foutief bericht
# 0.2 code cleanup
#

    $msg_tracknames[  2] = "SR Authorisation";
    $msg_trackfields[  2] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","Q_SCALE","D_SR"];
    
    $msg_tracknames[  3] = "Movement Authority";
    $msg_trackfields[  3] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
    
    $msg_tracknames[  6] = "Acknowledgement of exit from TRIP mode";
    $msg_trackfields[  6] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
    
    $msg_tracknames[  8] = "Acknowledgement of Train Data";
    $msg_trackfields[  8] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","T_TRAIN"];
    
    $msg_tracknames[ 15] = "Conditional Emergency Stop";
    $msg_trackfields[ 15] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","NID_EM","Q_SCALE","Q_DIR","D_EMERGENCYSTOP"];
    
    $msg_tracknames[ 16] = "Unconditional Emergency Stop";
    $msg_trackfields[ 16] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","NID_EM"];
    
    $msg_tracknames[ 18] = "Revocation of Emergency Stop";
    $msg_trackfields[ 18] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","NID_EM"];
    
    $msg_tracknames[ 24] = "General Message";
    $msg_trackfields[ 24] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
    
    $msg_tracknames[ 27] = "SH Refused";
    $msg_trackfields[ 27] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","T_TRAIN"];
    
    $msg_tracknames[ 28] = "SH Authorised";
    $msg_trackfields[ 28] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","T_TRAIN"];
    
    $msg_tracknames[ 32] = "Configuration Determination";
    $msg_trackfields[ 32] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","M_VERSION"];
    
    $msg_tracknames[ 33] = "MA with shifted Location Reference";
    $msg_trackfields[ 33] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","Q_SCALE","D_REF"];
    
    $msg_tracknames[ 34] = "Track Ahead Free Request";
    $msg_trackfields[ 34] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG","Q_SCALE","Q_DIR","D_TAFDISPLAY","L_TAFDISPLAY"];
    
    $msg_tracknames[ 39] = "Acknowledgement of termination of a communication session";
    $msg_trackfields[ 39] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
    
    $msg_tracknames[ 40] = "Train Rejected";
    $msg_trackfields[ 40] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
    
    $msg_tracknames[ 41] = "Train Accepted";
    $msg_trackfields[ 41] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","M_ACK","NID_LRBG"];
  }
  
   
  sub new {
    my $self= {};
    bless $self;
    $self->setMessageType("MessageFromRBC");
    return $self;
  }
  
  sub setMessage{
    my ($self,$bits) =@_;
    
    $self->{BitMessage} = $bits;
    my $length = hex(unpack("H*",pack("B10",substr($bits,8,10))))>>6;
    my $nid_message=unpack("C",pack("B8",substr($bits,0,8)));
    my $msgpos = $self->process_bits(substr($bits,0,8*$length),$msg_trackfields[$nid_message],0,0);
    if ($msgpos == -1) {
      $self->addField('NID_MESSAGE',substr($bits,0,8));
      $self->setFieldText('NID_MESSAGE',"Error in field NID_MESSAGE");
      $self->addField('L_MESSAGE',substr($bits,8,10));
      return -1;
    }
    my $nrpack=0;
    my $msglen = 8*$length;
    while ($msgpos < $msglen-7) {
      my $pack = $self->addPacket($nrpack);
      my $rv = $pack->process_packet(substr($bits,$msgpos,$msglen-$msgpos));
      $nrpack++;
      last if $rv == -1;
      $msgpos += $rv;
    }
    return $msgpos;
  }

  sub addPacket {
    my ($self,$nrpack) = @_;
    
    if ($nrpack == 0) {
      $self->{Packets} = [];
    }
    my $pack = ETCS::TrackPacket::new();
    $self->{Packets}->[$nrpack] = $pack;
    $pack->{Packetnr} = $nrpack;
    return $pack;
  }

  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    
    if ($field eq "NID_MESSAGE") {
      my $intval = unpack("C",pack("B*",$bitstream));
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$msg_tracknames[$intval]);
      
      return 1;
    } else {
      return $self->SUPER::displayValue($field,$bitstream);
    }
  }
}

{
  package ETCS::MessageToRBC;
  use Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::RBC);
  use strict;

  my @msg_trainnames;
  my @msg_trainfields;
  
  BEGIN {
    $ETCS::MessageToRBC::VERSION = "0.3";
#
# 0.3 hersynchronisatie na foutief bericht
# 0.2 code cleanup
#
    
    $msg_trainnames[129] = "Validated Train Data";
    $msg_trainfields[129] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[130] = "Request for shunting";
    $msg_trainfields[130] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[132] = "MA Request";
    $msg_trainfields[132] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","Q_TRACKDEL"];
    
    $msg_trainnames[136] = "Train Position Report";
    $msg_trainfields[136] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[137] = "Request to Shorten MA granted";
    $msg_trainfields[137] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","T_TRAIN"];
    
    $msg_trainnames[138] = "Request to shorten MA is rejected";
    $msg_trainfields[138] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","T_TRAIN"];
    
    $msg_trainnames[146] = "Acknowledgement";
    $msg_trainfields[146] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","T_TRAIN"];
    
    $msg_trainnames[147] = "Acknowledgement of Emergency Stop";
    $msg_trainfields[147] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","NID_EM","Q_EMERGENCYSTOP"];
    
    $msg_trainnames[149] = "Track Ahead Free Granted";
    $msg_trainfields[149] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[150] = "End of Mission";
    $msg_trainfields[150] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[155] = "Initiation of a communication session";
    $msg_trainfields[155] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[156] = "Termination of a communication session";
    $msg_trainfields[156] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
    
    $msg_trainnames[157] = "SOM Position report";
    $msg_trainfields[157] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE","Q_STATUS"];
    
    $msg_trainnames[159] = "Session established";
    $msg_trainfields[159] = ["NID_MESSAGE","L_MESSAGE","T_TRAIN","NID_ENGINE"];
  }
  
  
   
  sub new {
    my $self= {};
    bless $self;
    $self->setMessageType("MessageToRBC");
    return $self;
  }
  
  sub setMessage{
    my ($self,$bits) =@_;
    
    $self->{BitMessage} = $bits;
    my $length = hex(unpack("H*",pack("B10",substr($bits,8,10))))>>6;
    my $nid_message=unpack("C",pack("B8",substr($bits,0,8)));
    my $msgpos = $self->process_bits(substr($bits,0,8*$length),$msg_trainfields[$nid_message],0,0);
    if ($msgpos == -1) {
      $self->addField('NID_MESSAGE',substr($bits,0,8));
      $self->setFieldText('NID_MESSAGE',"Error in field NID_MESSAGE");
      $self->addField('L_MESSAGE',substr($bits,8,10));
      return -1;
    }
    my $nrpack=0;
    my $msglen = 8*$length;
    while ($msgpos < $msglen-7) {
      my $pack = $self->addPacket($nrpack);
      my $rv = $pack->process_packet(substr($bits,$msgpos,$msglen-$msgpos));
      $nrpack++;
      last if $rv == -1;
      $msgpos += $rv;
    }
    return $msgpos;
  }

  sub addPacket {
    my ($self,$nrpack) = @_;
    
    if ($nrpack == 0) {
      $self->{Packets} = [];
    }
    my $pack = ETCS::TrainPacket::new();
    $self->{Packets}->[$nrpack] = $pack;
    $pack->{Packetnr} = $nrpack;
    return $pack;
  }

  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    
    if ($field eq "NID_MESSAGE") {
      my $intval = unpack("C",pack("B*",$bitstream));
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$msg_trainnames[$intval]);
      
      return 1;
    } else {
      return $self->SUPER::displayValue($field,$bitstream);
    }
  }
}

1;
