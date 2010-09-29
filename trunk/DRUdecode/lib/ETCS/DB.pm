{
  package ETCS::DB;
  use Exporter;
  use Data::Dumper;
  use DBI;
  use strict;
  
  BEGIN {
#
# 0.2 Optional 2nd arg with path to database
# 0.1 Initial version, experimental with a Database on D disk because of performance
#
  $ETCS::DB::VERSION = "0.2";
  }
  
  sub new {
    my $self= {};
    $self->{trainid}=shift;
    $self->{dbname}=shift;
    $self->{dbname}="D:/Tijdelijk/JRUlogging.mdb" unless defined($self->{dbname});
    my $dbh = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=".$self->{dbname});
    $self->{sth_msg1} = $dbh->prepare("INSERT INTO Message (parent,bitstream,hexstream,messagetype,trainid) VALUES (NULL,?,?,?,?)");
    $self->{sth_msg2} = $dbh->prepare("INSERT INTO Message (parent,bitstream,hexstream,messagetype,trainid) VALUES (?,?,?,?,?)");
    $self->{sth_msgup} = $dbh->prepare("UPDATE Message set tijd=?,tts=? where seqnr=?");
    $self->{sth_msgup1} = $dbh->prepare("UPDATE Message set tijd=p.tijd,tts=p.tts from message p where message.seqnr=? and message.parent=p.seqnr ");
    $self->{sth_msgup2} = $dbh->prepare("UPDATE Message INNER JOIN (Datablock AS h INNER JOIN velden AS f ON h.seqnr = f.blockseq) ON Message.seqnr = h.messageseq SET Message.internal_message = [tekst] WHERE (((Message.seqnr)=?) AND ((h.type)='Header') AND ((f.fieldseq)=0));");
    $self->{sth_msgid} = $dbh->prepare("SELECT max(seqnr) FROM Message");
    $self->{sth_hdr} = $dbh->prepare("INSERT INTO Datablock (messageseq,type,packetnr) VALUES (?,?,NULL)");
    $self->{sth_pkt} = $dbh->prepare("INSERT INTO Datablock (messageseq,type,packetnr) VALUES (?,'Packet',?)");
    $self->{sth_blkid} = $dbh->prepare("SELECT max(seqnr) FROM Datablock");
    $self->{sth_fld} = $dbh->prepare("INSERT INTO Velden (blockseq,fieldseq,veld,bits,decimaal,tekst,hex) VALUES (?,?,?,?,?,?,?)");
    $self->{sth_fld1} = $dbh->prepare("SELECT decimaal from Velden where veld=? and blockseq=?");
    $self->{dbh} = $dbh;
    bless $self;
    return $self;
  }

  sub getvalue {
    my $self = shift;
    my $veld = shift;
    my $blockseq = shift;

    my $sth=$self->{sth_fld1};
    $sth->execute($veld,$blockseq);
    my @row=$sth->fetchrow_array;
    return $row[0];
  }

  sub fillheader {
    my $self =shift;
    my $jrumsgid = shift;
    my $hdr = shift;
    my $type = shift;

    $self->{dbh}->begin_work;  
    $self->{sth_hdr}->execute($jrumsgid,$type);
    $self->{sth_blkid}->execute;
    my @rows = $self->{sth_blkid}->fetchrow_array;
    my $hdrmsgid=$rows[0];
    $self->{dbh}->commit;

    my $fields = $hdr->{Fields};
    my $nrfields = $hdr->{nrfields};
    if (defined($nrfields)) {
      for (my $i=0;$i<$nrfields;$i++) {
        my $fld=$fields->[$i];
        my $field=$fld->{Field};
        my $fieldseq=$fld->{Fieldseq};
        my $decimal=$fld->{Decimal};
        my $bits=$fld->{Bits};
        my $text=$fld->{Text};
        my $heks=$fld->{Hex};
        unless ($self->{sth_fld}->execute($hdrmsgid,$fieldseq,$field,$bits,$decimal,$text,$heks)) {
          print Dumper($fld);
          print "fillheader: jrumsgid = $jrumsgid, hdrmsgid = $hdrmsgid, fieldseq=$fieldseq\n";
#          getc();
        }
      }
    }
    return $hdrmsgid;
  }

  sub fillpacket {
    my $self =shift;
    my $jrumsgid = shift;
    my $hdr = shift;
    my $packetnr = shift;

    $self->{dbh}->begin_work;  
    $self->{sth_pkt}->execute($jrumsgid,$packetnr);
    $self->{sth_blkid}->execute;
    my @rows = $self->{sth_blkid}->fetchrow_array;
    my $hdrmsgid=$rows[0];
    $self->{dbh}->commit;

    my $fields = $hdr->{Fields};
    my $nrfields = $hdr->{nrfields};
    for (my $i=0;$i<$nrfields;$i++) {
      my $fld=$fields->[$i];
      my $field=$fld->{Field};
      my $fieldseq=$fld->{Fieldseq};
      my $decimal=$fld->{Decimal};
      my $bits=$fld->{Bits};
      my $text=$fld->{Text};
      my $heks=$fld->{Hex};
      unless ($self->{sth_fld}->execute($hdrmsgid,$fieldseq,$field,$bits,$decimal,$text,$heks)) {
        print Dumper($fld);
        print "fillpacket: jrumsgid = $jrumsgid, hdrmsgid = $hdrmsgid, fieldseq=$fieldseq\n";
#        getc();
        }
    }
    return $hdrmsgid;
  }

  sub addmessage {
    my ($self,$message,$parent) = @_;
    my $trainid=$self->{trainid};
    my $dbh=$self->{dbh};
    
    $self->{dbh}->begin_work;  
    $self->{sth_msg2}->execute($parent,$message->{BitMessage},$message->{Hexstream},$message->{MessageType},$trainid);
    $self->{sth_msgid}->execute;
    my @rows = $self->{sth_msgid}->fetchrow_array;
    my $jrumsgid=$rows[0];
#    $self->{sth_msgup1}->execute($jrumsgid);
    $self->{dbh}->commit;
    
# hack hack Balises hebben headers RBC berichten niet dus daarom dit twee keer
    my $hdr;
    if (exists($message->{Header})) {
	$hdr = $self->fillheader($jrumsgid,$message->{Header},'Header') ;
    } else {
	$hdr = $self->fillheader($jrumsgid,$message,'Header') ;
    }

    my $packs=$message->{Packets};
    for (my $i=0;exists($packs->[$i]);$i++) {
      my $pkt = $packs->[$i];
      $self->fillpacket($jrumsgid,$pkt,$pkt->{Packetnr}) unless $pkt->{Fields}->[0]->{Decimal} == 255;
    }
    $self->{sth_msgup2}->execute($jrumsgid);
    return $jrumsgid;
  }
  
  sub store {
    my ($self, $message) = @_;
    my $trainid=$self->{trainid};
    my $dbh=$self->{dbh};
    my ($dts,$tts,$jaar,$maand,$dag,$uur,$min,$sec);
    
#    print Dumper($message);
    $self->{dbh}->begin_work;  
    $self->{sth_msg1}->execute($message->{Bitstream},$message->{Hexstream},$message->{MessageType},$trainid);
    $self->{sth_msgid}->execute;
    my @rows = $self->{sth_msgid}->fetchrow_array;
    my $jrumsgid=$rows[0];
    $self->{dbh}->commit;
    my $hdr = $self->fillheader($jrumsgid,$message->{Header},'Header');
    $jaar  = $self->getvalue('JRU_YEAR',$hdr);
    if (defined($jaar)) {
      $jaar += 2000;
      $jaar -= 100 if $jaar >= 2072;
      $maand = $self->getvalue('JRU_MONTH',$hdr);
      $dag   = $self->getvalue('JRU_DAY',$hdr);
      $uur   = $self->getvalue('JRU_HOUR',$hdr);
      $min   = $self->getvalue('JRU_MINUTES',$hdr);
      $sec   = $self->getvalue('JRU_SECONDS',$hdr);
      $tts   = $self->getvalue('JRU_TTS',$hdr);
      $dts = sprintf("%4.4d-%2.2d-%2.2d %2.2d:%2.2d:%2.2d",$jaar,$maand,$dag,$uur,$min,$sec);
    } else {
      $jaar  = $self->getvalue('DRU_YEAR',$hdr);
      if (defined($jaar)) {
        $jaar += 2000;
        $jaar -= 100 if $jaar >= 2072;
        if (defined($jaar)) {
          $maand = $self->getvalue('DRU_MONTH',$hdr);
          $dag   = $self->getvalue('DRU_DAY',$hdr);
          $uur   = $self->getvalue('DRU_HOUR',$hdr);
          $min   = $self->getvalue('DRU_MINUTES',$hdr);
          $sec   = $self->getvalue('DRU_SECONDS',$hdr);
          $tts   = $self->getvalue('DRU_TTS',$hdr);
          $dts = sprintf("%4.4d-%2.2d-%2.2d %2.2d:%2.2d:%2.2d",$jaar,$maand,$dag,$uur,$min,$sec);
        }
      }
    }
    $self->{sth_msgup}->execute($dts,$tts,$jrumsgid) if defined($dts);
    $self->{sth_msgup2}->execute($jrumsgid);


    my @messages = ('BaliseTelegram',
                    'MessageFromRBC',
                    'MessageToRBC');
    
    foreach my $msgt (@messages) {
      if (exists($message->{$msgt})) {
        my $tgmmsg = $self->addmessage($message->{$msgt},$jrumsgid);
        $self->{sth_msgup}->execute($dts,$tts,$tgmmsg)if defined($dts);
      }
    }
    
    my @simplemsg = ('EmergencyBrake',
                     'ServiceBrake',
                     'PlainTextMessage',
                     'PredefinedTextMessage',
                     'IndicationsToDriver',
                     'DriversActions',
                     'DataEntryCompleted',
                     'PermittedSpeed',
                     'MRSPSpeed',
                     'TargetSpeed',
                     'TargetDistance',
                     'ReleaseSpeed',
                     'Warning',
                     'STMSelected',
                     'Events',
                     'BaliseGroupError',
                     'ETCSID',
                     'StmInformation',
                     'JRUStateMsg',
                     'RadioLinkSupervisionError',
                     'ModeChange',
                     'LevelChange',
                     'OdometryCalibration',
                     'Encoded');

    foreach my $msgt (@simplemsg) {
      if (exists($message->{$msgt})) {
        $self->fillheader($jrumsgid,$message->{$msgt},$msgt);
      }
    }
  }
}

1;