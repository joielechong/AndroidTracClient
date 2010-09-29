{
  package ETCS::JRU;
  require Exporter;
  use Data::Dumper;
  @ISA = qw(ETCS::Structure);
  use strict;
  my $subset27version = 200;
  my $unknownMessages = 0;
  
  BEGIN {
    $ETCS::JRU::VERSION = "0.9";
#
#     0.9 Added fields of Ansaldo JRU
#     0.8 Correction of TargetDistance
#     0.7 Correction of definition of BALISE_GROUP_ERROR message
#         Correction of call to process_bits in BaliseGroupError message
#     0.6 Provision for resynchronisation
#     0.5 Date/time filtering
#
  }
  
  sub new {
    my $self={};
    $self->{Header} = ETCS::JRU::Header::new();
    bless $self;
    $self->{MessageType}='JRUMessage';
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
    my ($self,$bitstream) = @_;
    
    $self->{Bitstream} = $bitstream;
    $self->{Hexstream} = unpack("H*",pack("B*",$bitstream));
    
    my $bitpos = $self->{Header}->setMessage($bitstream);
    
    return -1 if $self->filteredOut();
    
    my $nid_message = $self->{Header}->{Fields}->[0]->{Decimal};
    
    my $msg = substr($bitstream,$bitpos);
    print "JRU Message $nid_message\n$msg\n" if $ETCS::Debug::Log;
    if ($nid_message == 0) {
      $self->{JRUStateMsg} = ETCS::JRU::JRUState::new();
      return $self->{JRUStateMsg}->setMessage($msg);
    } elsif ($nid_message == 1) {
      return $bitpos;  # GENERAL MESSAGE only has a header.
    } elsif ($nid_message == 2) {
      $self->{DataEntryCompleted} = ETCS::JRU::DataEntryCompleted::new();
      return $self->{DataEntryCompleted}->setMessage($msg);
    } elsif ($nid_message == 3) {
      $self->{EmergencyBrake} = ETCS::JRU::BrakeState::new();
      return $self->{EmergencyBrake}->setMessage($msg);
    } elsif ($nid_message == 4) {
      $self->{ServiceBrake} = ETCS::JRU::BrakeState::new();
      return $self->{ServiceBrake}->setMessage($msg);
    } elsif ($nid_message == 5) {
      $self->{Events} = ETCS::JRU::Events::new();
      return $self->{Events}->setMessage($msg);
    } elsif ($nid_message == 6) {
      $self->{BaliseTelegram} = ETCS::Balise::new();
      return $self->{BaliseTelegram}->setMessage($msg);
    } elsif ($nid_message == 9) {
      $self->{MessageFromRBC} = ETCS::MessageFromRBC::new();
      return $self->{MessageFromRBC}->setMessage($msg);
    } elsif ($nid_message == 10) {
      $self->{MessageToRBC} = ETCS::MessageToRBC::new();
      return $self->{MessageToRBC}->setMessage($msg);
    } elsif ($nid_message == 11) {
      $self->{DriversActions} = ETCS::JRU::DriversActions::new();
      return $self->{DriversActions}->setMessage($msg);
    } elsif ($nid_message == 12) {
      $self->{BaliseGroupError} = ETCS::JRU::BaliseGroupError::new();
      return $self->{BaliseGroupError}->setMessage($msg);
    } elsif ($nid_message == 13) {
      $self->{RadioLinkSupervisionError} = ETCS::JRU::RadioLinkSupervisionError::new();
      return $self->{RadioLinkSupervisionError}->setMessage($msg);
    } elsif ($nid_message == 14) {
      $self->{StmInformation} = ETCS::JRU::StmInformation::new();
      return $self->{StmInformation}->setMessage($msg);
    } elsif ($subset27version == 200)  {

      #    $msg_jrunames[15] = "PREDEFINED TEXT MESSAGE";
      #    $msg_jrunames[16] = "PLAIN TEXT PACKET";
      #    $msg_jrunames[17] = "INDICATIONS TO THE DRIVER";
      #    $msg_jrunames[18] = "DATA FROM EXTERNAL SOURCES";
      #    $msg_jrunames[19] = "DATA FROM VOICE RADIO";
      #    $msg_jrunames[20] = "ETCS_ID";
      #    $msg_jrunames[25] = "MVB_INFORMATION";
      if ($nid_message == 15) {
        $self->{PredefinedTextMessage} = ETCS::JRU::PredefinedTextMessage::new();
        return $self->{PredefinedTextMessage}->setMessage($msg);
      } elsif ($nid_message == 16)  {
        $self->{PlainTextMessage} = ETCS::JRU::PlainTextMessage::new();
        return $self->{PlainTextMessage}->setMessage($msg);
      } elsif ($nid_message == 17)  {
        $self->{IndicationsToDriver} = ETCS::JRU::IndicationsToDriver::new();
        return $self->{IndicationsToDriver}->setMessage($msg);
      } elsif ($nid_message == 20)  {
        $self->{ETCSID} = ETCS::JRU::ETCSID::new();
        return $self->{ETCSID}->setMessage($msg);
      } elsif ($nid_message == 254) {
      	$self->{ModeChange} = ETCS::JRU::ModeChange::new();
	return $self->{ModeChange}->setMessage($msg);
      } elsif ($nid_message == 253) {
      	$self->{LevelChange} = ETCS::JRU::LevelChange::new();
	return $self->{LevelChange}->setMessage($msg);
      } elsif ($nid_message == 252) {
      	$self->{OdometryCalibration} = ETCS::JRU::OdometryCalibration::new();
	return $self->{OdometryCalibration}->setMessage($msg);
      }
      $self->{Encoded}=$msg;
      print "Onbekende JRU message $nid_message\n";
      $unknownMessages++;
      return 1;

    } elsif ($subset27version == 224)  {
      #    $msg_jrunames[15] = "DATA FROM EXTERNAL SOURCES";
      #    $msg_jrunames[16] = "START DISPLAYING FIXED TEXT MESSAGE";
      #    $msg_jrunames[17] = "STOP DISPLAYING FIXED TEXT MESSAGE";
      #    $msg_jrunames[18] = "START DISPLAYING PLAIN TEXT MESSAGE";
      #    $msg_jrunames[19] = "STOP DISPLAYING PLAIN TEXT MESSAGE";
      #    $msg_jrunames[20] = "MOST RESTRICTIVE SPEED PROFILE";
      #    $msg_jrunames[21] = "TARGET SPEED";
      #    $msg_jrunames[22] = "TARGET DISTANCE";
      #    $msg_jrunames[23] = "RELEASE SPEED";
      #    $msg_jrunames[24] = "WARNING";
      #    $msg_jrunames[25] = "SR SPEED/DISTANCE";
      #    $msg_jrunames[26] = "STM SELECTED";
      #    $msg_jrunames[27] = "PERMITTED SPEED";
      #    $msg_jrunames[151] = "STATE ACK";
      #    $msg_jrunames[152] = "JRU FAILURE";
      #    $msg_jrunames[153] = "START OF TRANSMISSION";
      #    $msg_jrunames[154] = "END OF TRANSMISSION";
      if (($nid_message == 16) || ($nid_message == 17)) {
        $self->{PredefinedTextMessage} = ETCS::JRU::PredefinedTextMessage::new();
        return $self->{PredefinedTextMessage}->setMessage($msg);
      } elsif (($nid_message == 18) || ($nid_message == 19)) {
        $self->{PlainTextMessage} = ETCS::JRU::PlainTextMessage::new();
        return $self->{PlainTextMessage}->setMessage($msg);
      } elsif ($nid_message == 20)  {
        $self->{MRSPSpeed} = ETCS::JRU::MRSPSpeed::new();
        return $self->{MRSPSpeed}->setMessage($msg);
      } elsif ($nid_message == 21)  {
        $self->{TargetSpeed} = ETCS::JRU::TargetSpeed::new();
        return $self->{TargetSpeed}->setMessage($msg);
      } elsif ($nid_message == 22)  {
        $self->{TargetDistance} = ETCS::JRU::TargetDistance::new();
        return $self->{TargetDistance}->setMessage($msg);
      } elsif ($nid_message == 23)  {
        $self->{ReleaseSpeed} = ETCS::JRU::ReleaseSpeed::new();
        return $self->{ReleaseSpeed}->setMessage($msg);
      } elsif ($nid_message == 24)  {
        $self->{Warning} = ETCS::JRU::Warning::new();
        return $self->{Warning}->setMessage($msg);
      } elsif ($nid_message == 26)  {
        $self->{STMSelected} = ETCS::JRU::STMSelected::new();
        return $self->{STMSelected}->setMessage($msg);
      } elsif ($nid_message == 27)  {
        $self->{PermittedSpeed} = ETCS::JRU::PermittedSpeed::new();
        return $self->{PermittedSpeed}->setMessage($msg);
      } else {
	$self->{Encoded}=$msg;
      	print "Onbekende JRU message $nid_message\n";
        $unknownMessages++;
      	return 1;
      }
    } else {
      $self->{Encoded}=$msg;
      print "Onbekende JRU message $nid_message\n";
      $unknownMessages++;
      return -1;
    }
  }
  
  sub SetSubset27v224 {
    print "Set to ETCS::JRU subset 27 v2.2.4\n";
    
    $subset27version = 224;
    ETCS::Structure::SetSubset27v224();
    ETCS::JRU::Header::SetSubset27v224();
    ETCS::JRU::DataEntryCompleted::SetSubset27v224();
    ETCS::JRU::PredefinedTextMessage::SetSubset27v224();
    ETCS::JRU::PlainTextMessage::SetSubset27v224();
  }
  
  sub SetAnsaldo {
    print "Set to ETCS::JRU Ansldo variant\n";

    ETCS::Structure::SetAnsaldo();
    ETCS::JRU::PlainTextMessage::SetAnsaldo();
  }
  
  sub getUnknownMessageCount {
    return $unknownMessages;
  }
  
}

{
  package ETCS::JRU::Header;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  my @msg_jruheader = ("NID_MESSAGE","L_MESSAGE","JRU_YEAR","JRU_MONTH","JRU_DAY","JRU_HOUR","JRU_MINUTES","JRU_SECONDS","JRU_TTS","Q_SCALE","NID_LRBG","D_LRBG","Q_DIRLRBG","Q_DLRBG","L_DOUBTOVER","L_DOUBTUNDER","V_TRAIN","JRU_DRIVER_ID","NID_OPERATIONAL","JRU_M_LEVEL","M_MODE");
  my @msg_jrunames;
  
  BEGIN {
    $msg_jrunames[0] = "JRU STATE";
    $msg_jrunames[1] = "GENERAL MESSAGE";
    $msg_jrunames[2] = "DATA ENTRY/TRAIN DATA";
    $msg_jrunames[3] = "EMERGENCY BRAKE STATE";
    $msg_jrunames[4] = "SERVICE BRAKE STATE";
    $msg_jrunames[5] = "EVENTS";
    $msg_jrunames[6] = "TELEGRAM FROM BALISE";
    $msg_jrunames[7] = "MESSAGE FROM EUROLOOP";
    $msg_jrunames[8] = "MESSAGE FROM RADIO INFILL UNIT";
    $msg_jrunames[9] = "MESSAGE FROM RBC";
    $msg_jrunames[10] = "MESSAGE TO RBC";
    $msg_jrunames[11] = "DRIVER'S ACTION";
    $msg_jrunames[12] = "BALISE GROUP ERROR";
    $msg_jrunames[13] = "RADIO LINK SUPERVISION ERROR";
    $msg_jrunames[14] = "STM INFORMATION";
    $msg_jrunames[15] = "PREDEFINED TEXT MESSAGE";
    $msg_jrunames[16] = "PLAIN TEXT PACKET";
    $msg_jrunames[17] = "INDICATIONS TO THE DRIVER";
    $msg_jrunames[18] = "DATA FROM EXTERNAL SOURCES";
    $msg_jrunames[19] = "DATA FROM VOICE RADIO";
    $msg_jrunames[20] = "ETCS_ID";
    $msg_jrunames[25] = "MVB_INFORMATION";
    $msg_jrunames[254] = "MODE CHANGE";
    $msg_jrunames[253] = "LEVEL CHANGE";
    $msg_jrunames[252] = "AUTOMATIC ODOMETRY CALIBRATION";
  }    

  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    my $bitlen = $self->process_bits($bits,\@msg_jruheader,0,0);
    if ($bitlen < 0) {
      my ($package, $filename, $line) = caller;
      print "Aangeroepen uit $package, $filename, $line\n";
    }
    return $bitlen;
  }

  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    
    if ($field eq "NID_MESSAGE") {
      my $intval = unpack("C",pack("B*",$bitstream));
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$msg_jrunames[$intval]);
      
      return 1;
    } else {
      return $self->SUPER::displayValue($field,$bitstream);
    }
  }
  sub SetSubset27v224 {
    print "Set JRU::Header to subset 27 v2.2.4\n";
    
    @msg_jruheader = ("NID_MESSAGE","L_MESSAGE","JRU_YEAR","JRU_MONTH","JRU_DAY","JRU_HOUR","JRU_MINUTES","JRU_SECONDS","JRU_TTS","Q_SCALE","NID_LRBG","D_LRBG","Q_DIRLRBG","Q_DLRBG","L_DOUBTOVER","L_DOUBTUNDER","V_TRAIN","JRU_DRIVER_ID","NID_ENGINE","JRU_M_LEVEL","M_MODE");
    $msg_jrunames[15] = "DATA FROM EXTERNAL SOURCES";
    $msg_jrunames[16] = "START DISPLAYING FIXED TEXT MESSAGE";
    $msg_jrunames[17] = "STOP DISPLAYING FIXED TEXT MESSAGE";
    $msg_jrunames[18] = "START DISPLAYING PLAIN TEXT MESSAGE";
    $msg_jrunames[19] = "STOP DISPLAYING PLAIN TEXT MESSAGE";
    $msg_jrunames[20] = "MOST RESTRICTIVE SPEED PROFILE";
    $msg_jrunames[21] = "TARGET SPEED";
    $msg_jrunames[22] = "TARGET DISTANCE";
    $msg_jrunames[23] = "RELEASE SPEED";
    $msg_jrunames[24] = "WARNING";
    $msg_jrunames[25] = "SR SPEED/DISTANCE";
    $msg_jrunames[26] = "STM SELECTED";
    $msg_jrunames[27] = "PERMITTED SPEED";
    $msg_jrunames[151] = "STATE ACK";
    $msg_jrunames[152] = "JRU FAILURE";
    $msg_jrunames[153] = "START OF TRANSMISSION";
    $msg_jrunames[154] = "END OF TRANSMISSION";
  }
  
}

{
  package ETCS::JRU::DataEntryCompleted; 
#
# 0.2 subfields moved into package
#
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::DataEntryCompleted::VERSION="0.2";
  }
  use strict;
  
  my @msg_decheader = ("V_MAXTRAIN","NC_TRAIN","L_TRAIN","N_SERVICE_SECTIONS","*985","N_EMERGENCY_SECTIONS","*986",
                        "T_CUT_OFF","T_DELAY","M_LOADINGGAUGE","M_AXLELOAD","N_ITER","M_TRACTION","M_AIRTIGHT",
                        "M_ADHESION","NID_C","NID_RBC","NID_RADIO");
  my @msg_decpackets;
  
  BEGIN {
    $msg_decpackets[985] =["V_SERVICEDECEL_CAP","A_SERVICEDECEL_CAP"];
    $msg_decpackets[986] =["V_EMERGENCYDECEL_CAP","A_EMERGENCYDECEL_CAP"];
  }
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;

    my $bitlen = $self->process_bits($bits,\@msg_decheader,0,0);
    if ($bitlen < 0) {
      my ($package, $filename, $line) = caller;
      print "Aangeroepen uit $package, $filename, $line\n";
    }
    return $bitlen;
  }
  
  sub process_subfields {
    my ($pack,$msg,$dummsg,$iterdepth,$iter) = @_;

    my $bitlen = $pack->process_bits($msg,$msg_decpackets[$dummsg],$iterdepth,$iter);
    if ($bitlen < 0) {
      my ($package, $filename, $line) = caller;
      print "Aangeroepen uit $package, $filename, $line\n";
    }
    return $bitlen;
  }
  sub SetSubset27v224 {
    print "Set JRU::DataEntryCompleted to subset 27 v2.2.4\n";
    
    @msg_decheader = ("V_MAXTRAIN","NC_TRAIN","L_TRAIN","N_SERVICE_SECTIONS","*985","N_EMERGENCY_SECTIONS","*986",
                        "T_CUT_OFF","T_SERVICE_DELAY","T_EMERGENCY_DELAY","M_LOADINGGAUGE","M_AXLELOAD","M_TRACTION","M_AIRTIGHT",
                        "M_ADHESION","NID_RBC","NID_RADIO");
  }
  sub SetSubset27v229 {
    print "Set JRU::DataEntryCompleted to subset 27 v2.2.9\n";
    
    @msg_decheader = ("V_MAXTRAIN","NC_TRAIN","L_TRAIN","N_SERVICE_SECTIONS","*985","N_EMERGENCY_SECTIONS","*986",
                        "T_CUT_OFF","T_SERVICE_DELAY","T_EMERGENCY_DELAY","M_LOADINGGAUGE","M_AXLELOAD","N_ITER","M_TRACTION","M_AIRTIGHT",
                        "M_ADHESION","NID_RBC","NID_RADIO");
  }
}

{
  package ETCS::JRU::DriversActions;
#
# 0.2 M_DRIVERSACTIONS --> M_DRIVERACTIONS 
#
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::DriversActions::VERSION="0.2";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["M_DRIVERACTIONS"],0,0);
  }
}

{
  package ETCS::JRU::Warning;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::Warning::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,[],0,0);
  }
}

{
  package ETCS::JRU::OdometryCalibration;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::OdometryCalibration::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,[],0,0);
  }
}

{
  package ETCS::JRU::BaliseGroupError;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::BaliseGroupError::VERSION="0.2";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["NID_C","NID_BG","M_ERROR"],0,0);
  }
}

{
  package ETCS::JRU::RadioLinkSupervisionError;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::RadioLinkSupervisionError::VERSION="0.2";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["M_ERROR","NID_C","NID_RBC"],0,0);
  }
}

{
  package ETCS::JRU::TargetDistance;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::TargetDistance::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["D_LOA","Q_SCALE"],0,0);
  }
}

{
  package ETCS::JRU::TargetSpeed;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::TargetSpeed::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["V_LOA"],0,0);
  }
}

{
  package ETCS::JRU::ReleaseSpeed;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::ReleaseSpeed::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["V_RELEASE"],0,0);
  }
}

{
  package ETCS::JRU::MRSPSpeed;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::MRSPSpeed::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["V_MRSP"],0,0);
  }
}

{
  package ETCS::JRU::PermittedSpeed;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::PermittedSpeed::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["V_LOA"],0,0);
  }
}

{
  package ETCS::JRU::STMSelected;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::STMSelected::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["NID_STM"],0,0);
  }
}

{
  package ETCS::JRU::StmInformation;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::StmInformation::VERSION="0.2";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["NID_STM","L_MESSAGE_STM","NID_PACKET","L_PACKET"],0,0);
  }
}

{
  package ETCS::JRU::PlainTextMessage;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  BEGIN {
    $ETCS::JRU::PlainTextMessage::VERSION="0.2";
  }
  use strict;
  
  my @msg_decheader = ("Q_TEXTCLASS","Q_TEXTCONFIRM","L_TEXT","X_TEXT");
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,\@msg_decheader,0,0);
  }
  
  sub SetSubset27v224 {
    print "Set JRU::PlainTextMessage to subset 27 v2.2.4\n";
    
    @msg_decheader = ("L_TEXT","X_TEXT");
  }

  sub SetAnsaldo {
    print "Set JRU::PlainTextMessage to Ansaldo\n";
    
    @msg_decheader = ("L_TEXT","X_TEXT");
  }
}

{
  package ETCS::JRU::ModeChange;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  BEGIN {
    $ETCS::JRU::ModeChange::VERSION="0.1";
  }
  use strict;
  
  my @msg_decheader = ("M_MODE","Q_MANUEL_AUTO");
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,\@msg_decheader,0,0);
  }
}

{
  package ETCS::JRU::LevelChange;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  BEGIN {
    $ETCS::JRU::LevelChange::VERSION="0.1";
  }
  use strict;
  
  my @msg_decheader = ("M_LEVEL","NID_STM","Q_MANUEL_AUTO");
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,\@msg_decheader,0,0);
  }
}

{
  package ETCS::JRU::ETCSID;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::ETCSID::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["NID_ETCS_ID"],0,0);
  }
}

{
  package ETCS::JRU::PredefinedTextMessage;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  BEGIN {
    $ETCS::JRU::PredefinedTextMessage::VERSION="0.2";
  }
  use strict;
  
  my @msg_decheader = ("Q_TEXTCLASS","Q_TEXTCONFIRM","Q_TEXT","L_TEXT","X_TEXT");

  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,\@msg_decheader,0,0);
  }
  
  sub SetSubset27v224 {
    print "Set JRU::PredefinedTextMessage to subset 27 v2.2.4\n";
    
    @msg_decheader = ("Q_TEXT");
  }
}

{
  package ETCS::JRU::IndicationsToDriver;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);

  BEGIN {
    $ETCS::JRU::IndicationsToDriver::VERSION="0.2";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["M_INDICATION_6","M_INDICATION_5","M_INDICATION_4","M_INDICATION_3","M_INDICATION_2",
                                      "M_INDICATION_1","V_PERMITTED","V_TARGET","L_TARGET","V_RELEASE","Q_WARNING"],0,0);
  }
}

{
  package ETCS::JRU::Events;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::Events::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["M_EVENTS"],0,0);
  }
}

{
  package ETCS::JRU::BrakeState;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::BrakeState::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["M_BRAKESTATE"],0,0);
  }
}

{
  package ETCS::JRU::JRUState;
  require Exporter;
  use Data::Dumper;
  
  @ISA = qw(ETCS::Structure);
  
  BEGIN {
    $ETCS::JRU::JRUState::VERSION="0.1";
  }
  use strict;
  
  sub new {
    my $self={};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    return $self->process_bits($bits,["JRU_STATE"],0,0);
  }
}

1;
