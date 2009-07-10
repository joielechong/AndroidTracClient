#! /usr/bin/perl -w

use strict;
use DBI;
use Data::Dumper;
use CGI;

my $q = new CGI;
my $file = $q->param('upload');
if (defined($file)) {

  print $q->header(-expires=>'-1h');
  print $q->start_html("Importeren ING CSV file");
  my $dbh = DBI->connect("dbi:Pg:dbname=httpd user=mfvl");
  my $sth2=$dbh->prepare("SELECT * FROM nieuwegiro");
  my $sth3=$dbh->prepare("INSERT INTO giro SELECT * FROM nieuwegiro");
  my $sth4=$dbh->prepare("SELECT * FROM ntb");

  my $canwrite = open OUT, ">/home/mfvl/download/girotel/$file";
  if ($canwrite) { 
      print "Kan file $file wegschrijven<p>\n";
  } else {
      print "Kan file $file <b>niet</b> wegschrijven<p>\n";
  }
  my $fh = $q->upload('upload')->handle;
  $dbh->{AutoCommit} = 0;
  $dbh->{RaiseError} = 1;
  eval {
      $dbh->do("DELETE FROM girotmp2");
      $dbh->do("COPY girotmp2 FROM STDIN WITH CSV HEADER QUOTE AS '\"'");
      while (<$fh>) {
	  $dbh->pg_putcopydata($_);
	  print OUT $_ if $canwrite;
      }      
      $dbh->pg_putcopyend();
      close OUT if $canwrite;

      print $q->h3("Ingelezen records");
      print $q->start_table;
      $sth2->execute();
      while (my @row=$sth2->fetchrow_array()) {
	  print "<tr><td>".join("</td><td>",@row)."</td></tr>\n";
      }
      print $q->end_table;
      $sth3->execute();
      $dbh->commit;
  };
  if ( $@ ) {
      warn "Transactie afgebroken wegens $@";
      eval { $dbh->rollback};
  }
  print $q->h3("Niet geclassificeerde records");
  print $q->start_table;
  $sth4->execute();
  while (my @row=$sth4->fetchrow_array()) {
      print "<tr><td>".join("</td><td>",@row)."</td></tr>\n";
  }
  print $q->end_table;
  print $q->end_html;
  $dbh->commit;
  $dbh->disconnect;
  
} else {
  print $q->header;
  print $q->start_html("Giro upload");
  print $q->start_multipart_form();
  print $q->filefield(-name=>'upload',-size=>50);
  print $q->submit(-name=>'Verzenden');
  print $q->endform();
  print $q->end_html();
}
