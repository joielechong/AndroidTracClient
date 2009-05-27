#! /usr/bin/perl -w

use strict;
use DBI;

my %velden;

$velden{"Datum & tijd"} = "tijd";
$velden{"Datum"} = "tijd";
$velden{"Type"} = "type";
$velden{"Nummer"} = "nummer";
$velden{"Bedrag"} = "bedrag";
$velden{"Duur"} = "duur";
$velden{"datum & tijd"} = "tijd";
$velden{"datum"} = "tijd";
$velden{"type"} = "type";
$velden{"nummer"} = "nummer";
$velden{"bedrag"} = "bedrag";
$velden{"duur"} = "duur";
$velden{"MB(s)"} = "MB";
$velden{"Bundel"} = "bundel";

my $defaulttype = "GPRS dataverkeer";

my $directory="/network/THUIS/MICHIEL/C/My Documents/ict/Uren en kosten";

my $dbh=DBI->connect("dbi:Pg:dbname=mfvl") or die "$@";

opendir (DIR, $directory) || die "Kan directory \"$directory\" niet openen\n";

my @files=grep { /^telfort-rekening-.*\.csv/ && -f "$directory/$_"} readdir(DIR);
closedir DIR;

foreach my $file (@files) {
    print $file,"\n";
    open FILE,"<$directory/$file" or die "Kan file \"$directory/$file\" niet openen\n";
    my @veldnamen;
    my $typeincl=0;
    my $sth;
    while (<FILE>) {
	chomp;
	next if length == 0;
	my @fields = split(/;/); 
	if ($fields[0] =~ /^Datum/) {
#	    print $_,"\n";
	    @veldnamen=map {$velden{$_}} @fields;
	    $typeincl = grep {/type/} @veldnamen;
#	    print join("<>",@veldnamen),"<-->$typeincl\n";
	    if ($typeincl==0) {
		push @veldnamen,$velden{"Type"};
	    }
	    my $cmd = "INSERT into telfort (".join(",",@veldnamen).") VALUES (";
	    foreach (@veldnamen) {
		$cmd .= "?,";
	    }
	    $cmd =~ s/,$/\)/;
	    print $cmd,"\n";
	    $sth = $dbh->prepare($cmd) or die $@;
	} elsif ($fields[0] =~ /^datum/) {
#	    print $_,"\n";
	    @veldnamen=map {$velden{$_}} @fields;
	    push @veldnamen,$velden{"Bundel"};
	    $typeincl = grep {/type/} @veldnamen;
#	    print join("<>",@veldnamen),"<-->$typeincl\n";
	    if ($typeincl==0) {
		push @veldnamen,$velden{"Type"};
	    }
	    my $cmd = "INSERT into telfort (".join(",",@veldnamen).") VALUES (";
	    foreach (@veldnamen) {
		$cmd .= "?,";
	    }
	    $cmd =~ s/,$/\)/;
#	    print $cmd,"\n";
	    $sth = $dbh->prepare($cmd) or die $@;
	} else {
#	    print $_," -- ",$#veldnamen," -- ",$#fields," -- ",$typeincl,"\n";
	    if ($typeincl==1) {
		if ($#veldnamen != $#fields) {
		    push @fields,"N";
		}
	    } else {
		if ($#veldnamen != ($#fields+1)) {
		    push @fields,"N";
		}
		push @fields,$defaulttype;
	    }
	    for (my $i=0;$i<=$#veldnamen;$i++) {
		if ($veldnamen[$i] eq "tijd") {
		    $fields[$i] =~ s:\.:/:g;
		}
		if ($veldnamen[$i] eq "bedrag") {
		    $fields[$i] =~ s:\,:.:g;
		    $fields[$i] *= 100;
		}
		if ($veldnamen[$i] eq "MB") {
		    $fields[$i] =~ s:\,:.:g;
		}
		if ($veldnamen[$i] eq "duur") {
		    my ($min,$sec) = split(/:/,$fields[$i]);
		    $fields[$i] = 60*$min+$sec;
		}
		if ($veldnamen[$i] eq "bundel") {
		    $fields[$i] = "Y" if $fields[$i] eq "J";
		}
#		print $veldnamen[$i]," -- ", $fields[$i],"\n";
	    }
	    $sth->execute(@fields);
	}
    }
}
