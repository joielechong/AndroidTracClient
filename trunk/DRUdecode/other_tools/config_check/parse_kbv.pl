#! /usr/local/bin/perl -w

use strict;
use XML::Parser;
use XML::Parser::Style::Subs;
use DBI;
use Data::Dumper;

sub SQLExec {
    my ($dbh,$string) = @_;
    
#    print $string,"\n";
    $dbh->do($string);
}

my $filename = shift;

#my $p1 = XML::Parser->new(Style=>'Tree');
#open TEST,">xml-tree.txt";
#my $ref1= $p1->parsefile($filename);
#print TEST Dumper($ref1);
#close TEST;
#exit();

my $currentVerbinding=1;
my $currentSpoortak=0;
my $currentDetector=0;
my $lastUndefTag="";

my $p = XML::Parser->new(Style=>'Subs', Pkg=>'ParseKBV');
$p->setHandlers('Char'=>\&ParseKBV::_CatchText);
$p->setHandlers('Start'=>\&ParseKBV::_Start);
my $dbh = DBI->connect("DBI:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=BB21.mdb") or die "cannot open database\n";

my $rv;

my @table_names = $dbh->tables(undef,undef,undef,"TABLE",{ 
  ado_columns => 1, ado_trim_catalog => 0, ado_filter => q{TABLE_NAME LIKE 'KBV%'}, }
);
#my @table_names = $dbh->tables;

foreach my $t (@table_names) {
	$rv=SQLExec($dbh,"DELETE FROM ".$t);
}

SQLExec($dbh,"ALTER TABLE kbv_detector ADD detectornr INTEGER");
SQLExec($dbh,"ALTER TABLE kbv_verbinding ADD id INTEGER");
SQLExec($dbh,"ALTER TABLE kbv_spoortak ADD spoortaknr INTEGER");

my $ref= $p->parsefile($filename);

SQLExec($dbh,"ALTER TABLE kbv_detector DROP detectornr");
SQLExec($dbh,"ALTER TABLE kbv_verbinding DROP id");
SQLExec($dbh,"ALTER TABLE kbv_spoortak DROP spoortaknr");

{
    package ParseKBV;
    
    my $text;
    my $reftext = \$text;
    my $currentMeldingbetekeniscode;
    my $currentKBVmeldingid;
    my $currentTable="";
    my $currentKey="";
    my $currentRow=-1;
    my $currentPrefix="";
    
    sub _Start {
        my $ret=XML::Parser::Style::Subs::Start(@_);
        &_UndefinedTag(@_) unless defined $ret;
    }
    
    sub _CatchText {
	my ($expat,$string) = @_;
	$$reftext .= $string;
    }
    
    sub _SQL {
	my $SQLcmd = shift;
	my $rows = ::SQLExec($dbh,$SQLcmd);
	unless (defined($rows)) {
	    print STDERR "fout bij uitvoeren van $SQLcmd\n   ".$dbh->errstr; exit(4);
	}
    }
    
    sub _Insert {
	my ($table,$fieldRef,$valueRef) = @_;
	
	my $SQLcmd = "INSERT INTO $table (".join(",",@$fieldRef).") VALUES (".join(",",@$valueRef).")";
	_SQL($SQLcmd);
    }	
    
    sub _Update {
	my ($table,$element,$key,$argRef) = @_;
	if ($table ne "") {
	    my $SQLcmd = "UPDATE $table set ";
	    while ($#$argRef> 0) {
		$SQLcmd .= $currentPrefix.$element."_".shift(@$argRef). "='".shift(@$argRef)."'";
		$SQLcmd .="," if $#$argRef > 0;
	    }
	    $SQLcmd .= " WHERE ( $key )" if $key ne "";
	    _SQL($SQLcmd);
	}
    }
    
    sub Detectormelding {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    my $veld=shift(@args);
	    push (@fields,$veld);
	    my $text = shift(@args);
#	    $text =~ s/'/'''/g;
	    push (@values,'"'.$text.'"');
	}
        $currentKey=$fields[$#fields]."=".$values[$#values];
        $currentKey =~ s/\"//g;
	_Insert("KBV_Detectormelding",\@fields,\@values);
	$currentTable="KBV_Detectormelding";
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Detectormelding_ {
	$currentTable ="";
	$currentKey="";
    }
    
    sub ADRdeviceid {
 	my ($expat,$element, @args) = @_;
        unless ($currentTable eq "") {
            _Update($currentTable,$element,$currentKey,\@args);
        }
    }
  
    sub Melding {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    my $veld=shift(@args);
	    push (@fields,$veld);
	    my $text = shift(@args);
	    $currentMeldingbetekeniscode = $text if ($veld eq "Meldingbetekeniscode");
#	    $text =~ s/'/'''/g;
	    push (@values,'"'.$text.'"');
	}
	_Insert("KBV_Melding",\@fields,\@values);
    }
    
    sub ParameterORC {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	push (@fields,"Meldingbetekeniscode");
	push (@values,"'".$currentMeldingbetekeniscode."'");
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Melding_ParameterORC",\@fields,\@values);
    }
    
    sub BronParameter {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	push (@fields,"Meldingbetekeniscode");
	push (@values,"'".$currentMeldingbetekeniscode."'");
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Melding_BronParameter",\@fields,\@values);
    }
    
  
    sub Meldingtrigger {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Meldingtrigger",\@fields,\@values);
    }
    
    sub SSRAlarm {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_SSRAlarm",\@fields,\@values);
    }
    
    sub SSRBron {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_SSRBron",\@fields,\@values);
    }
    
    sub Antwoordtekst {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Antwoordtekst",\@fields,\@values);
    }
    
    sub Antwoordreden {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Antwoordreden",\@fields,\@values);
    }
    
    sub BorderSafetyProvision {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	    $currentKey=$fields[$#fields]."=".$values[$#values];
	}
	_Insert("KBV_BorderSafetyProvision",\@fields,\@values);
	$currentTable="KBV_BorderSafetyProvision";
	$currentRow = -1;
	$currentPrefix="";
    }
    
    sub BorderSafetyProvision_ {
	$currentTable ="";
	$currentKey="";
    }
    
    sub Aarde {
        my ($expat,$element,@args) = @_;
        my @fields=();
        my @values =();
        while ($#args > 0) {
                push (@fields,shift(@args));
                push (@values,"'".shift(@args)."'");
                $currentKey=$fields[$#fields]."=".$values[$#values];
        }
        _Insert("KBV_Aarde",\@fields,\@values);
	$currentTable="KBV_Aarde";
	$currentRow = -1;
	$currentPrefix="";
    }
    
    sub Aarde_ {
	$currentTable ="";
	$currentKey="";
    }
    
    sub Detector {
        my ($expat,$element,@args) = @_;
        my @fields=();
        my @values =();
        $currentDetector++;
        $currentKey="DetectorNr=$currentDetector";
        push(@fields,"DetectorNr");
        push(@values,$currentDetector);
        while ($#args > 0) {
                push (@fields,shift(@args));
                push (@values,"'".shift(@args)."'");
        }
        _Insert("KBV_Detector",\@fields,\@values);
	$currentTable="KBV_Detector";
	$currentRow = -1;
	$currentPrefix="";
    }
    
    sub Detector_ {
	$currentTable ="";
	$currentKey="";
    }
    
    sub Additionaldetectorid {
	my ($expat,$element, @args) = @_;
        unless ($currentTable eq "") {
            _Update($currentTable,$element,$currentKey,\@args);
        }
    }
#	my @fields=();
#	my @values=();
#	while ($#args> 0) {
#	    push (@fields,shift(@args));
#	    push (@values,"'".shift(@args)."'");
#	}
#	_Insert("KBV_Detector",\@fields,\@values);
#        }

    sub Dienstoverpad {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	_Insert("KBV_Dienstoverpad",\@fields,\@values);
	$currentTable="KBV_Dienstoverpad";
    }

    sub Dienstoverpad_ {
	$currentTable ="";
	$currentKey="";
    }

    sub Route {
	my ($expat,$element, @args) = @_;
	$currentTable="KBV_Route";
    }

    sub Route_ {
      $currentTable="";
      $currentKey="";
    }

    sub RouteidPRL {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	my @keylist=();
	while ($#args> 0) {
	    my $field=shift(@args);
          my $value=shift(@args);
	    push (@fields,$field);
	    push (@values,"'".$value."'");
	    push (@keylist,"$field='$value'");
	}
	$currentKey=join(" AND ",@keylist);
	_Insert($currentTable,\@fields,\@values);
    }

    sub Beginsein {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Eindsein {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub InfraSegment {
    }

    sub InfraSegment_ {
	$currentRow=-1;
    }
 
    sub StartPositie {
	$currentPrefix .= "StartPositie_";
    }

    sub StartPositie_ {
	$currentPrefix =~ s/StartPositie_//;
    }

    sub EindPositie {
	$currentPrefix .= "EindPositie_";
    }

    sub EindPositie_ {
	$currentPrefix =~ s/EindPositie_//;
	$currentRow=0;
    }

    sub Sein {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Sein";
	_Insert($currentTable,\@fields,\@values);
	$currentPrefix="";
	$currentRow=-1;
    }

    sub Sein_ {
	$currentTable="";
	$currentKey="";
    }

    sub SeinidBev21 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Afrijdsectie {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Voorsectie {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Seinidbev21 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub XGRContact {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Seinpositie {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
	$currentPrefix = "Seinpositie_";
    }

    sub Seinpositie_ {
	$currentPrefix="";
    }

    sub VHR_B_deel {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	my @keylist = split(" AND ",$currentKey);
	foreach my $key (@keylist) {
	    my ($keyfield,$keyval)=split("=",$key);
	    push (@fields,$keyfield);
	    push (@values,$keyval);
      }
	$currentTable .= "_VHR_B" unless $currentTable =~/.*_VHR_B/;
	$currentRow++;
	push (@fields,'Row');
	push (@values, "'".$currentRow."'");
	_Insert($currentTable,\@fields,\@values);
    }

    sub VHR_B_deel_ {
	$currentTable =~s/_VHR_B//;
    }

    sub VHR_E_deel {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	my @keylist = split(" AND ",$currentKey);
	foreach my $key (@keylist) {
	    my ($keyfield,$keyval)=split("=",$key);
	    push (@fields,$keyfield);
	    push (@values,$keyval);
      }
	$currentTable .= "_VHR_E" unless $currentTable =~/.*_VHR_E/;
	$currentRow++;
	push (@fields,'Row');
	push (@values, "'".$currentRow."'");
	_Insert($currentTable,\@fields,\@values);
    }

    sub VHR_E_deel_ { 
	$currentTable =~s/_VHR_E//;
   }

    sub Spoortak {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	$currentSpoortak++;
	$currentKey="SpoortakNr=$currentSpoortak";
	push(@fields,"SpoortakNr");
	push(@values,"'".$currentSpoortak."'");
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentTable="KBV_Spoortak";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Spoortak_ {
	$currentTable="";
    }

    sub Knoop1 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Knoop2 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Spoortakid {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Sectie {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Sectie";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = 0;
	$currentPrefix="";
    }

    sub Sectie_ {
	$currentTable="";
	$currentKey="";
	$currentRow = -1;
    }


    sub PositieOpSpoortak {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }

    sub Spoortakdeel {
	my ($expat,$element, @args) = @_;
	if ($currentTable eq "KBV_Verbinding") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	} else {
	    my @fields=();
	    my @values=();
	    while ($#args> 0) {
		push (@fields,shift(@args));
		push (@values,"'".shift(@args)."'");
	    }
	    my @keylist = split(" AND ",$currentKey);
	    foreach my $key (@keylist) {
		my ($keyfield,$keyval)=split("=",$key);
		push (@fields,$keyfield);
		push (@values,$keyval);
	    }
	    $currentTable .= "_Spoortakdeel" unless $currentTable =~/.*_Spoortakdeel/;
	    $currentRow++;
	    push (@fields,'Row');
	    push (@values, "'".$currentRow."'");
	    _Insert($currentTable,\@fields,\@values);
	}
    }

    sub Spoortakdeel_ {
	$currentTable =~s/_Spoortakdeel//;
    }
    
    sub SpoortakDeel {
	my ($expat,$element, @args) = @_;
	Spoortakdeel($expat,$element,@args);
    }

    sub SpoortakDeel_ {
	Spoortakdeel_
    }

    sub SpoortakIdBev21 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    my $cond = $currentKey;
	    $cond .= " AND Row = $currentRow" unless $currentRow eq '-1';
	    _Update($currentTable,$element,$cond,\@args);
	}
    }
    
    sub Knoopid {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	_Insert("KBV_Knoop",\@fields,\@values);
    }
    
    sub KnoopIdBev21 {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }
    
    sub Areaid {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }
    
    sub VertragingSein {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }

    sub Stroomvoorziening {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Stroomvoorziening";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Stroomvoorziening_ {
	$currentTable="";
    }

    sub CodegeverSeinverlichting {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_CodegeverSeinverlichting";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub CodegeverSeinverlichting_ {
	$currentTable="";
    }

    sub Seinverlichting {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Seinverlichting";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Seinverlichting_ {
	$currentTable="";
    }

    sub Noncoreelementid {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }

    sub Tunnelschuif {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Tunnelschuif";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Tunnelschuif_ {
	$currentTable="";
    }

    sub VasteKruising {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_VasteKruising";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub VasteKruising_ {
	$currentTable="";
    }

    sub Spoortakdeel1 {
	$currentPrefix = "Spoortakdeel1_";
    }
    
    sub Spoortakdeel1_ {
	$currentPrefix="";
    }
    
    sub Spoortakdeel2 {
	$currentPrefix = "Spoortakdeel2_";
    }
    
    sub Spoortakdeel2_ {
	$currentPrefix="";
    }
    
    sub Verbinding {
	$currentTable="KBV_Verbinding";
	my @fields=();
	my @values=();
	$currentVerbinding++;
	$currentKey="id=$currentVerbinding";
	push(@fields,"id");
	push(@values,"'".$currentVerbinding."'");
	_Insert($currentTable,\@fields,\@values);
	$currentRow=-1;
	$currentPrefix="";
    }

    sub Verbinding_ {
	$currentTable="";
    }


    sub Steller {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }

    sub Stand {
	$$reftext="";
    }

    sub Stand_ {
	my ($expat,$element) = @_;
	my @args=();
	push (@args,"Value");
	push (@args,$$reftext);
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }

    sub VrijgaveRangeren {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_VrijgaveRangeren";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub VrijgaveRangeren_ {
	$currentTable="";
    }

    sub Werkzone {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Werkzone";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Werkzone_ {
	$currentTable="";
    }

    sub Wissel {
	my ($expat,$element, @args) = @_;
	my @fields=();
	my @values=();
	while ($#args> 0) {
	    push (@fields,shift(@args));
	    push (@values,"'".shift(@args)."'");
	}
	$currentKey=$fields[$#fields]."=".$values[$#values];
	$currentTable="KBV_Wissel";
	_Insert($currentTable,\@fields,\@values);
	$currentRow = -1;
	$currentPrefix="";
    }

    sub Wissel_ {
	$currentTable="";
    }

    sub GewoneWisselTakDelen {
	$currentPrefix="GewoneWisselTakDelen_";
    }

    sub GewoneWisselTakDelen_ {
	$currentPrefix="";
    }

    sub Stellerid {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
    }

    sub WisselTakdeelLinks {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
	$currentPrefix .= "WisselTakdeelLinks_";
    }

    sub WisselTakdeelLinks_ {
	$currentPrefix =~ s/WisselTakdeelLinks_//;
    }

    sub WisselTakdeelRechts {
	my ($expat,$element, @args) = @_;
	unless ($currentTable eq "") {
	    _Update($currentTable,$element,$currentKey,\@args);
	}
	$currentPrefix .= "WisselTakdeelRechts_";
    }

    sub WisselTakdeelRechts_ {
	$currentPrefix =~ s/WisselTakdeelRechts_//;
    }
    
    sub _UndefinedTag {
	my ($expat,$element, @args) = @_;
        if ($element ne $lastUndefTag) {
            print "Undefined start van tag gevonden: $element\n";
            $lastUndefTag=$element;
        }
    }

}
