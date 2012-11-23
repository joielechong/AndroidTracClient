#! /usr/bin/perl -w

use strict;

use Gedcom;
use DBI;
use Data::Dumper;

sub printveld {
    my $s="";
    my $r=shift;
    if (defined($r)) {
	my $x=shift;
	my $v = $r->get_value($x);
	$s .=$v if defined($v);
    }
    my $null = shift;
    return NULL unless defined($null)l
    return $s;
}

sub printnaam {
    my $name =shift;
    $name =~ s:/::g;
    $name =~ s/ $//g;
    return $name;
}

my $dbh = DBI->connect_cached("dbi:Pg:dbname=mfvl");
$dbh->do("SET search_path TO gedcom");
my $sthbi=$dbh->prepare("INSERT INTO bronnen (filename) values (?)") or die $dbh->errstr;
my $sthbs=$dbh->prepare("SELECT id,filename,type FROM bronnen WHERE filename=?") or die $dbh->errstr;
my $sthiu=$dbh->prepare("UPDATE individuals set updated=false where bron=?") or die $dbh->errstr;
my $sthii=$dbh->prepare("INSERT INTO individuals (bron,xref,name,sex,birthdate,birthplace,chrdate,chrplace,deathdate,deathplace,burialdate,burialplace) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)") or die $dbh->errstr;
my $sthis=$dbh->prepare("SELECT xref,id,name,sex,birthdate,birthplace,chrdate,chrplace,deathdate,deathplace,burialdate,burialplace FROM individuals WHERE bron=?") or die $dbh->errstr;

my $filename=shift;
$sthbs->execute($filename);
my $row=$sthbs->fetch();
unless (defined($$row[0])) {
    $sthbi->execute($filename);
    $sthbs->execute($filename);
    $row=$sthbs->fetch();
}
my $bronid=$$row[0];
my $brontype=$$row[2];
$brontype='' unless defined $brontype;

$sthiu->execute($bronid);
$sthis->execute($bronid);

my $geda=Gedcom->new($filename);

my @inda = $geda->individuals();

for my $i (@inda) {
    my $name=printnaam($i->name);
    print "\"$name\",";
    my @b=$i->birth;
    if ($#b >= 0) {
	print printveld($b[0],"date"),",";
	my $p=printveld($b[0],"place");
	print "\"$p\"" if $p ne "";
	print ",";
    } else {
	print ",,";
    }
    my @c=$i->chr;
    if ($#c >= 0) {
	print printveld($c[0],"date"),",";
	my $p=printveld($c[0],"place");
	print "\"$p\"" if $p ne "";
	print ",";
    } else {
	print ",,";
    }
    my @d=$i->death;
    if ($#d >= 0) {
	print printveld($d[0],"date"),",";
	my $p=printveld($d[0],"place");
	print "\"$p\"" if $p ne "";
	print ",";
    } else {
	print ",,";
    }
    my @u=$i->burial;
    if ($#u >= 0) {
	print printveld($u[0],"date"),",";
	my $p=printveld($u[0],"place");
	print "\"$p\"" if $p ne "";
	print ",";
    } else {
	print ",,";
    }
    my $f=$i->father;
    print "\"",printnaam($f->name),"\"" if defined($f);
    print ",";
    my $m=$i->mother;
    print "\"",printnaam($m->name),"\"" if defined($m);
    print ","; #,$i->xref;
    $sthii->execute($bronid,$i->xref,$i->name,$i->sex,printveld($b[0],"date",1),printveld($b[0],"place",1),printveld($c[0],"date",1),printveld($c[0],"place",1),printveld($d[0],"date",1),printveld($d[0],"place",1),printveld($u[0],"date",1),printveld($u[0],"place",1));


    print"\n";
}
