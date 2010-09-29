{
  package ETCS::Packet;
  use Exporter;
  @ISA = qw(ETCS::Structure); 
  use strict;
    
  BEGIN {
   $ETCS::Packet::VERSION = "0.3";
  }
  
  sub new {
    my $self= {};
    bless $self;
    return $self;
  }
}

{
  package ETCS::TrackPacket;
  use Exporter;
  @ISA = qw(ETCS::Packet); 
  use strict;
  
  my @msg_trackpacketnames;
  my @msg_trackpackets;
    
  BEGIN {
    $ETCS::TrackPacket::VERSION = "0.3";
#
# 0.3 MvL: packets 45, 46 en 90 toegevoegd
# 0.2 MvL: subfields moved into module
#

    $msg_trackpacketnames[   3] = "NATIONAL VALUES";
    $msg_trackpackets[  3]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_VALIDNV","N_ITER","NID_C","V_NVSHUNT","V_NVSTFF","V_NVONSIGHT","V_NVUNFIT","V_NVREL","D_NVROLL","Q_NVSRBKTRG","Q_NVEMRRLS","V_NVALLOWOVTRP","V_NVSUPOVTRP","D_NVOVTRP","T_NVOVTRP","D_NVPOVTRP","M_NVCONTACT","T_NVCONTACT","M_NVDERUN","D_NVSTFF","Q_NVDRIVER_ADHES"];
    
    $msg_trackpacketnames[   5] = "LINKING";
    $msg_trackpackets[  5]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*989","N_ITER","*989"];
    
    $msg_trackpacketnames[  12] = "LEVEL 1 MOVEMENT AUTHORITY";
    $msg_trackpackets[ 12]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","V_MAIN","V_LOA","T_LOA","N_ITER","*998","L_ENDSECTION","Q_SECTIONTIMER","*997","Q_ENDTIMER","*996","Q_DANGERPOINT","*995","Q_OVERLAP","*994"];
    
    $msg_trackpacketnames[  15] = "LEVEL 2/3 MOVEMENT AUTHORITY";
    $msg_trackpackets[ 15]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","V_LOA","T_LOA","N_ITER","*998","L_ENDSECTION","Q_SECTIONTIMER","*997","Q_ENDTIMER","*996","Q_DANGERPOINT","*995","Q_OVERLAP","*994"];
    
    $msg_trackpacketnames[  16] = "REPOSITIONING INFORMATION";
    $msg_trackpackets[ 16]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","L_SECTION"];
    
    $msg_trackpacketnames[  21] = "GRADIENT PROFILE";
    $msg_trackpackets[ 21]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*990","N_ITER","*990"];
    
    $msg_trackpacketnames[  27] = "INTERNATIONAL STATIC SPEED PROFILE";
    $msg_trackpackets[ 27]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*991","N_ITER","*991"];
    
    $msg_trackpacketnames[  39] = "TRACK CONDITION CHANGE OF TRACTION POWER";
    $msg_trackpackets[ 39]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_TRACTION","M_TRACTION"];
    
    $msg_trackpacketnames[  41] = "LEVEL TRANSITION ORDER";
    $msg_trackpackets[ 41]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_LEVELTR","M_LEVELTR","NID_STM","L_ACKLEVELTR","N_ITER","*999"];
    
    $msg_trackpacketnames[  42] = "SESSION MANAGEMENT";
    $msg_trackpackets[ 42]=["NID_PACKET","Q_DIR","L_PACKET","Q_RBC","NID_C","NID_RBC","NID_RADIO","Q_SLEEPSESSION"];
    
    $msg_trackpacketnames[  44] = "DATA USED BY OTHER APPLICATIONS";
    $msg_trackpackets[ 44]=["NID_PACKET","Q_DIR","L_PACKET","NID_XUSER","XUSER_DATA"];
    
    $msg_trackpacketnames[  45] = "Radio Network registration";
    $msg_trackpackets[ 45]=["NID_PACKET","Q_DIR","L_PACKET","NID_MN"];
    
    $msg_trackpacketnames[  46] = "Conditional Level Transition Order";
    $msg_trackpackets[ 46]=["NID_PACKET","Q_DIR","L_PACKET","M_LEVELTR","NID_STM","N_ITER","*977"];
    
    $msg_trackpacketnames[  49] = "LIST OF BALISES FOR SH AREA";
    $msg_trackpackets[ 49]=["NID_PACKET","Q_DIR","L_PACKET","N_ITER","*984"];
    
    $msg_trackpacketnames[  51] = "AXLE LOAD SPEED PROFILE";
    $msg_trackpackets[ 51]=["NID_PACKET","Q_DIR","L_PACKET","Q_TRACKINIT","*983"];
    
    $msg_trackpacketnames[  57] = "MA REQUEST PARAMETERS";
    $msg_trackpackets[ 57]=["NID_PACKET","Q_DIR","L_PACKET","T_MAR","T_TIMEOUTRQST","T_CYCRQST"];
    
    $msg_trackpacketnames[  58] = "POSITION REPORT PARAMETERS";
    $msg_trackpackets[ 58]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","T_CYCLOC","D_CYCLOC","M_LOC","N_ITER","*993"];
    
    $msg_trackpacketnames[  63] = "LIST OF BALISES FOR SR AUTHORITY";
    $msg_trackpackets[ 63]=["NID_PACKET","Q_DIR","L_PACKET","N_ITER","*984"];
    
    $msg_trackpacketnames[  65] = "TEMPORARY SPEED RESTRICTION";
    $msg_trackpackets[ 65]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","NID_TSR","D_TSR","L_TSR","Q_FRONT","V_TSR"];
    
    $msg_trackpacketnames[  66] = "TEMPORARY SPEED RESTRICTION REVOCATION";
    $msg_trackpackets[ 66]=["NID_PACKET","Q_DIR","L_PACKET","NID_TSR"];
    
    $msg_trackpacketnames[  67] = "TRACK CONDITION BIG METAL MASSES";
    $msg_trackpackets[ 67]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*980","N_ITER","*980"];
    
    $msg_trackpacketnames[  68] = "TRACK CONDITIONS";
    $msg_trackpackets[ 68]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","Q_TRACKINIT","D_TRACKINIT","*987","N_ITER","*987"];
    
    $msg_trackpacketnames[  70] = "ROUTE SUITABILITY DATA";
    $msg_trackpackets[ 70]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","Q_TRACKINIT","D_TRACKINIT","*979","N_ITER","*979"];
    
    $msg_trackpacketnames[  71] = "ADHESION FACTOR";
    $msg_trackpackets[ 71]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_ADHESION","L_ADHESION","M_ADHESION"];
    
    $msg_trackpacketnames[  72] = "PLAIN TEXT MESSAGE";
    $msg_trackpackets[ 72]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","Q_TEXTCLASS","Q_TEXTDISPLAY","D_TEXTDISPLAY","M_MODETEXTDISPLAY","M_LEVELTEXTDISPLAY","NID_STM","L_TEXTDISPLAY","T_TEXTDISPLAY","M_MODETEXTDISPLAY","M_LEVELTEXTDISPLAY","NID_STM","Q_TEXTCONFIRM","L_TEXT","X_TEXT"];
    
    $msg_trackpacketnames[  76] = "PREDEFINED TEXT MESSAGE";
    $msg_trackpackets[ 76]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","Q_TEXTCLASS","Q_TEXTDISPLAY","D_TEXTDISPLAY","M_MODETEXTDISPLAY","M_LEVELTEXTDISPLAY","NID_STM","L_TEXTDISPLAY","T_TEXTDISPLAY","M_MODETEXTDISPLAY","M_LEVELTEXTDISPLAY","NID_STM","Q_TEXTCONFIRM","Q_TEXT"];
    
    $msg_trackpacketnames[ 79] = "GEOGRAPHICAL POSITION INFORMATION";
    $msg_trackpackets[ 79]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*978","N_ITER","*978"];
    
    $msg_trackpacketnames[ 80] = "MODE PROFILE";
    $msg_trackpackets[ 80]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","*988","N_ITER","*988"];
    
    $msg_trackpacketnames[ 90] = "Track Ahead Free up to level 2/3 transition location";
    $msg_trackpackets[ 90]=["NID_PACKET","Q_DIR","L_PACKET","Q_NEWCOUNTRY","NID_C","NID_BG"];
    
    $msg_trackpacketnames[ 131] = "RBC TRANSITION ORDER";
    $msg_trackpackets[131]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_RBCTR","NID_C","NID_RBC","NID_RADIO","Q_SLEEPSESSION"];
    
    $msg_trackpacketnames[ 132] = "DANGER FOR SHUNTING INFORMATION";
    $msg_trackpackets[132]=["NID_PACKET","Q_DIR","L_PACKET","Q_ASPECT"];
    
    $msg_trackpacketnames[ 133] = "RADIO INFILL AREA INFORMATION";
    $msg_trackpackets[133]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","Q_RIU","NID_C","NID_RIU","NID_RADIO","D_INFILL","NID_C","NID_BG"];
    
    $msg_trackpacketnames[ 134] = "EOLM PACKET";
    $msg_trackpackets[134]=["NID_PACKET","Q_DIR","Q_SCALE","NID_LOOP","D_LOOP","L_LOOP","Q_LOOPDIR","Q_SSCODE"];
    
    $msg_trackpacketnames[ 135] = "ASSIGNMENT OF COORDINATE SYSTEM";
    $msg_trackpackets[135]=["NID_PACKET","Q_DIR","L_PACKET","NID_C","NID_BG","Q_ORIENTATION"];
    
    $msg_trackpacketnames[ 136] = "INFILL LOCATION REFERENCE";
    $msg_trackpackets[136]=["NID_PACKET","Q_DIR","L_PACKET","Q_NEWCOUNTRY","NID_C","NID_BG"];
    
    $msg_trackpacketnames[ 137] = "STOP IF IN STAFF RESPONSIBLE";
    $msg_trackpackets[137]=["NID_PACKET","Q_DIR","L_PACKET","Q_SRSTOP"];
    
    $msg_trackpacketnames[ 138] = "REVERSING AREA INFORMATION";
    $msg_trackpackets[138]=["NID_PACKET","Q_DIR","L_PACKET","Q_SCALE","D_STARTREVERSE","L_REVERSEAREA"];
    
    $msg_trackpacketnames[ 139] = "REVERSING SUPERVISION INFORMATION";
    $msg_trackpackets[139]=["NID_PACKET","Q_DIR","L_PACKET","D_REVERSE","L_REVERSE"];
    
    $msg_trackpacketnames[ 140] = "TRAIN RUNNING NUMBER FROM RBC";
    $msg_trackpackets[140]=["NID_PACKET","Q_DIR","L_PACKET","NID_OPERATIONAL"];
    
    $msg_trackpacketnames[ 141] = "DEFAULT GRADIENT FOR TSR";
    $msg_trackpackets[141]=["NID_PACKET","Q_DIR","L_PACKET","Q_GDIR","G_TSR"];
    
    $msg_trackpacketnames[ 254] = "DEFAULT BALISE, LOOP OR RIU INFORMATION";
    $msg_trackpackets[254]=["NID_PACKET","Q_DIR","L_PACKET"];    

    $msg_trackpackets[977] = ["M_LEVELTR","NID_STM"];
    $msg_trackpackets[978] = ["Q_NEWCOUNTRY","NID_C","NID_BG","D_POSOFF","Q_MPOSITION","M_POSITION"];
    $msg_trackpackets[979] = ["D_SUITABILITY","Q_SUITABILITY","M_LOADINGGAUGE","M_AXLELOAD","M_TRACTION"];
    $msg_trackpackets[980] = ["D_TRACKCOND","L_TRACKCOND"];
    $msg_trackpackets[981] = ["M_AXLELOAD","V_AXLELOAD"];
    $msg_trackpackets[982] = ["D_AXLELOAD","L_AXLELOAD","Q_FRONT","N_ITER","*981"];
    $msg_trackpackets[983] = ["*982","N_ITER","*982"];
    $msg_trackpackets[984] = ["Q_NEWCOUNTRY","NID_C","NID_BG"];
    $msg_trackpackets[987] = ["D_TRACKCOND","L_TRACKCOND","M_TRACKCOND"];
    $msg_trackpackets[988] = ["D_MAMODE","M_MAMODE","V_MAMODE","L_MAMODE","L_ACKMAMODE"];
    $msg_trackpackets[989] = ["D_LINK","Q_NEWCOUNTRY","NID_C","NID_BG","Q_LINKORIENTATION","Q_LINKREACTION","Q_LINKACC"];
    $msg_trackpackets[990] = ["D_GRADIENT","Q_GDIR","G_A"];
    $msg_trackpackets[991] = ["D_STATIC","V_STATIC","Q_FRONT","N_ITER","*992"];
    $msg_trackpackets[992] = ["NC_DIFF","V_DIFF"];
    $msg_trackpackets[993] = ["D_LOC","Q_LGTLOC"];
    $msg_trackpackets[994] = ["D_STARTOL","T_OL","D_OL","V_RELEASEOL"];
    $msg_trackpackets[995] = ["D_DP","V_RELEASEDP"];
    $msg_trackpackets[996] = ["T_ENDTIMER","D_ENDTIMERSTARTLOC"];
    $msg_trackpackets[997] = ["T_SECTIONTIMER","D_SECTIONTIMERSTOPLOC"];
    $msg_trackpackets[998] = ["L_SECTION","Q_SECTIONTIMER","*997"];
    $msg_trackpackets[999] = ["M_LEVELTR","NID_STM","L_ACKLEVELTR"];
  }
  
  sub new {
    my $self= {};
    bless $self;
    return $self;
  }

  sub process_packet {
    my ($pack,$bitstream,$msg_packets) = @_;
    
    my $nid_packet=unpack("C",pack("B*",substr($bitstream,0,8)));
    if ($nid_packet != 255) {
      print "Packet: $nid_packet\n" if $ETCS::Debug::Log;
      my $count = $pack->process_bits($bitstream,$msg_trackpackets[$nid_packet],0,0);
      if ($count < 0) {
        $pack->addField('NID_PACKET',substr($bitstream,0,8));
        $pack->setFieldDecimal('NID_PACKET',$nid_packet);
        $pack->{Error} = "Undefined packet encountered";
      }
      return $count;
    } else {
      $pack->addField('NID_PACKET',substr($bitstream,0,8));
      $pack->setFieldDecimal('NID_PACKET',$nid_packet);
      return -1;
    }
  }     
  
  sub process_subfields {
    my ($pack,$msg,$dummsg,$iterdepth,$iter) = @_;
    return $pack->process_bits($msg,$msg_trackpackets[$dummsg],$iterdepth,$iter);
  }
  
  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    
    if ($field eq "NID_PACKET") {
      my $intval = unpack("C",pack("B*",$bitstream));
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$msg_trackpacketnames[$intval]);
      
      return 1;
    } else {
      return $self->SUPER::displayValue($field,$bitstream);
    }
  }
}

{
  package ETCS::TrainPacket;
  use Exporter;
  @ISA = qw(ETCS::Packet); 
  use strict;
  
  my @msg_trainpacketnames;
  my @msg_trainpackets;
    
  BEGIN {
    $ETCS::TrainPacket::VERSION = "0.2";
#
# 0,2 MvL: Packet 9 toegevoegd
#
    
    $msg_trainpacketnames[   0] = "POSITION REPORT";
    $msg_trainpackets[  0]=["NID_PACKET","L_PACKET","Q_SCALE","NID_LRBG","D_LRBG","Q_DIRLRBG","Q_DLRBG","L_DOUBTOVER","L_DOUBTUNDER","Q_LENGTH","L_TRAININT","V_TRAIN","Q_DIRTRAIN","M_MODE","M_LEVEL","NID_STM"];
    
    $msg_trainpacketnames[   1] = "POSITION REPORT BASED ON TWO BALISE GROUPS";
    $msg_trainpackets[  1]=["NID_PACKET","L_PACKET","Q_SCALE","NID_LRBG","NID_PRVBG","D_LRBG","Q_DIRLRBG","Q_DLRBG","L_DOUBTOVER","L_DOUBTUNDER","Q_LENGTH","L_TRAININT","V_TRAIN","Q_DIRTRAIN","M_MODE","M_LEVEL","NID_STM"];
    
    $msg_trainpacketnames[   3] = "ONBOARD TELEPHONE NUMBERS";
    $msg_trainpackets[  3]=["NID_PACKET","L_PACKET","N_ITER","NID_RADIO"];
    
    $msg_trainpacketnames[   4] = "ERROR REPORTING";
    $msg_trainpackets[  4]=["NID_PACKET","L_PACKET","M_ERROR"];
    
    $msg_trainpacketnames[   9] = "Level 2/3 transition information";
    $msg_trainpackets[  9]=["NID_PACKET","L_PACKET","NID_LTRBG"];
    
    $msg_trainpacketnames[  11] = "VALIDATED TRAINDATA";
    $msg_trainpackets[  11]=["NID_PACKET","L_PACKET","NID_OPERATIONAL","NC_TRAIN","L_TRAIN","V_MAXTRAIN","M_LOADINGGAUGE","M_AXLELOAD","M_AIRTIGHT","N_ITER","M_TRACTION","N_ITER","NID_STM"];
    
    $msg_trainpacketnames[  44] = "DATA USED BY OTHER APPLICATIONS";
    $msg_trainpackets[  44]=["NID_PACKET","L_PACKET","NID_XUSER","XUSER_DATA"];
  }
  
  sub new {
    my $self= {};
    bless $self;
    return $self;
  }
  
  sub process_packet {
    my ($pack,$bitstream,$msg_packets) = @_;
    
    my $nid_packet=unpack("C",pack("B*",substr($bitstream,0,8)));
    if ($nid_packet != 255) {
      print "Packet: $nid_packet\n" if $ETCS::Debug::Log;
      my $count = $pack->process_bits($bitstream,$msg_trainpackets[$nid_packet],0,0);
      if ($count < 0) {
        $pack->addField('NID_PACKET',substr($bitstream,0,8));
        $pack->setFieldDecimal('NID_PACKET',$nid_packet);
        $pack->{Error} = "Undefined packet encountered";
      }
      return $count;
    } else {
      $pack->addField('NID_PACKET',substr($bitstream,0,8));
      $pack->setFieldDecimal('NID_PACKET',$nid_packet);
      return -1;
    }
  }
 
  sub displayValue {
    my ($self,$field,$bitstream) = @_;
    
    if ($field eq "NID_PACKET") {
      my $intval = unpack("C",pack("B*",$bitstream));
      print " ($intval)" if $ETCS::Debug::Log;
      $self->setFieldDecimal($field,$intval);
      $self->setFieldText($field,$msg_trainpacketnames[$intval]);
      return 1;
    } else {
      return $self->SUPER::displayValue($field,$bitstream);
    }
  }
}

1;