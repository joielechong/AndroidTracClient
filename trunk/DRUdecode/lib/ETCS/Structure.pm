{
  package ETCS::Structure;
  
  use Exporter;
  use Data::Dumper;
  @ISA = qw(Exporter);
  @EXPORT_OK = qw(%ertms_fields encode_ansaldofield);  
  use strict;
  
  my @msg_trackpackets;
  our %ertms_fields;
  
  BEGIN {
    $ETCS::Structure::VERSION = "0.99";
#
# 0.99 Een heleboel voor de Ansaldo JRU
# 0.96 Q_MEDIA vertaling toegevoegd, Q_DIRLRBG uitgebreid, NID_MN gecorrigeerd
# 0.95 Aangepast voor versie 2.2.4
# 0.94 NID_STM vertaling voor een deel ingevoerd
# 0.93 NID_MN en NID_LTRBG toegevoegd (2.3.0)
# 0.92 D_REF blijkt 2's complement te zijn
# 0.91 Correctie Display_v_target
# 0.9 M_INDICATION* corrected
# 0.8 Q_SCALE afhandeling
#     JRU_DRIVER_ID display
#     Q_LINKORIENTATION display
#     Q_GDIR display
#     M_ACK display
#     M_ADHESION display
#     M_DUP display
#     Q_ASPECT display
#     M_AIRTIGHT display
# 0.7 Q_STATUS display
#     Q_SCALE display
#     Q_LENGTH display
#     Q_LINKREACTION display
#     Q_SUITABILITY display
#     Q_TEXTCLASS display
#     Q_TEXTCONFIRM display
# 0.6 Test voor cvs
#     m_brakestate toegevoegd
#     jru_state toegevoegd
#     foutje in aanroep van process_subfields verbeterd
# 0.5 Moved subfields out of module
#     m_driversactions --> m_driveractions
#     use of soft references in displayValue
#     %ertms_fields from ETCS.pm
#     also produce textline for unknown LRBG in displayValue
#
    $ertms_fields{"A_EMERGENCYDECEL_CAP"}= 8; 
    $ertms_fields{"A_SERVICEDECEL_CAP"} =  8;
    
    $ertms_fields{"D_ADHESION"}		= 15;
    $ertms_fields{"D_AXLELOAD"}		= 15;
    $ertms_fields{"D_CYCLOC"}		= 15;
    $ertms_fields{"D_DP"}		= 15;
    $ertms_fields{"D_EMERGENCYSTOP"}	= 15;
    $ertms_fields{"D_ENDTIMERSTARTLOC"}	= 15;
    $ertms_fields{"D_GRADIENT"}		= 15;
    $ertms_fields{"D_INFILL"}		= 15;
    $ertms_fields{"D_LEVELTR"}		= 15;
    $ertms_fields{"D_LINK"}		= 15;
    $ertms_fields{"D_LOA"}		= 15;
    $ertms_fields{"D_LOC"}		= 15;
    $ertms_fields{"D_LOOP"}		= 15;
    $ertms_fields{"D_LRBG"}		= 15;
    $ertms_fields{"D_MAMODE"}		= 15;
    $ertms_fields{"D_NVOVTRP"}		= 15;
    $ertms_fields{"D_NVPOVTRP"}		= 15;
    $ertms_fields{"D_NVROLL"}		= 15;
    $ertms_fields{"D_NVSTFF"}		= 15;
    $ertms_fields{"D_OL"}		= 15;
    $ertms_fields{"D_POSOFF"}		= 15;
    $ertms_fields{"D_RBCTR"}		= 15;
    $ertms_fields{"D_REF"}		= 16;
    $ertms_fields{"D_REVERSE"}		= 15;
    $ertms_fields{"D_SECTIONTIMERSTOPLOC"}= 15;
    $ertms_fields{"D_SR"}		= 15;
    $ertms_fields{"D_STARTOL"}		= 15;
    $ertms_fields{"D_STARTREVERSE"}	= 15;
    $ertms_fields{"D_STATIC"}		= 15;
    $ertms_fields{"D_SUITABILITY"}	= 15;
    $ertms_fields{"D_TAFDISPLAY"}	= 15;
    $ertms_fields{"D_TEXTDISPLAY"}	= 15;
    $ertms_fields{"D_TRACKINIT"}	= 15;
    $ertms_fields{"D_TRACKCOND"}	= 15;
    $ertms_fields{"D_TRACTION"}		= 15;
    $ertms_fields{"D_TSR"}		= 15;
    $ertms_fields{"D_VALIDNV"}		= 15;
    
    $ertms_fields{"DRU_DAY"}            =  5;
    $ertms_fields{"DRU_HOUR"}           =  5;
    $ertms_fields{"DRU_L_PACKET"}	= 16;
    $ertms_fields{"DRU_L_MESSAGE"}	= 16;
    $ertms_fields{"DRU_M_DIAG"}		= 12;
    $ertms_fields{"DRU_MINUTES"}        =  6;
    $ertms_fields{"DRU_MONTH"}          =  4;
    $ertms_fields{"DRU_NID_CHANNEL"}	=  4;
    $ertms_fields{"DRU_NID_MESSAGE"}    =  8;
    $ertms_fields{"DRU_NID_PACKET"}     =  8;
    $ertms_fields{"DRU_NID_SOURCE"}	=  8;
    $ertms_fields{"DRU_SECONDS"}        =  6;
    $ertms_fields{"DRU_TTS"}            =  5;
    $ertms_fields{"DRU_YEAR"}           =  7;

    $ertms_fields{"G_A"}		=  8;
    $ertms_fields{"G_TSR"}		=  8;
    
    $ertms_fields{"JRU_DAY"}            =  5;
    $ertms_fields{"JRU_DRIVER_ID"}      = 32;
    $ertms_fields{"JRU_DUMMY"}          =  1;
    $ertms_fields{"JRU_HOUR"}           =  5;
    $ertms_fields{"JRU_M_LEVEL"}        =  3;
    $ertms_fields{"JRU_MINUTES"}        =  6;
    $ertms_fields{"JRU_MONTH"}          =  4;
    $ertms_fields{"JRU_NID_MESSAGE"}    =  8;
    $ertms_fields{"JRU_SECONDS"}        =  6;
    $ertms_fields{"JRU_STATE"}          =  1;
    $ertms_fields{"JRU_TTS"}            =  5;
    $ertms_fields{"JRU_YEAR"}           =  7;
    
    $ertms_fields{"L_ACKLEVELTR"}	= 15;
    $ertms_fields{"L_ACKMAMODE"}	= 15;
    $ertms_fields{"L_ADHESION"}		= 15;
    $ertms_fields{"L_AXLELOAD"}		= 15;
    $ertms_fields{"L_DOUBTOVER"}	= 15;
    $ertms_fields{"L_DOUBTUNDER"}	= 15;
    $ertms_fields{"L_ENDSECTION"}	= 15;
    $ertms_fields{"L_LOOP"}		= 15;
    $ertms_fields{"L_MAMODE"}		= 15;
    $ertms_fields{"L_MESSAGE"}		= 10;
    $ertms_fields{"L_MESSAGE_RBC"}	= 10;
    $ertms_fields{"L_MESSAGE_STM"}	= 8;
    $ertms_fields{"L_PACKET"}		= 13;
    $ertms_fields{"L_REVERSEAREA"}	= 15;
    $ertms_fields{"L_SECTION"}		= 15;
    $ertms_fields{"L_TAFDISPLAY"}	= 15;
    $ertms_fields{"L_TARGET"}	        = 15;
    $ertms_fields{"L_TEXT"}		=  8;
    $ertms_fields{"L_TEXTDISPLAY"}	= 15;
    $ertms_fields{"L_TRACKCOND"}	= 15;
    $ertms_fields{"L_TRAIN"}		= 12;
    $ertms_fields{"L_TRAININT"}		= 15;
    $ertms_fields{"L_TSR"}		= 15;
    
    $ertms_fields{"M_ACK"}		=  1;
    $ertms_fields{"M_ADHESION"}		=  1;
    $ertms_fields{"M_AIRTIGHT"}		=  2;
    $ertms_fields{"M_AXLELOAD"}		=  7;
    $ertms_fields{"M_BRAKESTATE"}	=  1;
    $ertms_fields{"M_BRAKE_STATE"}	=  1;
    $ertms_fields{"M_DRIVERACTIONS"}	=  8;
    $ertms_fields{"M_DUP"}		=  2;
    $ertms_fields{"M_ERROR"}		=  8;
    $ertms_fields{"M_EVENTS"}		=  8;
    $ertms_fields{"M_INDICATION_1"}     =  1;
    $ertms_fields{"M_INDICATION_2"}     =  1;
    $ertms_fields{"M_INDICATION_3"}     =  1;
    $ertms_fields{"M_INDICATION_4"}     =  1;
    $ertms_fields{"M_INDICATION_5"}     =  1;
    $ertms_fields{"M_INDICATION_6"}     =  3;
    $ertms_fields{"M_LEVEL"}		=  3;
    $ertms_fields{"M_LEVELTEXTDISPLAY"}	=  3;
    $ertms_fields{"M_LEVELTR"}		=  3;
    $ertms_fields{"M_LEVEL_TR"}		=  3;
    $ertms_fields{"M_LOADINGGAUGE"}	=  8;
    $ertms_fields{"M_LOC"}		=  3;
    $ertms_fields{"M_MAMODE"}		=  2;
    $ertms_fields{"M_MCOUNT"}		=  8;
    $ertms_fields{"M_MODE"}		=  4;
    $ertms_fields{"M_MODETEXTDISPLAY"}	=  4;
    $ertms_fields{"M_NVCONTACT"}	=  2;
    $ertms_fields{"M_NVDERUN"}		=  1;
    $ertms_fields{"M_POSITION"}		= 20;
    $ertms_fields{"M_TRACKCOND"}	=  4;
    $ertms_fields{"M_TRACTION"}		=  8;
    $ertms_fields{"M_VERSION"}		=  7;
    
    $ertms_fields{"N_EMERGENCY_SECTIONS"}=  3;
    $ertms_fields{"N_SERVICE_SECTIONS"} =  3;
    $ertms_fields{"N_ITER"}		=  5;
    $ertms_fields{"N_PIG"}		=  3;
    $ertms_fields{"N_TOTAL"}		=  3;
    
    $ertms_fields{"NC_DIFF"}		=  4;
    $ertms_fields{"NC_TRAIN"}		= 15;
    
    $ertms_fields{"NID_BG"}		= 14;
    $ertms_fields{"NID_C"}		= 10;
    $ertms_fields{"NID_EM"}		=  4;
    $ertms_fields{"NID_ENGINE"}		= 24;
    $ertms_fields{"NID_ETCS_ID"}	= 24;
    $ertms_fields{"NID_LOOP"}		= 14;
    $ertms_fields{"NID_LRBG"}		= 24;
    $ertms_fields{"NID_LTRBG"}		= 24;
    $ertms_fields{"NID_MESSAGE"}	=  8;
    $ertms_fields{"NID_MN"}     	= 24;
    $ertms_fields{"NID_OPERATIONAL"}	= 32;
    $ertms_fields{"NID_PACKET"}		=  8;
    $ertms_fields{"NID_PRVBG"}		= 24;
    $ertms_fields{"NID_RADIO"}		= 64;
    $ertms_fields{"NID_RADIO_HIGH"}	= 32;
    $ertms_fields{"NID_RADIO_LOW"}	= 32;
    $ertms_fields{"NID_RBC"}		= 14;
    $ertms_fields{"NID_RIU"}		= 14;
    $ertms_fields{"NID_STM"}		=  8;
    $ertms_fields{"NID_TSR"}		=  8;
    $ertms_fields{"NID_XUSER"}		=  9;

    $ertms_fields{"PADDING_T"}          =  2;

    $ertms_fields{"Q_ASPECT"}		=  1;
    $ertms_fields{"Q_DANGERPOINT"}	=  1;
    $ertms_fields{"Q_DIR"}		=  2;
    $ertms_fields{"Q_DIRLRBG"}		=  2;
    $ertms_fields{"Q_DIRTRAIN"}		=  2;
    $ertms_fields{"Q_DLRBG"}		=  2;
    $ertms_fields{"Q_EMERGENCYSTOP"}	=  2;
    $ertms_fields{"Q_ENDTIMER"}		=  1;
    $ertms_fields{"Q_FRONT"}		=  1;
    $ertms_fields{"Q_GDIR"}		=  1;
    $ertms_fields{"Q_INFILL"}		=  1;
    $ertms_fields{"Q_LENGTH"}		=  2;
    $ertms_fields{"Q_LGTLOC"}		=  1;
    $ertms_fields{"Q_LINK"}		=  1;
    $ertms_fields{"Q_LINKACC"}		=  6;
    $ertms_fields{"Q_LINKORIENTATION"}	=  1;
    $ertms_fields{"Q_LINKREACTION"}	=  2;
    $ertms_fields{"Q_LOCACC"}		=  6;
    $ertms_fields{"Q_LOOPDIR"}		=  1;
    $ertms_fields{"Q_MANUEL_AUTO"}      =  2;
    $ertms_fields{"Q_MEDIA"}		=  1;
    $ertms_fields{"Q_MPOSITION"}	=  1;
    $ertms_fields{"Q_NEWCOUNTRY"}	=  1;
    $ertms_fields{"Q_NVDRIVER_ADHES"}	=  1;
    $ertms_fields{"Q_NVEMRRLS"}		=  1;
    $ertms_fields{"Q_NVSRBKTRG"}	=  1;
    $ertms_fields{"Q_ORIENTATION"}	=  1;
    $ertms_fields{"Q_OVERLAP"}		=  1;
    $ertms_fields{"Q_RBC"}		=  1;
    $ertms_fields{"Q_RIU"}		=  1;
    $ertms_fields{"Q_SCALE"}		=  2;
    $ertms_fields{"Q_SECTIONTIMER"}	=  1;
    $ertms_fields{"Q_SLEEPSESSION"}	=  1;
    $ertms_fields{"Q_SRSTOP"}		=  1;
    $ertms_fields{"Q_SSCODE"}		=  4;
    $ertms_fields{"Q_STATUS"}		=  2;
    $ertms_fields{"Q_SUITABILITY"}	=  2;
    $ertms_fields{"Q_TEXT"}		=  8;
    $ertms_fields{"Q_TEXTCLASS"}	=  2;
    $ertms_fields{"Q_TEXTCONFIRM"}	=  2;
    $ertms_fields{"Q_TEXTDISPLAY"}	=  1;
    $ertms_fields{"Q_TRACKDEL"}		=  1;
    $ertms_fields{"Q_TRACKINIT"}	=  1;
    $ertms_fields{"Q_UPDOWN"}		=  1;
    $ertms_fields{"Q_WARNING"}		=  1;
    
    $ertms_fields{"T_CUT_OFF"}          =  8;
    $ertms_fields{"T_CYCLOC"}		=  8;
    $ertms_fields{"T_CYCRQST"}		=  8;
    $ertms_fields{"T_DELAY"}            =  9;
    $ertms_fields{"T_ENDTIMER"}		= 10;
    $ertms_fields{"T_LOA"}		= 10;
    $ertms_fields{"T_MAR"}		=  8;
    $ertms_fields{"T_NVCONTACT"}	=  8;
    $ertms_fields{"T_NVOVTRP"}		=  8;
    $ertms_fields{"T_OL"}		= 10;
    $ertms_fields{"T_SECTIONTIMER"}	= 10;
    $ertms_fields{"T_TEXTDISPLAY"}	= 10;
    $ertms_fields{"T_TIMEOUTRQST"}	= 10;
    $ertms_fields{"T_TRAIN"}		= 32;
   
    $ertms_fields{"V_AXLELOAD"}		=  7;
    $ertms_fields{"V_DIFF"}		=  7;
    $ertms_fields{"V_EMERGENCYDECEL_CAP"}=10;
    $ertms_fields{"V_SERVICEDECEL_CAP"} = 10;
    $ertms_fields{"V_LOA"}		=  7;
    $ertms_fields{"V_MAIN"}		=  7;
    $ertms_fields{"V_MAMODE"}		=  7;
    $ertms_fields{"V_MAXTRAIN"}		=  7;
    $ertms_fields{"V_MRSP"}		=  7;
    $ertms_fields{"V_NVALLOWOVTRP"}	=  7;
    $ertms_fields{"V_NVONSIGHT"}	=  7;
    $ertms_fields{"V_NVREL"}		=  7;
    $ertms_fields{"V_NVSHUNT"}		=  7;
    $ertms_fields{"V_NVSTFF"}		=  7;
    $ertms_fields{"V_NVSUPOVTRP"}	=  7;
    $ertms_fields{"V_NVUNFIT"}		=  7;
    $ertms_fields{"V_PERMITTED"}	=  7;
    $ertms_fields{"V_RELEASE"}  	=  7;
    $ertms_fields{"V_RELEASEDP"}	=  7;
    $ertms_fields{"V_RELEASEOL"}	=  7;
    $ertms_fields{"V_REVERSEP"}		=  7;
    $ertms_fields{"V_SERVICEDECEL_CAP"} = 10;
    $ertms_fields{"V_STATIC"}		=  7;
    $ertms_fields{"V_TARGET"}	        =  7;
    $ertms_fields{"V_TRAIN"}		=  7;
    $ertms_fields{"V_TSR"}		=  7;
    
    $ertms_fields{"X_TEXT"}		=  8;
    $ertms_fields{"XUSER_DATA"}		=  8;

    $ertms_fields{"*977"}		=  0;
    $ertms_fields{"*978"}		=  0;
    $ertms_fields{"*979"}		=  0;
    $ertms_fields{"*980"}		=  0;
    $ertms_fields{"*981"}		=  0;
    $ertms_fields{"*982"}		=  0;
    $ertms_fields{"*983"}		=  0;
    $ertms_fields{"*984"}		=  0;
    $ertms_fields{"*985"}		=  0;
    $ertms_fields{"*986"}		=  0;
    $ertms_fields{"*987"}		=  0;
    $ertms_fields{"*988"}		=  0;
    $ertms_fields{"*989"}		=  0;
    $ertms_fields{"*990"}		=  0;
    $ertms_fields{"*991"}		=  0;
    $ertms_fields{"*992"}		=  0;
    $ertms_fields{"*993"}		=  0;
    $ertms_fields{"*994"}		=  0;
    $ertms_fields{"*995"}		=  0;
    $ertms_fields{"*996"}		=  0;
    $ertms_fields{"*997"}		=  0;
    $ertms_fields{"*998"}		=  0;
    $ertms_fields{"*999"}		=  0;
  }
  
  my @q_scaleval = (0.1,1.0,10.0,undef);
  my $scale = undef;
  
  sub new {
    my $self= {};
    $self->{Fields}=[];
    $self->{nrfields}=0;
    $self->{currentfield}=undef;
    bless $self;
    return $self;
  }

  sub addField {
    my ($self,$field,$bitstream) = @_;
    
    $self->{currentfield} = $self->{nrfields}++;
    $self->{Fields}->[$self->{currentfield}]->{Fieldseq}=$self->{currentfield};
    $self->{Fields}->[$self->{currentfield}]->{Field}=$field;
    $self->{Fields}->[$self->{currentfield}]->{Bits}=$bitstream;
    return $bitstream;
  }
  
  sub setIter {
    my ($self,$field,$iter) = @_;
    
    $self->{Fields}->[$self->{currentfield}]->{Iteration}=$iter;
    return $iter;
  }
  
  sub setFieldDecimal {
    my ($self,$field,$value) = @_;
    
    $self->{Fields}->[$self->{currentfield}]->{Decimal}=$value;
    print " $value" if $ETCS::Debug::Log;
    return $value;
  }

  sub setFieldText {
    my ($self,$field,$value) = @_;
    
    $self->{Fields}->[$self->{currentfield}]->{Text}=$value;
    print " $value" if $ETCS::Debug::Log;
    return $value;
  }

  sub setFieldHex {
    my ($self,$field,$value) = @_;
    
    $self->{Fields}->[$self->{currentfield}]->{Hex}=$value;
    print " $value" if $ETCS::Debug::Log;
    return $value;
  }
  
  sub addPacket {
    my ($self,$nrpack) = @_;
    
    if ($nrpack == 0) {
      $self->{Packets} = [];
    }
    my $pack = ETCS::Packet::new();
    $self->{Packets}->[$nrpack] = $pack;
    $pack->{Packetnr} = $nrpack;
    return $pack;
  }
  
  sub process_bits {
    my ($pack,$msg,$msg_def,$iterdepth,$outeriter) = @_;
    $outeriter = 0 if $iterdepth==0;
    
    my $bitpos    = 0;
    my $skipnext  = 0;
    my $skip2nd   = 0;
    my $skip3rd   = 0;
    my $repcnt    = 1;
    my $itercnt   = 1;
    
    if (!defined($msg_def)) {
      print "Geen definitie voor dit bericht: $msg\n";
      my ($package, $filename, $line) = caller;
      print "Aangeroepen uit $package, $filename, $line\n";
      return -1;
    }
    foreach my $field (@$msg_def) {
      if (!defined($ertms_fields{$field})) {
        print "Veld $field niet gedefinieerd\n";
        print Dumper(@$msg_def);
        print "  msg_def = ",join(",",@$msg_def),"\n";
    	my ($package, $filename, $line) = caller;
        print "Aangeroepen uit $package, $filename, $line\n";
        getc();
      }
      my $bitlen = $ertms_fields{$field} * $repcnt;
      if ($field eq "XUSER_DATA") {
        $bitlen = length($msg) - $bitpos;
      }
      my $bitstream;
      my $intval;
      for (my $iter=1; $iter <= $itercnt; $iter++) {
        if ($skipnext == 0) {
          if ($field =~/^\*(\d+)/) {
	    my $dummsg = $1;
            $bitlen = $pack->process_subfields(substr($msg,$bitpos),$dummsg,$iterdepth,$iter);
            if ($bitlen < 0) {
              my ($package, $filename, $line) = caller;
              print "Aangeroepen uit $package, $filename, $line\n";
              return $bitlen;
            }
	    $bitpos += $bitlen;
          } else {
	    print $field,":  " if $ETCS::Debug::Log;
	    if (!defined($ertms_fields{$field})) {
              print "Veld niet gedefinieerd $field\n";
	      getc();
	    }
	    if ($bitpos > length($msg)) {
	    	print "Decodeer fout in msg:\n$msg\n$field = $field\nbitpos = $bitpos\nLengte = ".length($msg)."\n";
	    	getc();
 	    }
	    $bitstream = substr($msg,$bitpos,$bitlen);
	    print $bitstream if $ETCS::Debug::Log;
            addField($pack,$field,$bitstream);
            setIter($pack,$field,$outeriter) if $outeriter > 0;
            $pack->displayValue($field,$bitstream);
            $intval = $pack->{Fields}->[$pack->{currentfield}]->{Decimal}; # gevaarlijk want Decimal wordt niet altijd gezet.
	    if ($field eq "Q_SCALE") {
              $scale = $q_scaleval[$intval];
	    }
	    $bitpos += $bitlen;
            print "\n" if $ETCS::Debug::Log;
	  }
	}
        
        $skipnext = $skip2nd;
        $skip2nd  = $skip3rd;
        $skip3rd  = 0;
        
	if ((($field eq "M_LEVELTEXTDISPLAY") ||
             ($field eq "M_LEVELTR") ||
             ($field eq "M_LEVEL_TR") ||
	     ($field eq "M_LEVEL")) && ($bitstream ne "001")) {
          $skipnext = 1;
	} elsif (($field eq "Q_LENGTH") &&(($bitstream eq "00") || ($bitstream eq "11"))) {
	  $skipnext = 1;
	} elsif ((($field eq "Q_SECTIONTIMER") ||
		  ($field eq "Q_ENDTIMER") ||
		  ($field eq "Q_DANGERPOINT") ||
		  ($field eq "Q_NEWCOUNTRY") ||
		  ($field eq "Q_OVERLAP"))&& ($bitstream eq "0")) {
	  $skipnext = 1;
	} elsif ($field eq "Q_TRACKINIT") {
          if ($bitstream eq "0") {
            $skipnext=1;
          } else {
            $skip2nd=1;
          }
        } elsif ($field eq "Q_SUITABILITY") {
          if ($bitstream eq "00") {
            $skip2nd = 1;
            $skip3rd = 1;
          } elsif ($bitstream eq "01") {
            $skipnext = 1;
            $skip3rd = 1;
          } else { #eigenlijk zou hier alleen op 10 getest moeten worden
            $skipnext=1;
            $skip2nd = 1;
          }
        }
	    
	if ($field eq "L_TEXT") {
	  $repcnt=$intval;
	} else {
	  $repcnt = 1;
	}  
      }
      if (($field eq "N_ITER") || ($field eq "N_SERVICE_SECTIONS") || ($field eq "N_EMERGENCY_SECTIONS")) {
	$itercnt=$intval;
        $iterdepth++;
      } else {
        $itercnt = 1;
        $iterdepth-- if $iterdepth > 0;
      }
    }	
    print "\n" if $ETCS::Debug::Log;
    return $bitpos;
  }

  our @dru_nid_source;
  our @jru_state;
  our @m_ack;
  our @m_adhesion;
  our @m_airtight;
  our @m_brakestate;
  our @m_driveractions;
  our @m_dup;
  our @m_error;
  our @m_events;
  our @m_indication_1;
  our @m_indication_2;
  our @m_indication_3;
  our @m_indication_4;
  our @m_indication_5;
  our @m_level;
  our @m_leveltextdisplay;
  our @m_mamode;
  our @m_mcount;
  our @m_mode;
  our @m_modetextdisplay;
  our @m_trackcond;
  our @m_version;
  our @nc_train;
  our @n_pig;
  our @n_total;
  our @nid_stm;
  our @q_aspect;
  our @q_dir;
  our @q_dirlrbg;
  our @q_gdir;
  our @q_length;
  our @q_link;
  our @q_linkreaction;
  our @q_manuel_auto;
  our @q_media;
  our @q_scale;
  our @q_sectiontimer;
  our @q_sleepsession;
  our @q_srstop;
  our @q_status;
  our @q_suitability;
  our @q_text;
  our @q_textclass;
  our @q_textconfirm;
  our @q_trackdel;
  our @q_trackinit;
  our @q_updown;
  
  my %vertaal;
  
  BEGIN {
    $vertaal{"JRU_M_LEVEL"} = "m_level";
    $vertaal{"M_LEVELTR"} = "m_level";
    $vertaal{"M_NVCONTACT"} = "q_linkreaction";
    $vertaal{"Q_DIRTRAIN"} = "q_dirlrbg";
    $vertaal{"Q_DLRBG"} = "q_dirlrbg";
    $vertaal{"Q_LINKORIENTATION"} = "q_dir";
    $vertaal{"V_RELEASEDP"} = "v_release";
    $vertaal{"V_RELEASEOL"} = "v_release";
    $vertaal{"V_PERMITTED"} = "v_target";
    $vertaal{"NID_MN"} = "nid_operational";
    $vertaal{"M_BRAKE_STATE"} = "m_brakestate";
    
    $m_version[16] = "Class 1";
    
    $m_mcount[255] = "The telegram fits with all telegrams of the same balise group";
    
    $nc_train[0] = "Basic static speed profile";
        
    $n_pig[0] = 'I am the 1st';
    $n_pig[1] = 'I am the 2nd';
    $n_pig[2] = 'I am the 3rd';
    $n_pig[3] = 'I am the 4th';
    $n_pig[4] = 'I am the 5th';
    $n_pig[5] = 'I am the 6th';
    $n_pig[6] = 'I am the 7th';
    $n_pig[7] = 'I am the 8th';
    
    $n_total[0] = '1 balise in the group';
    $n_total[1] = '2 balise in the group';
    $n_total[2] = '3 balise in the group';
    $n_total[3] = '4 balise in the group';
    $n_total[4] = '5 balise in the group';
    $n_total[5] = '6 balise in the group';
    $n_total[6] = '7 balise in the group';
    $n_total[7] = '8 balise in the group';
    
    $q_link[0] = 'Unlinked';
    $q_link[1] = 'Linked';
    
    $q_aspect[0] = "Stop if in SH";
    $q_aspect[1] = "Go if in SH";
    
    $q_gdir[0] = "downhill";
    $q_gdir[1] = "uphill";
    
    $m_ack[0] = "No acknowledgment required";
    $m_ack[1] = "Acknowledgment required";
    
    $m_airtight[0] = "Not fitted";
    $m_airtight[1] = "Fitted";
    $m_airtight[2] = "Unknown";
    $m_airtight[3] = "Spare";
    
    $m_adhesion[0] = "70%";
    $m_adhesion[1] = "100%";

    $vertaal{"Duplicate of the next balise"} = "This balise is a duplicate of the next balise";
    $vertaal{"Duplicate of the previous balise"} = "This balise is a duplicate of the previous balise";

    $m_dup[0] = "No duplicates";
    $m_dup[1] = "This balise is a duplicate of the next balise";
    $m_dup[2] = "This balise is a duplicate of the previous balise";
    $m_dup[3] = "Spare";
    
    $q_length[0] = "No train integrity information available";
    $q_length[1] = "Train integrity confirmed by integrity monitoring device";
    $q_length[2] = "Train integrity confirmed by driver";
    $q_length[3] = "Train integrity lost";
    
    $q_srstop[0] = "Stop if in SR mode";
    $q_srstop[1] = "Go if in SR mode";

    $q_textclass[0] = "Auxiliary information";
    $q_textclass[1] = "Important information";
    $q_textclass[2] = "Spare (2)";
    $q_textclass[3] = "Spare (3)";
    
    $q_textconfirm[0] = "No confirmation required";
    $q_textconfirm[1] = "Continue display until confirmed";
    $q_textconfirm[2] = "Apply service brake if not confirmed when end conditions reached";
    $q_textconfirm[3] = "Spare";
    
    $q_linkreaction[0] = "Train trip";
    $q_linkreaction[1] = "Apply service brake";
    $q_linkreaction[2] = "No reaction";
    $q_linkreaction[3] = "Spare";
    
    $q_suitability[0] = "Loading gauge profile";
    $q_suitability[1] = "Max axle load";
    $q_suitability[2] = "Traction power";
    $q_suitability[3] = "Spare";
    
    $q_scale[0] = "10cm";
    $q_scale[1] = "1m";
    $q_scale[2] = "10m";
    $q_scale[3] = "Spare";
    
    $q_status[0] = "Invalid";
    $q_status[1] = "Valid";
    $q_status[2] = "Unknown";
    $q_status[3] = "Spare";
    
    $q_dir[0] = 'Reverse';
    $q_dir[1] = 'Nominal';
    $q_dir[2] = 'Both directions';
    $q_dir[3] = 'Spare';
    
    $q_dirlrbg[0] = 'Reverse';
    $q_dirlrbg[1] = 'Nominal';
    $q_dirlrbg[2] = 'Unknown';
    $q_dirlrbg[3] = 'Spare';
    
    $q_media[0] = 'Balise';
    $q_media[1] = 'Loop';
    
    $q_sectiontimer[0] = "No Section Timer information";
    $q_sectiontimer[1] = "Section Timer information to follow";    
    
    $q_sleepsession[0] = "Ignore session management information";
    $q_sleepsession[1] = "Consider session management information";    
    
    $q_text[1] = "Emergency brake command error";
    $q_text[2] = "Emergency brake release error";
    $q_text[3] = "Pneumatic insertion error";
    $q_text[4] = "Service brake command error";
    $q_text[5] = "Service brake release error";
    $q_text[6] = "Traction cut off error";
    $q_text[7] = "EXTERNAL SPEED DISPLAY ERROR";
    $q_text[8] = "Train at Kp";
    $q_text[9] = "Route unsuitabitility : loading gauge";
    $q_text[10] = "Route unsuitabitility : power supply";
    $q_text[11] = "Route unsuitabitility : axle load";
    $q_text[12] = "Emergency brake test are busy";
    $q_text[13] = "Emergency brake test aborted";
    $q_text[14] = "Emergency brake test are OK";
    $q_text[15] = "Emergency brake test are not OK";
    $q_text[16] = "Stopping is not permitted : Tunnel";
    $q_text[17] = "Stopping not permitted : Bridge";
    $q_text[18] = "Stopping not permitted : other reason";
    $q_text[19] = "Unconditional emergency stop";
    $q_text[20] = "conditional emergency stop";
    $q_text[21] = "Shunting request send";
    $q_text[22] = "Shunting refused by RBC";
    $q_text[23] = "Shunting request not responded";
    $q_text[24] = "Shunting not required";
    $q_text[25] = "Eurobalise version not compatible";
    $q_text[26] = "Balise group data inconsistency";
    $q_text[27] = "Linking Inconsistency";
    $q_text[28] = "Roll away protection";
    $q_text[29] = "Reverse movement protection";
    $q_text[30] = "Standstill supervision";
    $q_text[31] = "Reversing Permitted";
    $q_text[32] = "Level 0 announced";
    $q_text[33] = "Level STM announced";
    $q_text[34] = "Level 1 announced";
    $q_text[35] = "Level 2 announced";
    $q_text[36] = "Level 3 announced";
    $q_text[37] = "Entry in Full Supervision";
    $q_text[38] = "Entry in On-Sight";
    $q_text[39] = "Stop in SH information received";
    $q_text[40] = "Stop in SR Information received";
    $q_text[41] = "Unexpected balise in Shunting";
    $q_text[42] = "Unexpected balise in SR";
    $q_text[43] = "Over passing distance to run in RV";
    $q_text[44] = "Over passing distance to run in PT";
    $q_text[45] = "RBC version not compatible";
    $q_text[46] = "Radio link supervision error";
    $q_text[47] = "Train has been rejected by RBC";
    $q_text[48] = "Wait for driver selection entry";
    $q_text[49] = "Selection not more available";
    $q_text[50] = "No track condition will be received";
    $q_text[51] = "Communication lost with STM";
    $q_text[52] = "EVC not compatible with STM";
    $q_text[53] = "Shutdown of STM";
    $q_text[54] = "Failure of STM";
    $q_text[55] = "Failure of DRU";
    $q_text[56] = "Failure of JRU";
    $q_text[57] = "Radio total failure";
    $q_text[58] = "Power-up tests on-going";
    $q_text[59] = "Power-up tests Failled";
    $q_text[60] = "Power-up tests successful";
    $q_text[61] = "Power-up test success low availability";
    $q_text[62] = "Overspeed Emergency brake applied";
    $q_text[63] = "Overspeed Service brake applied";
    $q_text[64] = "ACK Train Trip";
    $q_text[65] = "ACK transition to level 0";
    $q_text[66] = "ACK Transition to level STM";
    $q_text[67] = "ACK Transition to level 1";
    $q_text[68] = "ACK Transition to level 2";
    $q_text[69] = "ACK transition to level 3";
    $q_text[71] = "ACK release emergency brake";
    $q_text[72] = "ACK Reversing mode";
    $q_text[73] = "ACK Unfitted mode";
    $q_text[74] = "ACK Staff responsible";
    $q_text[75] = "ACK On-sight mode";
    $q_text[76] = "ACK shunting mode";
    $q_text[77] = "ACK SN mode";
    $q_text[78] = "Tilting Problem";
    $q_text[79] = "TILTING PROBLEM ACK";
    $q_text[80] = "External System Failure";
    $q_text[81] = "ACK SE Mode";
    $q_text[82] = "Driver Level or Override selection await";
    $q_text[83] = "Driver start selection awaited";
    $q_text[84] = "Driver PT selection awaited";
    $q_text[85] = "TRU failure";
    $q_text[86] = "Air Tightness Failure";
    $q_text[87] = "Passenger em. brake inhibition failure";
    $q_text[88] = "SPECIFIC DATA ENTRY";
    $q_text[89] = "WAITING FOR STM DATA";
    $q_text[90] = "Emergency brake tests partially KO";
    $q_text[91] = "Com. term. not received after RBC area";
    $q_text[92] = "No detection of two consecutive BGS";
    $q_text[93] = "Cross talking error";
    $q_text[94] = "Wait session termination";
    $q_text[95] = "EVC external low availability";
    $q_text[96] = "STM data view in preparation";
    $q_text[97] = "Radio message sequence error";
    $q_text[98] = "Radio message decoding error";
    $q_text[99] = "Unknown LRBG reference sent by RBC";
    $q_text[100] = "Balise message decoding error";
    $q_text[101] = "Invalid Driver identity";
    $q_text[102] = "Invalid Train number";
    $q_text[103] = "Invalid Level";
    $q_text[104] = "Invalid Level STM";
    $q_text[105] = "IO1 Monitoring Error";
    $q_text[106] = "IO2 Monitoring Error";
    $q_text[107] = "IO3 Monitoring Error";
    $q_text[108] = "IO4 Monitoring Error";
    $q_text[109] = "IO5 Monitoring Error";
    $q_text[110] = "IO6 Monitoring Error";
    $q_text[111] = "IO7 Monitoring Error";
    $q_text[112] = "IO8 Monitoring Error";
    $q_text[113] = "IO9 Monitoring Error";
    $q_text[114] = "IO10 Monitoring Error";
    $q_text[115] = "IO11 Monitoring Error";
    $q_text[116] = "IO12 Monitoring Error";
    $q_text[117] = "IO13 Monitoring Error";
    $q_text[118] = "IO14 Monitoring Error";
    $q_text[119] = "IO15 Monitoring Error";
    $q_text[120] = "IO16 Monitoring Error";
    $q_text[121] = "IO17 Monitoring Error";
    $q_text[122] = "IO18 Monitoring Error";
    $q_text[123] = "IO19 Monitoring Error";
    $q_text[124] = "IO20 Monitoring Error";
    $q_text[125] = "IO21 Monitoring Error";
    $q_text[126] = "IO22 Monitoring Error";
    $q_text[127] = "IO23 Monitoring Error";
    $q_text[128] = "IO24 Monitoring Error";
    $q_text[129] = "IO25 Monitoring Error";
    $q_text[130] = "IO26 Monitoring Error";
    $q_text[131] = "IO27 Monitoring Error";
    $q_text[132] = "IO28 Monitoring Error";
    $q_text[133] = "IO29 Monitoring Error";
    $q_text[134] = "IO30 Monitoring Error";
    $q_text[135] = "IO31 Monitoring Error";
    $q_text[136] = "Nbr of hours before an EVC reset :";
    $q_text[137] = "From now on, the EVC must be reset";
    $q_text[138] = "Entry of maintenance parameters is mandatory";
    
    $q_trackdel[0] = "No track description deleted";
    $q_trackdel[1] = "Track description deleted";
    
    $q_trackinit[0] = "No Initial states to be resumed, profile to follow";
    $q_trackinit[1] = "Empty profile, initial states to be resumed";
    
    $m_error[0] = "Balise consistency: linking";
    $m_error[1] = "Balise consistency: message error";
    $m_error[2] = "Balise consistency: unlinked group";
    $m_error[3] = "Radio consistency: message error";
    $m_error[4] = "Radio consistency: sequence";
    $m_error[5] = "Radio consistency: radio link";
    $m_error[6] = "No fatal error";
    $m_error[7] = "Fatal error (equipment in SL or NL mode)";
    
    $m_events[0] = "Not used";
    $m_events[1] = "Service brake failure";
    $m_events[2] = "Standstill supervision intervention";
    $m_events[3] = "Roll away supervision intervention";
    $m_events[4] = "Reverse movement supervision";
    $m_events[5] = "Override EOA";
    $m_events[6] = "Balise transmission error";
    $m_events[7] = "Radio transmission error";
    $m_events[8] = "Loop transmission error";
    $m_events[9] = "Odometer failure";
    $m_events[10] = "Passenger brake application";
    $m_events[11] = "Version incompatibility with RBC";
    $m_events[12] = "STM failure";

    $vertaal{"Level STM"} = "Level STM as indicated by NID_STM";
    
    $m_level[0] = "Level 0";
    $m_level[1] = "Level STM as indicated by NID_STM";
    $m_level[2] = "Level 1";
    $m_level[3] = "Level 2";
    $m_level[4] = "Level 3";
    $m_level[5] = "Spare (5)";
    $m_level[6] = "Spare (6)";
    $m_level[7] = "Spare (7)";

    $m_leveltextdisplay[0] = "Level 0";
    $m_leveltextdisplay[1] = "Level STM as indicated by NID_STM";
    $m_leveltextdisplay[2] = "Level 1";
    $m_leveltextdisplay[3] = "Level 2";
    $m_leveltextdisplay[4] = "Level 3";
    $m_leveltextdisplay[5] = "The display of the text shall not be limited by the level";
    $m_leveltextdisplay[6] = "Spare (6)";
    $m_leveltextdisplay[7] = "Spare (7)";

    $vertaal{"Stand by"} = "Stand By";
        
    $m_mamode[0] = "On Sight";
    $m_mamode[1] = "Shunting";
    
    $m_mode[0] = "Full Supervision";
    $m_mode[1] = "On Sight";
    $m_mode[2] = "Staff responsible";
    $m_mode[3] = "Shunting";
    $m_mode[4] = "Unfitted";
    $m_mode[5] = "Sleeping";
    $m_mode[6] = "Stand By";
    $m_mode[7] = "Trip";
    $m_mode[8] = "Post Trip";
    $m_mode[9] = "System Failure";
    $m_mode[10] = "Isolation";
    $m_mode[11] = "Non Leading";
    $m_mode[12] = "STM European";
    $m_mode[13] = "STM National";
    $m_mode[14] = "Reversing";
    $m_mode[15] = "Spare (15)";
    
    $m_modetextdisplay[0] = "Full Supervision";
    $m_modetextdisplay[1] = "On Sight";
    $m_modetextdisplay[2] = "Staff responsible";
    $m_modetextdisplay[3] = "Shunting";
    $m_modetextdisplay[4] = "Unfitted";
    $m_modetextdisplay[5] = "Sleeping";
    $m_modetextdisplay[6] = "Stand By";
    $m_modetextdisplay[7] = "Trip";
    $m_modetextdisplay[8] = "Post Trip";
    $m_modetextdisplay[9] = "System Failure";
    $m_modetextdisplay[10] = "Isolation";
    $m_modetextdisplay[11] = "Non Leading";
    $m_modetextdisplay[12] = "STM European";
    $m_modetextdisplay[13] = "STM National";
    $m_modetextdisplay[14] = "Reversing";
    $m_modetextdisplay[15] = "The display of the text shall not be limited by the mode";
    
    $m_trackcond[0] = "Non stopping area - tunnel. Initial state: stopping permitted (no tunnel)";
    $m_trackcond[1] = "Non stopping area - bridge. Initial state: stopping permitted (no bridge)";
    $m_trackcond[2] = "Non stopping area - other reasons. Initial state: stopping permitted";
    $m_trackcond[3] = "Powerless section - lower pantograph. Initial state: not powerless section";
    $m_trackcond[4] = "Radio hole (stop supervising T_NVCONTACT). Initial state: supervise T_NVCONTACT";
    $m_trackcond[5] = "Air tightness. Initial state: no request for air tightness";
    $m_trackcond[6] = "Switch off regenerative brake. Initial state: regenerative brake on";
    $m_trackcond[7] = "Switch off eddy current brake for service brake. Initial state: eddy current brake for service brake on";
    $m_trackcond[8] = "Switch off magnetic shoe brake. Initial state: magnetic shoe brake on";
    $m_trackcond[9] = "Powerless section - switch off the main power switch. Initial state: not powerless section";
    
    $dru_nid_source[1] = "EVC";
    $dru_nid_source[2] = "EVC_CORE";
    $dru_nid_source[3] = "EVC_TIU";
    $dru_nid_source[4] = "DMI";
    $dru_nid_source[5] = "EIRENE";
    $dru_nid_source[6] = "TRU";
    $dru_nid_source[7] = "EVC_MM";    

    $m_driveractions[0] = "Ack of on sight";
    $m_driveractions[1] = "Ack of full supervision";
    $m_driveractions[2] = "Ack of shunting";
    $m_driveractions[3] = "Ack of trip";
    $m_driveractions[4] = "Ack of staff responsible";
    $m_driveractions[5] = "Ack of unfitted";
    $m_driveractions[6] = "Ack of reversing";
    $m_driveractions[7] = "Ack Level 0";
    $m_driveractions[8] = "Ack Level 1";
    $m_driveractions[9] = "Ack Level 2";
    $m_driveractions[10] = "Ack Level 3";
    $m_driveractions[11] = "Ack Level STM";
    $m_driveractions[12] = "Shunting selected";
    $m_driveractions[13] = "Non Leading selected";
    $m_driveractions[14] = "Reversing selected";
    $m_driveractions[15] = "Override EOA selected";
    $m_driveractions[16] = "Override route suitability";
    $m_driveractions[17] = "Emergency brakes release";
    $m_driveractions[18] = "Exit of shunting";
    $m_driveractions[19] = "Exit of Non Leading";
    $m_driveractions[20] = "Start of mission";
    $m_driveractions[21] = "Data Entry requested";
    $m_driveractions[22] = "Driver confirmation of data";
    $m_driveractions[23] = "Confirm track ahead free";
    $m_driveractions[24] = "Ack of Plain Text Messages";
    $m_driveractions[25] = "Ack of Fixed Text Messages";
    $m_driveractions[26] = "Open door";
    $m_driveractions[27] = "Close door";
    $m_driveractions[28] = "Pantograph up";
    $m_driveractions[29] = "Pantograph down";
    $m_driveractions[30] = "Switch off main power";
    $m_driveractions[31] = "Switch on main power";
    $m_driveractions[32] = "Open circuit breaker";
    $m_driveractions[33] = "Close circuit breaker";
    $m_driveractions[34] = "Switch off air condition";
    $m_driveractions[35] = "Switch on air condition";
    $m_driveractions[36] = "Switch off Regenerative brake";
    $m_driveractions[37] = "Switch on Regenerative brake";
    $m_driveractions[38] = "Switch of Eddy current brake";
    $m_driveractions[39] = "Switch on Eddy current brake";
    $m_driveractions[40] = "Swtich off Magnetic shoe brake";
    $m_driveractions[41] = "Switch on Magnetic shoe brake";
    $m_driveractions[42] = "Change of traction power";
    $m_driveractions[43] = "Train integrity confirmation";
    $m_driveractions[44] = "Emergency braking intervention";
    $m_driveractions[45] = "Service braking activation";
    $m_driveractions[46] = "Show (permitted + target) speed + target distance";
    $m_driveractions[47] = "Show permitted speed";
    $m_driveractions[48] = "Ack of SN";
    $m_driveractions[49] = "Ack of SE";
    $m_driveractions[50] = "Train integrity lost";
    
    $m_indication_1[0] = "Permitted speed has not changed";
    $m_indication_1[1] = "Permitted speed has changed";

    $m_indication_2[0] = "Target speed has not changed";
    $m_indication_2[1] = "Target speed has changed";

    $m_indication_3[0] = "Target distance has not changed";
    $m_indication_3[1] = "Target distance has changed";

    $m_indication_4[0] = "Release speed has not changed";
    $m_indication_4[1] = "Release speed has changed";

    $m_indication_5[0] = "Warning has not changed";
    $m_indication_5[1] = "Warning has changed";
    
    $m_brakestate[0] = "Revocation";
    $m_brakestate[1] = "Application";
    
    $jru_state[0] = "TRU off";
    $jru_state[1] = "TRU on";

# NID_STM bron = http://www.era.europa.eu/public/core/ertms/Pages/etcs_nid_stm.aspx
    
    $nid_stm[  0] = "Spain/ASFA";
    $nid_stm[  1] = "Netherlands/ATB";
    $nid_stm[  2] = "Spain/ASFA AVE";
    $nid_stm[  3] = "Spain/LZB Spain (C)";
    $nid_stm[  5] = "Belgium/TBL 1";
    $nid_stm[  6] = "Germany/INDUSI";    
    $nid_stm[  7] = "Belgium/TBL2/3";
    $nid_stm[  8] = "France/KVB";
    $nid_stm[  9] = "Germany-Austria/LZB";
    $nid_stm[ 10] = "Spain/LZB";
    $nid_stm[ 11] = "Italy/SCMT";
    $nid_stm[ 12] = "Luxembourg/MEMOR II+";
    $nid_stm[ 14] = "TVM";
    $nid_stm[ 15] = "Italy/BACC";
    $nid_stm[ 16] = "Italy/RSDD";
    $nid_stm[ 17] = "Hungary/EVM";
    $nid_stm[ 18] = "Belgium/Crocodile";
    $nid_stm[ 19] = "Spain/EBICAB 900";
    $nid_stm[ 20] = "UK/TPWS/AWS";
    $nid_stm[ 22] = "ATC2";
    $nid_stm[ 23] = "Finland/EBICAB 900";
    $nid_stm[ 24] = "Poland/EBICAB 900 (PL)";
    $nid_stm[ 25] = "Korea/KNR ATS";
    $nid_stm[ 26] = "Poland/SHP";
    $nid_stm[ 28] = "Belgium/TBL1+";
    $nid_stm[ 30] = "Denmark/ZUB 123";
    $nid_stm[ 31] = "Switzerland/ZUB 121 / Signum";
    $nid_stm[ 32] = "France/RPS";
    $nid_stm[ 33] = "Czech Republic/Slovakia/LS";
    $nid_stm[ 34] = "Portugal/EBICAB 700 (P)";
    $nid_stm[ 35] = "UK/selcab";
    $nid_stm[ 36] = "INDUSI I 60";
    $nid_stm[ 37] = "UK/TBL";
    $nid_stm[ 39] = "Balticum/ALSN";
    $nid_stm[ 40] = "Bulgaria/EBICAB";
    $nid_stm[ 45] = "China/CTCS-2";
    $nid_stm[254] = "EVC";
    $nid_stm[255] = "Multicast";
    
    $q_updown[0] = "Down link telegram";
    $q_updown[1] = "Up link telegram";
    
  }
  
sub IsNumeric {
    my $val = $_[0];
    if ($val !~/^[0-9|.|,|-]*$/) {
        return 0;   #Non-numeric
    } else {
        return 1;   #Numeric
    }
}  #End IsNumeric

sub encoded_unknown {
    my $text = shift;
    my $field = shift;
    my $var = shift;
    
    print "Kan niet decoderen: $text, $var\n";

    return substr(unpack("B16",pack("H*","ffffffff")),16-$ertms_fields{"$field"});
}

  sub encode_ansaldofield {
    my $text = shift;
    my $val;
    chomp($text);
    print "Encoding: $text\n" if $ETCS::Debug::Log;
    
    my ($field,$value) = split('=',$text);
    my $var;
    
    if ($field =~ /^OCTET_/) {
    	$val = unpack("B8",pack("H*",substr($value,2,2)));
	print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
	return $val;    	
    }
    
    if ($field =~ /^NID_RADIO_/) {
    	$val = unpack("B32",pack("H*",substr($value,2,8)));
	print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
	return $val;    	
    }
    
    if ($field eq "NID_OPERATIONAL") {
    	$value="0xffffffff" if ($value eq "Unknown");
    	$val = unpack("B32",pack("H*",substr($value,2,8)));
	print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
	return $val;    	
    }
    
    if ($field eq "JRU_DRIVER_ID") {
    	$value="0xffffffff" if ($value eq "Unknown");
    	$val = unpack("B32",pack("H*",substr($value,2,8)));
	print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
	return $val;    	
    }
    
    if ($field eq "X_TEXT") {
	my $lengte = length($value);
	$val = unpack("B".8*$lengte,$value);
	print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
	return $val;    	
    }
    
    unless (exists($ertms_fields{$field})) {
        print "Onbekend veld gevraagd $field\n" ;
        return "";
    }
    
    print "Field = $field len = ".$ertms_fields{$field}."\n" if $ETCS::Debug::Log;
    
    if (exists($vertaal{$field})) {
      $var = $vertaal{$field};
    } else {
      $var = lc($field);
    }

    no strict 'refs';

    if (defined(@$var)) {
      for (my $i=0;$i<=$#$var;$i++) {
      	if (exists($$var[$i])) {
      	  if ($$var[$i] eq $value) {
      	    $val = substr(unpack("B16",pack("n",int($i))),16-$ertms_fields{"$field"});
	    print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
      	    return $val;
      	  } elsif (exists($vertaal{$value}) and ($$var[$i] eq $vertaal{$value})) {
      	    $val = substr(unpack("B16",pack("n",int($i))),16-$ertms_fields{"$field"});
	    print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
      	    return $val;
      	  }	
      	}
      }
      if (IsNumeric($value)) {
        $val = substr(unpack("B16",pack("n",int($value))),16-$ertms_fields{"$field"});
      } else {
      	$val = encoded_unknown($text,$field,$var);
      }
      print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
      return $val;
    }

    use strict 'refs';
    
    if (IsNumeric($value)) {    
      $val = substr(unpack("B16",pack("n",int($value))),16-$ertms_fields{"$field"});
    } else {
      $val = encoded_unknown($text,$field,$var);
    }
    print "Encoded: $text  $val\n" if $ETCS::Debug::Log;
    return $val;
  }

  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    my $bitlen = length($bitstream);
    my $var;   
    
    if (exists($vertaal{$field})) {
      $var = $vertaal{$field};
    } else {
      $var = lc($field);
    }
        
    no strict 'refs';

# roep specifieke display routine aan

    my $proc = "Display_".$var;
    if (defined(&$proc)) {
      my $rv = &$proc($self,$field,$bitstream);
      return $rv unless $rv == 0;
    }
    
    if (($field eq "NID_RADIO") ||
             ($field =~ m/^NC_/)) {
      my $hexval = unpack("H*",pack("B*",$bitstream));
      print " ($hexval)" if $ETCS::Debug::Log;
      $self->setFieldHex($field,$hexval);
      return 1;
    }
    
    my $zerofill = substr("00000000",0, 7 - (($bitlen-1) %8));
    my $intval = hex(unpack("H*",pack("B*",$zerofill.$bitstream)));

    if (defined(@$var)) {
      $self->setFieldDecimal($field,$intval);
      if (exists($$var[$intval])) {
        $self->setFieldText($field,$$var[$intval]);
      }
      return 1;
    }

    use strict 'refs';
    
    if ( ($field =~ m/^D_/) ||
              ($field eq "L_ACKLEVELTR") ||
              ($field eq "L_ACKMAMODE") ||
              ($field eq "L_ADHESION") ||
              ($field eq "L_AXLELOAD") ||
              ($field =~ m/^L_DOUBT/) ||
              ($field eq "L_ENDSECTION") ||
              ($field eq "L_LOOP") ||
              ($field eq "L_MAMODE") ||
              ($field eq "L_REVERSEAREA") ||
              ($field eq "L_SECTION") ||
              ($field eq "L_TAFDISPLAY") ||
              ($field eq "L_TEXTDISPLAY") ||
              ($field eq "L_TRACKCOND") ||
              ($field eq "L_TSR") ){
      $zerofill = substr($zerofill,0, 7 - (($bitlen-1) %8));
      if ($field eq "D_REF") {
        if ($intval > 32767) {
          $intval = 65535 - $intval;
        }
      }
      if (!defined($scale)) {
      	print "scale niet gezet voor $field!\n" if $ETCS::Debug::Log;
        $self->setFieldText($field,"Value: $intval (Q_SCALE in error)");
        print Dumper($self) if $ETCS::Debug::Log;
#        die "We zitten echt in een fout frame";
      } else {
        $intval *= $scale;
        print " ($intval m)" if $ETCS::Debug::Log;
        $self->setFieldDecimal($field,$intval);
      }
       return 1;
    } elsif (($field eq "JRU_TTS") || ($field eq "DRU_TTS")) {
      $intval *= 50;
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      return 1;
    } elsif (($field eq "NID_LRBG") || ($field eq "NID_PRVBG") || ($field eq "NID_LTRBG")) {
      my $nid_bg_str;
      if ($bitstream eq "111111111111111111111111") {
        $nid_bg_str = "[*,*]";
      } else {
        my $nid_c = unpack("n",pack("B*","000000".substr($bitstream,0,10)));
        my $nid_bg= unpack("n",pack("B*","00".substr($bitstream,10,14)));
        $nid_bg_str = sprintf("[$nid_c,$nid_bg]");
      }
      print " $nid_bg_str" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$nid_bg_str);
      return 1;
    } elsif ($field =~ m/M_AXLELOAD/) {
      return Display_axleload($self,$field,$intval);
    } elsif (($field =~ m/^N_/) || 
	     ($field =~ m/^NID_/) || 
	     ($field =~ m/^T_/) || 
	     ($field =~ m/^M_/) || 
	     ($field =~ m/^G_/) || 
	     ($field =~ m/^Q_/) || 
	     ($field =~ m/^JRU_/) || 
	     ($field =~ m/^DRU_/) || 
	     ($field =~ m/^L_/) ) {
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      return 1;
    } elsif ($field =~ m/^A_.*_CAP$/) {
      return Display_acceleration_component_brake_capacity($self,$field,$bitstream);
    } elsif ($field =~ m/^V_.*_CAP$/) {
      return Display_speed_component_brake_capacity($self,$field,$bitstream);
    } elsif ($field =~ m/^V_/) {
      return Display_speed($self,$field,$bitstream);
    }
    return 0;
  }

  sub Display_x_text {
    my ($self,$field,$bitstream) = @_;
    my $textval = pack("B*",$bitstream);
    print " ($textval)" if $ETCS::Debug::Log;
    $self->setFieldText($field,$textval);
    return 1;
  }
  
  sub Display_nid_operational {
    my ($self,$field,$bitstream) = @_;

    my $hexval = unpack("H*",pack("B*",$bitstream));
    print " ($hexval)" if $ETCS::Debug::Log;
    $self->setFieldHex($field,$hexval);
    if ($hexval eq 'ffffffff') {
      $self->setFieldText($field,'Unknown');
    } else {
      $hexval =~ s/[f]+$//;
      $self->setFieldText($field,$hexval);
    }
    return 1;
  }
  
  
  sub Display_jru_driver_id {
    my ($self,$field,$bitstream) = @_;

    my $hexval = unpack("H*",pack("B*",$bitstream));
    $self->setFieldHex($field,$hexval);
    if (length ($bitstream) >32 ) {
      if ($hexval eq 'ffffffffffffffff') {
         $self->setFieldText($field,'Unknown');
      } else {
        my $textval = substr($hexval, 1, 1) . substr($hexval, 3, 1) . substr($hexval, 5, 1) . substr($hexval, 7, 1) . substr($hexval, 9, 1) . substr($hexval, 11, 1) . substr($hexval, 13, 1) . substr($hexval, 15, 1);
        $self->setFieldText($field, $textval);
      }
    } else {
      if ($hexval eq 'ffffffff') {
         $self->setFieldText($field,'Unknown');
      } else {
        my $intval=hex($hexval);
        print " ($intval)" if $ETCS::Debug::Log;
        $self->setFieldDecimal($field,$intval);
      }
    }
    return 1;
  }
  
  sub Display_axleload {
    my ($self,$field,$intval) = @_;
    $self->setFieldDecimal($field,$intval);
    my $textval;
    if ($intval <= 80) {
      $textval = sprintf("%.1f t", $intval/2);
    } elsif ($intval == 126) {
      $textval = "> 40 t";
    }
    $self->setFieldText($field,$textval);
    return 1;
  }
  
  sub Display_speed {
    my ($self,$field,$bitstream) = @_;
    my $intval = hex(unpack("H*",pack("B*","0".$bitstream)));
    if ($intval < 121) {
      $self->setFieldDecimal($field,$intval*5);
    }
    return 1;
  }
  
  sub Display_v_release {
    my ($self,$field,$bitstream) = @_;
    my $intval = hex(unpack("H*",pack("B*","0".$bitstream)));
    if ($intval < 121) {
      $self->setFieldDecimal($field,$intval*5);
    } elsif ($intval == 126) {
      $self->setFieldText($field,"Use onboard calculated release speed");
    } elsif ($intval == 127) {
      $self->setFieldText($field,"Use national value");
    }
    return 1;
  }
  
  sub Display_v_target {
    my ($self,$field,$bitstream) = @_;
    my $intval = hex(unpack("H*",pack("B*","0".$bitstream)));
    if ($intval < 121) {
      $self->setFieldDecimal($field,$intval*5);
      print " ($intval km/h)" if $ETCS::Debug::Log;
    } elsif ($intval ==127) {
      $self->setFieldText($field,"No more shown");
    }
    return 1;
  }
  
  sub Display_speed_component_brake_capacity {
    my ($self,$field,$bitstream) = @_;
    my $intval = hex(unpack("H*",pack("B*","000000".$bitstream)));
    if ($intval < 601) {
      $self->setFieldDecimal($field,$intval);
      my $textval = sprintf("%d m/s", $intval);
      $self->setFieldText($field,$textval);
    }
    return 1;
  }
  
  sub Display_acceleration_component_brake_capacity {
    my ($self,$field,$bitstream) = @_;
    my $intval = hex(unpack("H*",pack("B*","00000000".$bitstream)));
    $self->setFieldDecimal($field,$intval);
    my $doubleval = $intval/100;
    my $textval = sprintf("%.2f m/s2", $doubleval);
    $self->setFieldText($field,$textval);
    return 1;
  }
  
  sub SetAnsaldo {
    print "Set to ETCS::JRU Ansldo variant\n";
    
# Vertaling van franse termen naar engels
    
# M_DRIVERACTIONS

    $vertaal{"Acquittement du niveau 1"} = "Ack Level 1";
    $vertaal{"Acquittement du mode On Sight"} = "Ack of on sight";
    $vertaal{"Acquittement du mode Trip"} = "Ack of trip";
    $vertaal{"Acquittement du mode Staff Responsible"} = "Ack of staff responsible";
    $vertaal{"Confirmation de voie libre"} = "Confirm track ahead free";
    $vertaal{"Acquittement du niveau 0"} = "Ack Level 0";
    $vertaal{"Acquittement du niveau 2"} = "Ack Level 2";
    $vertaal{"S�lection du mode Override EOA (outrepasser)"} = "Override EOA selected";
    $vertaal{"R�vocation des freins"} = "Emergency brakes release";
    $vertaal{"Acquittement du niveau STM"} = "Ack Level STM";

# M_EVENTS
   
    $vertaal{"Erreur de transmission Eurobalise"} = "Balise transmission error";
    $vertaal{"Franchissement EOA par le train"} = "Override EOA";
    $vertaal{"Detection une marche arriere"} = "Reverse movement supervision";
    $vertaal{"Erreur de transmission Euroradio"} = "Radio transmission error";
    $vertaal{"Panne odometrie"} = "Odometer failure";
    $vertaal{"Supervision arret du train"} = "Standstill supervision intervention";
    
# Q_MANUEL_AUTO

    $q_manuel_auto[0] = "Manuel";
    $q_manuel_auto[1] = "Automatique";
    $q_manuel_auto[2] = "R�serve";
        
  }
  
  sub SetSubset27v224 {
    print "Set ETCS::Structure to subset 27 v2.2.4\n";
    $ertms_fields{"JRU_DRIVER_ID"}      = 64;
    $ertms_fields{"L_MESSAGE"}		= 11;
    $ertms_fields{"T_CUT_OFF"}          = 12;
    $ertms_fields{"T_SERVICE_DELAY"}    = 12;
    $ertms_fields{"T_EMERGENCY_DELAY"}  = 12;

    $m_driveractions[0] = "Ack of on sight";
    $m_driveractions[1] = "Ack of shunting";
    $m_driveractions[2] = "Ack of trip";
    $m_driveractions[3] = "Ack of staff responsible";
    $m_driveractions[4] = "Ack of unfitted";
    $m_driveractions[5] = "Ack of reversing";
    $m_driveractions[6] = "Ack Level 0";
    $m_driveractions[7] = "Ack Level 1";
    $m_driveractions[8] = "Ack Level 2";
    $m_driveractions[9] = "Ack Level 3";
    $m_driveractions[10] = "Ack Level STM";
    $m_driveractions[11] = "Shunting selected";
    $m_driveractions[12] = "Non Leading selected";
    $m_driveractions[13] = "Reversing selected";
    $m_driveractions[14] = "Override EOA selected";
    $m_driveractions[15] = "Override route suitability";
    $m_driveractions[16] = "Emergency brakes release";
    $m_driveractions[17] = "Exit of shunting";
    $m_driveractions[18] = "Exit of Non Leading";
    $m_driveractions[19] = "Start of mission";
    $m_driveractions[20] = "Data Entry requested";
    $m_driveractions[21] = "Driver confirmation of data";
    $m_driveractions[22] = "Confirm track ahead free";
    $m_driveractions[23] = "Ack of Plain Text Messages";
    $m_driveractions[24] = "Ack of Fixed Text Messages";
    $m_driveractions[25] = "Intentionally deleted";
    $m_driveractions[26] = "Train integrity confirmation";
    $m_driveractions[27] = "Show (permitted + target) speed + target distance";
    $m_driveractions[28] = "Change of language";
    $m_driveractions[29] = "Ack of SN/SE mode";
    $m_driveractions[30] = "Desk closed";
    $m_driveractions[31] = "Distance in SR changed";
    $m_driveractions[32] = "Speed in SR changed";
    $m_driveractions[33] = "Level 0 selected";
    $m_driveractions[34] = "Level 1 selected";
    $m_driveractions[35] = "Level 2 selected";
    $m_driveractions[36] = "Level 3 selected";
    $m_driveractions[37] = "Level STM selected";
    $m_driveractions[38] = "";
    $m_driveractions[39] = "";
    $m_driveractions[40] = "";
    $m_driveractions[41] = "";
    $m_driveractions[42] = "";
    $m_driveractions[43] = "";
    $m_driveractions[44] = "";
    $m_driveractions[45] = "";
    $m_driveractions[46] = "";
    $m_driveractions[47] = "";
    $m_driveractions[48] = "";
    $m_driveractions[49] = "";
    $m_driveractions[50] = "";

    $m_events[6] = "Loop transmission error";
    $m_events[7] = "Odometer failure";
    $m_events[8] = "Passenger brake application";
    $m_events[9] = "Version incompatibility with RBC";
    $m_events[10] = "Override EoA deactivated";
    $m_events[11] = "Stop due to route unsuitability";
    $m_events[12] = "";

    $m_mode[15] = "INITIALISATION";
  }
  
  sub SetSubset27v229 {
    print "Set ETCS::Structure to subset 27 v2.2.9\n";
    $m_driveractions[0] = "Ack of on sight";
    $m_driveractions[1] = "Ack of shunting";
    $m_driveractions[2] = "Ack of trip";
    $m_driveractions[3] = "Ack of staff responsible";
    $m_driveractions[4] = "Ack of unfitted";
    $m_driveractions[5] = "Ack of reversing";
    $m_driveractions[6] = "Ack Level 0";
    $m_driveractions[7] = "Ack Level 1";
    $m_driveractions[8] = "Ack Level 2";
    $m_driveractions[9] = "Ack Level 3";
    $m_driveractions[10] = "Ack Level STM";
    $m_driveractions[11] = "Shunting selected";
    $m_driveractions[12] = "Non Leading selected";
    $m_driveractions[13] = "Reversing selected";
    $m_driveractions[14] = "Override EOA selected";
    $m_driveractions[15] = "Override route suitability";
    $m_driveractions[16] = "Emergency brakes release";
    $m_driveractions[17] = "Exit of shunting";
    $m_driveractions[18] = "Exit of Non Leading";
    $m_driveractions[19] = "Start of mission";
    $m_driveractions[20] = "Data Entry requested";
    $m_driveractions[21] = "Driver confirmation of data";
    $m_driveractions[22] = "Confirm track ahead free";
    $m_driveractions[23] = "Ack of Plain Text Messages";
    $m_driveractions[24] = "Ack of Fixed Text Messages";
    $m_driveractions[25] = "Intentionally deleted";
    $m_driveractions[26] = "Train integrity confirmation";
    $m_driveractions[27] = "Show (permitted + target) speed + target distance";
    $m_driveractions[28] = "Change of language";
    $m_driveractions[29] = "Show geographical position";
    $m_driveractions[30] = "Desk closed";
    $m_driveractions[31] = "Distance in SR changed";
    $m_driveractions[32] = "Speed in SR changed";
    $m_driveractions[33] = "Level 0 selected";
    $m_driveractions[34] = "Level 1 selected";
    $m_driveractions[35] = "Level 2 selected";
    $m_driveractions[36] = "Level 3 selected";
    $m_driveractions[37] = "Level STM selected";
    $m_driveractions[38] = "";
    $m_driveractions[39] = "";
    $m_driveractions[40] = "";
    $m_driveractions[41] = "";
    $m_driveractions[42] = "";
    $m_driveractions[43] = "";
    $m_driveractions[44] = "";
    $m_driveractions[45] = "";
    $m_driveractions[46] = "";
    $m_driveractions[47] = "";
    $m_driveractions[48] = "";
    $m_driveractions[49] = "";
    $m_driveractions[50] = "";
  }
}

{
    package ETCS::Debug;
    
    our $Log=0;
    
    sub debugOn {
        $Log = 1;
    }

    sub debugOff {
        $Log = 0;
    }
}

1;
                                           