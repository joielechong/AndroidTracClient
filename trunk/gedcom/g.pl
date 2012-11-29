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
    return undef if defined($null) and ($s eq "");
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
my $sthis=$dbh->prepare("SELECT xref,id,name,sex,birthdate,birthplace,chrdate,chrplace,deathdate,deathplace,burialdate,burialplace FROM individuals WHERE bron=? ORDER BY xref") or die $dbh->errstr;

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
my @fama = $geda->families();

for my $i (@inda) {
    my $name=printnaam($i->name);
    print "\"$name\",";
    
    my $bstr = "";
    my @b=$i->birth;
    if ($#b >= 0) {
	$bstr .= printveld($b[0],"date");
        $bstr .= ",";
	my $p=printveld($b[0],"place");
	$bstr .= "\"$p\"" if $p ne "";
	$bstr .=  ",";
    } else {
	$bstr .= ",,";
    }
    
    my $cstr = "";
    my @c=$i->chr;
    if ($#c >= 0) {
	$cstr .= printveld($c[0],"date");
        $cstr .= ",";
	my $p=printveld($c[0],"place");
	$cstr .= "\"$p\"" if $p ne "";
	$cstr .= ",";
    } else {
	$cstr .= ",,";
    }
    $bstr =",," if lc($bstr) eq lc($cstr);
    print $bstr;
    print $cstr;
    
    my $dstr = "";
    my @d=$i->death;
    if ($#d >= 0) {
	$dstr .= printveld($d[0],"date");
        $dstr .= ",";
	my $p=printveld($d[0],"place");
	$dstr .= "\"$p\"" if $p ne "";
	$dstr .= ",";
    } else {
	$dstr .= ",,";
    }
    
    my $ustr = "";
    my @u=$i->burial;
    if ($#u >= 0) {
	$ustr .= printveld($u[0],"date");
        $ustr .= ",";
	my $p=printveld($u[0],"place");
	$ustr .= "\"$p\"" if $p ne "";
	$ustr .= ",";
    } else {
	$ustr .= ",,";
    }
    $dstr =",," if lc($dstr) eq lc($ustr);
    print $dstr;
    print $ustr;

    my $f=$i->father;
    print "\"",printnaam($f->name),"\"" if defined($f);
    print ",";
    my $m=$i->mother;
    print "\"",printnaam($m->name),"\"" if defined($m);
    print ","; #,$i->xref;
#    $sthii->execute($bronid,$i->xref,$i->name,$i->sex,printveld($b[0],"date",1),printveld($b[0],"place",1),printveld($c[0],"date",1),printveld($c[0],"place",1),printveld($d[0],"date",1),printveld($d[0],"place",1),printveld($u[0],"date",1),printveld($u[0],"place",1));


    print"\n";
}
my @fama = $geda->families();
for my $f (@inda) {
    my $husb=printnaam($f->husband);
    my $wife=printnaam($f->wife);
    my $xref = $f->xref;
    print "\"$xref: $husb, $wife\",";
}