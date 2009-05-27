#! /usr/bin/perl -w

use strict;
use Xbase;

my $dbase_root="/p233/c/dbase/geld";

my @rekening;

my $rekening=new Xbase;

$rekening->open_dbf("$dbase_root/rekening.dbf");

$rekening->go_top;

while (! $rekening->eof ) {
    my @record=$rekening->get_record;
    $rekening[$record[1]]=$record[0];
    $rekening->go_next;
}
$rekening->close_dbf;

my $aandeel=new Xbase;

$aandeel->open_dbf("$dbase_root/aandeel.dbf");
$aandeel->dbf_stat;
$aandeel->go_top;

my @aandeel;
{
    my @tmp_aandeel=();

    while (! $aandeel->eof ) {
	my @record=$aandeel->get_record;
	push @tmp_aandeel,join(":",@record);
	$aandeel->go_next;
    }
    @aandeel=sort(@tmp_aandeel);
}
$aandeel->close_dbf;

my $koers=new Xbase;
$koers->open_dbf("$dbase_root/koers.dbf");
$koers->dbf_stat;
$koers->go_top;

my @koers;

{
    my @tmp_koers=();

    while (! $koers->eof ) {
	my @record=$koers->get_record;
	push @tmp_koers,join(":",$record[0],$record[2],$record[1]);
	$koers->go_next;
    }
    @koers=sort(@tmp_koers);
}
$koers->close_dbf;

my $spaar=new Xbase;
$spaar->open_dbf("$dbase_root/spaar.dbf");
$spaar->dbf_stat;
$spaar->go_top;

my @spaar;

{
    my @tmp_spaar=();

    while (! $spaar->eof ) {
	my @record=$spaar->get_record;
	push @tmp_spaar,join(":",@record);
	$spaar->go_next;
    }
    @spaar=sort(@tmp_spaar);
}
$spaar->close_dbf;

my $giro=new Xbase;
$giro->open_dbf("$dbase_root/giro.dbf");
$giro->dbf_stat;
$giro->go_top;

my @giro;

{
    my @tmp_giro=();

    while (! $giro->eof ) {
	my @record=$giro->get_record;
	push @tmp_giro,join(":",$record[0],$record[5],$record[6]);
	$giro->go_next;
    }
    @giro=sort(@tmp_giro);
}
$giro->close_dbf;

