#! /usr/bin/perl -w

use strict;

use MIME::Base64 qw(encode_base64);
use Net::FTP;
use Net::SMTP;

my $tcstr = "f:/tcp_mt_pem_endurance/filelogging";


sub zendmail {

  my $file=shift;

  my $mail_server = 'van-loon.xs4all.nl';
  #my $mail_server = 'localhost';
  my $mail_from = "michiel.vanloon\@prorail.nl";
  my $mail_to = "michiel.vanloon\@prorail.nl";


  if (my $smtp = Net::SMTP->new($mail_server,Hello=>'kbvmonitor.tpc.maastricht',Debug=>0,Timeout=>30)) {
    $smtp->mail($mail_from);
    $smtp->recipient('mfvl','mfvl.ns');
#    $smtp->recipient('mfvl');

    $smtp->data();
    my $mailhead = <<EOM;
To: $mail_to
From: michiel\@van-loon.xs4all.nl
Subject: Archive: $file
Message-ID: <$$.$file\@kbvmonitor.tpc.maastricht>
Content-Type: application/zip
Content-Transfer-Encoding: base64
Content-Disposition: attachment; filename="$file"

EOM

    $smtp->datasend($mailhead);
    open FILE, "<$file";
    binmode FILE;

    local($/) = undef;

    $smtp->datasend(encode_base64(<FILE>));
    $smtp->dataend();
    $smtp->quit;

    close FILE;
  } else {
    print LOGFILE "Can't connect : $@\n";
  }
}


sub  haal_visfiles {

  my $name = shift;
  my $ip = shift;
  my $nu = shift;
  my $gist = shift;

  mkdir $name;
  chdir $name;
  if (my $ftp = Net::FTP->new($ip,Debug=>0) ) {
    $ftp->login('vis','vijver');
    $ftp->cwd('VIS_2_1/LOG');
    $ftp->binary;
    if ($ftp->cwd($nu)) {
      mkdir $nu;
      chdir $nu;

      my @files=grep (/^\d\d\.LOG/ , $ftp->ls());
      for my $f (@files) {
        $ftp->get($f);
      }
      $ftp->cdup;
      chdir("..");
    }

    if ($ftp->cwd($gist)) {
      mkdir $gist;
      chdir $gist;

      my @files=grep (/^\d\d\.LOG/ , $ftp->ls());
      for my $f (@files) {
        $ftp->get($f);
      }
      $ftp->cdup;
      chdir("..");
    }
    $ftp->quit;
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
  if (my $ftp = Net::FTP->new($ip,Debug=>0) ) {
    $ftp->login('kbvmgr','kbvmgr');
    $ftp->cwd('DATA');
    $ftp->ascii;
    $ftp->get("KBVLOG_".$eer.".LOG");
    $ftp->get("KBVLOG_".$gist.".LOG");
    $ftp->get("KBVLOG_".$nu.".LOG");
    $ftp->quit;
  }
  chdir("..");
}


mkdir "D:/temp/zendlogs";
chdir "D:/temp/zendlogs";

my  $nu=time();

my @tijd = localtime($nu);
my @gist = localtime($nu-86400);
my @eer = localtime($nu-2*86400);

my $datstr = sprintf "%4.4d%2.2d%2.2d",$tijd[5]+1900,$tijd[4]+1,$tijd[3];
print $datstr,"\n";

my $gisstr = sprintf "%4.4d%2.2d%2.2d",$gist[5]+1900,$gist[4]+1,$gist[3];
print $gisstr,"\n";

my $eerstr = sprintf "%4.4d%2.2d%2.2d",$eer[5]+1900,$eer[4]+1,$eer[3];
print $eerstr,"\n";

open LOGFILE,">test.log";
my $tijd=localtime($nu);
print LOGFILE "Run gestart op ",$tijd,"\n";

mkdir $datstr;
chdir $datstr;

haal_visfiles('VISA','176.176.176.1',$datstr,$gisstr);
haal_visfiles('VISB','176.176.176.2',$datstr,$gisstr);
haal_kbvfiles('KBVA','176.176.176.11',$datstr,$gisstr,$eerstr);
haal_kbvfiles('KBVB','176.176.176.13',$datstr,$gisstr,$eerstr);

chdir("D:/temp/zendlogs");

system("d:/zip231xn/zip -9mr $datstr $datstr $tcstr");
#system("C:/Program\ Files/Internet\ Explorer/IEXPLORE.EXE");
#sleep(30);

my $filenaam="$datstr.zip";
zendmail($filenaam);

system("kill iexplore.exe");

close LOGFILE;