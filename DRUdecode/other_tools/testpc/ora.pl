#! /usr/bin/perl -w

use strict;
use DBI;

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
  print join(", ",@row),"\n";
  $sth_pi->execute(@row);
}



$dbh_p->disconnect;
$dbh_o->disconnect;
