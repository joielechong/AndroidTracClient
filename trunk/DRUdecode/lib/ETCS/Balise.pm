{
  package ETCS::Balise;
  use Exporter;
  use Data::Dumper;
  @ISA = qw(ETCS::Structure);
  use strict;
  
  BEGIN {
#
# 0.3 removed ETCS::Track from @ISA
#
  $ETCS::Balise::VERSION = "0.3";
  }
  
  sub new {
    my $self= {};
    $self->{Header} = ETCS::Balise::Header::new();
    $self->{MessageType} = "BaliseTelegram";
    bless $self;
    return $self;
  }
  
  sub setMessage{
    my ($self,$bits,$balsize) =@_;
    
    $self->{BitMessage} = $bits;
    $balsize=length($bits) if !defined($balsize);
    
    $self->{Header}->setMessage(substr($bits,0,50));
    my $msgpos = 50;
    my $nrpack=0;
        
    while ($msgpos < $balsize) {
      my $pack = $self->addPacket($nrpack);
      my $rv = $pack->process_packet(substr($bits,$msgpos,$balsize-$msgpos));
      last if $rv == -1;
      $msgpos += $rv;
      $nrpack++;
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
  
}

{
  package ETCS::Balise::Header;
  use Exporter;
  use Data::Dumper;
  
  BEGIN {
    $ETCS::Balise::Header::VERSION = "0.1";
  }
  
  @ISA = qw(ETCS::Structure);
  use strict;

  my @msg_baliseheader = ("Q_UPDOWN","M_VERSION","Q_MEDIA","N_PIG","N_TOTAL","M_DUP",
			"M_MCOUNT","NID_C","NID_BG","Q_LINK");
   
  sub new {
    my $self= {};
    bless $self;
    return $self;
  }
  
  sub setMessage {
    my ($self,$bits) = @_;
    
    return $self->process_bits($bits,\@msg_baliseheader,0,0);
  }
}

1;
