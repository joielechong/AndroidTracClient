{
  package ETCS;
  require Exporter;
  use ETCS::Structure;
  use ETCS::Balise;
  use ETCS::RBC;
  use ETCS::Packet;
  use ETCS::Canape;
  use ETCS::JRU;
  use ETCS::DRU;

  @ISA = qw(Exporter);

  BEGIN {
    $ETCS::VERSION = "0.5";
#
# 0.5 moved %ertms_fields to ETCS::Structure leavingthis module empty
#
  }  
}

1;
