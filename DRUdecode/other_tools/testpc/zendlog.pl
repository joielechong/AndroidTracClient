#! /usr/bin/perl -w

use strict;
use DBI;
use MIME::Base64 qw(encode_base64);
use Net::FTP;
use Net::SMTP;
use Net::Telnet;
use POSIX qw(strftime);

sub logit {
  my $s = shift;
  my $tijd = strftime "%Y-%m-%d %H:%M:%S",localtime();

  print LOGFILE $tijd," ", $s;
  print STDERR $tijd," ",$s;
}

my $tcstr = "g:/tcp_mt_pem_endurance/filelogging";

sub call_vis {
  my $naam=shift;
  my $ip=shift;
  my $datstr=shift;

  my $ftp= new Net::FTP($ip,timeout=>10);
  $ftp->login('vis','vijver');
  $ftp->cwd('vis_2_1/bin');

  open T,">temp.tmp";
  print T "set pagesize 73\n";
  print T "set linesize 150\n";
  print T "set markup html on\n";
  print T "spool ".$naam."_sql_$datstr.html\n";
  print T "set termout off\n";
  print T "\@show_all\n";
  print T "quit\n";
  close T;

  $ftp->ascii;
  $ftp->put('temp.tmp');
  $ftp->quit;

  unlink('temp.tmp');

  my $t = new Net::Telnet(errmode=>'return');
  if ($t->open($ip)) {
    $t->login('vis','vijver');
    my @lines = $t->cmd('set def [.vis_2_1.bin]');
    for (@lines) {
	logit($_);
    }
    logit "Start van download uit MS database van $naam\n";
    @lines = $t->cmd('sqlplus vis/vis @temp.tmp');
    for (@lines) {
	logit($_);
    }
    logit "Einde van download uit MS database van $naam\n";
    @lines = $t->cmd('purge temp.tmp');
    for (@lines) {
	logit($_);
    }
    @lines = $t->print("log");
    for (@lines) {
	logit($_);
    }
    $t->close;
  } else {
    logit "Kan geen telnet verbinding maken met $naam: ".$t->errmsg()."\n";
  }
}

sub zendmail {

  my $file=shift;

  my $mail_server = 'van-loon.xs4all.nl';
  #my $mail_server = 'localhost';
  my $mail_from = "michiel.vanloon\@prorail.nl";
  my $mail_to = "michiel.vanloon\@prorail.nl";


  if (open FILE, "<$file") {
      binmode FILE;
      
      if (my $smtp = Net::SMTP->new($mail_server,Hello=>'kbvmonitor.tpc.maastricht',Timeout=>30)) {
	  $smtp->mail($mail_from);
	  $smtp->recipient('mfvl','mfvl.ns');
#    $smtp->recipient('mfvl');
	  
	  $smtp->data();
	  my $mailhead = <<EOM;
To: $mail_to
From: $mail_from
Subject: Archive: $file
Message-ID: <$$.$file\@kbvmonitor.tpc.maastricht>
Content-Type: application/zip
Content-Transfer-Encoding: base64
Content-Disposition: attachment; filename="$file"

EOM
;
	  local($/) = undef;

          $smtp->datasend($mailhead);
	  logit "==============Mail verzenden file $file via $mail_server:\n";
	  logit $mailhead;
	  logit "==============Einde mailheader\n";
	  
	  $smtp->datasend(encode_base64(<FILE>));
	  $smtp->dataend();
	  $smtp->quit;
	  logit "Mail verzonden\n";
	  
	  close FILE;
      } else {
	  logit "Kan geen verbinding maken met : $@\n";
      }
  } else {
      logit "Kan $file niet openen: $!\n";
  }
}

sub  haal_visfiles {

  my $name = shift;
  my $ip = shift;
  my $nu = shift;
  my $gist = shift;

  mkdir $name;
  chdir $name;
  if (my $ftp = Net::FTP->new($ip,timeout=>10) ) {
    $ftp->login('vis','vijver');
    $ftp->cwd('VIS_2_1/LOG');
    $ftp->binary;
    if ($ftp->cwd($nu)) {
      mkdir $nu;
      chdir $nu;

      my @files=grep (/^\d\d\.LOG/ , $ftp->ls());
      for my $f (@files) {
        $f=~s/;1//;
        if ($ftp->get($f)) {
          logit "Downloaded: $name $nu $f\n";
        } else {
          logit "Download mislukt: $name $nu $f\n";
        }
      }
      $ftp->cdup;
      chdir("..");
    }

    if ($ftp->cwd($gist)) {
      mkdir $gist;
      chdir $gist;

      my @files=grep (/^\d\d\.LOG/ , $ftp->ls());
      for my $f (@files) {
        $f=~s/;1//;
        if ($ftp->get($f)) {
          logit "Downloaded: $name $gist $f\n";
        } else {
          logit "Download mislukt: $name $gist $f\n";
        }
      }
      $ftp->cdup;
      chdir("..");
    }
    $ftp->cdup;
    $ftp->cwd('bin');
    $ftp->ascii;
    my $logfile = $name."_sql_$nu.html";
    if ($ftp->get($logfile)) {
      $ftp->delete($logfile.";0");
      logit "Downloaded: $logfile\n";
      $ftp->delete('temp.tmp;0');
    } else {
      logit "Download mislukt: $logfile\n";
    }
    $ftp->quit;
  } else {
    logit "Kan geen verbinding maken met $name: $@\n";
  }
  chdir("..");
}

sub  haal_kbvfiles {
  my $name = shift;
  my $ip = shift;
  my $nu = shift;
  my $gist = shift;
  my $eer = shift;

  mkdir $name;
  chdir $name;
  if (my $ftp = Net::FTP->new($ip,timeout=>10) ) {
    $ftp->login('kbvmgr','kbvmgr');
    $ftp->cwd('DATA');
    $ftp->ascii;
    if ($ftp->get("KBVLOG_".$eer.".LOG")) {
      logit "Downloaded: $name KBVLOG_$eer.LOG\n";
    } else {
      logit "Download mislukt: $name KBVLOG_$eer.LOG\n";
    }
    if ($ftp->get("KBVLOG_".$gist.".LOG")) {
      logit "Downloaded: $name KBVLOG_$gist.LOG\n";
    } else {
      logit "Download mislukt: $name KBVLOG_$gist.LOG\n";
    }
    if ($ftp->get("KBVLOG_".$nu.".LOG")) {
      logit "Downloaded: $name KBVLOG_$nu.LOG\n";
    } else {
      logit "Download mislukt: $name KBVLOG_$nu.LOG\n";
    }
    $ftp->quit;
  } else {
    logit "Kan geen verbinding maken met $name: $@\n";
  }
  chdir("..");
}


sub voer_commando_uit {
  my $commando = shift;

  if (open PIJP,"$commando |") {
    logit "==========Uitvoer van commando: $commando=============\n";

    while (<PIJP>) {
      logit $_;
    }
    close PIJP;
    logit "==========Einde uitvoer==========\n\n";
  } else {
    logit "Kan commando niet uitvoeren: $commando\n";
  }
}
  

sub haal_updates {
  my $server = shift;

  mkdir ("d:/temp/receive");
  chdir ("d:/temp/receive");

  if (my $ftp = Net::FTP->new($server,timeout=>10)) {
    $ftp->login('testpc','testpc');
    $ftp->cwd('upload/ascii');
    $ftp->ascii;

    my @files = $ftp->ls();
    for my $f (@files) {
      if ($ftp->get($f)) {
        my $logmes="Uploaded and ";
        unless ($ftp->delete($f)) {
          $logmes .=  "not ";
        }
        logit $logmes."deleted: ascii/$f\n";
      }
    }

    $ftp->cdup;
    $ftp->cwd('bin');
    $ftp->binary;

    @files=$ftp->ls();
    for my $f (@files) {
      if ($ftp->get($f)) {
        my $logmes="Uploaded and ";
        unless ($ftp->delete($f)) {
          $logmes .=  "not ";
        }
        logit $logmes."deleted: bin/$f\n";
      }
    }

    $ftp->quit;
  } else {
    logit "Kan niet verbinden met $server: $@\n";
  }
}

mkdir "D:/temp/zendlogs";
chdir "D:/temp/zendlogs";

my  $nu=time();

my @tijd = localtime($nu);
my @gist = localtime($nu-86400);
my @eer = localtime($nu-2*86400);

my $datstr = sprintf "%4.4d%2.2d%2.2d",$tijd[5]+1900,$tijd[4]+1,$tijd[3];
my $gisstr = sprintf "%4.4d%2.2d%2.2d",$gist[5]+1900,$gist[4]+1,$gist[3];
my $eerstr = sprintf "%4.4d%2.2d%2.2d",$eer[5]+1900,$eer[4]+1,$eer[3];

open LOGFILE,">>zendlog.log";
my $tijd=localtime($nu);
logit "Run gestart\n";

voer_commando_uit('net use g: \\\\176.176.176.101\\testconductor /user:administrator ""');

mkdir $datstr;
chdir $datstr;

call_vis('VISA','176.176.176.1',$datstr);
call_vis('VISB','176.176.176.2',$datstr);

haal_visfiles('VISA','176.176.176.1',$datstr,$gisstr);
haal_visfiles('VISB','176.176.176.2',$datstr,$gisstr);
haal_kbvfiles('KBVA','176.176.176.11',$datstr,$gisstr,$eerstr);
haal_kbvfiles('KBVB','176.176.176.13',$datstr,$gisstr,$eerstr);

chdir("D:/temp/zendlogs");

voer_commando_uit("d:/zip231xn/zip -9mr $datstr $datstr $tcstr");

my $filenaam="$datstr.zip";
zendmail($filenaam);

voer_commando_uit("kill iexplore.exe");
voer_commando_uit("net use g: /delete");

haal_updates("van-loon.xs4all.nl");

logit "Kopieer database van VISA naar eigen database\n";

my $dbh_o=DBI->connect("dbi:Oracle:host=176.176.176.1;port=1521;sid=vis",'vis/vis') or die $!;

my $dbh_p=DBI->connect("dbi:PgPP:dbname=mh;host=van-loon.xs4all.nl","testpc","testpc") or die $!;

my $sth_os=$dbh_o->prepare("select * from table_of_vis_alarms where datetime_of_recording_in_vis >= ?");

my $sth_ps=$dbh_p->prepare("select max(datetime_of_recording_in_vis) from visdb");
my $sth_pd=$dbh_p->prepare("delete from visdb where datetime_of_recording_in_vis >= ?");
my $sth_pi=$dbh_p->prepare("insert into visdb values (?,?,?,?,?,?,?,?,?)");

$sth_ps->execute;
my @datum=$sth_ps->fetchrow_array;

$sth_pd->execute(@datum);

$sth_os->execute(@datum);

while (my @row=$sth_os->fetchrow_array) {
#  print join(", ",@row),"\n";
  $sth_pi->execute(@row);
}

$dbh_p->disconnect;
$dbh_o->disconnect;

logit "Run gestopt\n";

close LOGFILE;
