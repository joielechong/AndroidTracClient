#! /usr/bin/perl -w

use strict;
use XML::Simple;
use Data::Dumper;
use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=croon");

my $sth1 = $dbh->prepare("DELETE FROM pkmimport");
my $sth2 = $dbh->prepare("INSERT INTO pkmimport (soort,obs,eistype,eis,eistekst,aantoonmoment,bewijsmethode,bewijsdocument,paragraaf,status) VALUES(?,?,?,?,?,?,?,?,?,?)");
my $sth3 = $dbh->prepare("INSERT INTO unieke_eisen SELECT * FROM eis_import WHERE NOT eis IN (SELECT eis FROM unieke_eisen)");
my $sth4 = $dbh->prepare("INSERT INTO eis_di SELECT i.eis,i.di,i.status from eisdi_import as i left join eis_di as e on (e.eis=i.eis and e.di=i.di) where e.eis is null");
my $sth5 = $dbh->prepare("UPDATE unieke_eisen AS u SET eistekst=i.eistekst,soort=i.soort,eistype=i.eistype,status=i.status FROM eis_import AS i WHERE i.eis=u.eis");
my $sth6 = $dbh->prepare("UPDATE eis_di as d SET status=i.status FROM eisdi_import as i WHERE i.eis=d.eis and i.di=d.di;");
my $sth7 = $dbh->prepare("UPDATE unieke_eisen SET status='vervallen',eistekst='Eis is vervallen' WHERE eistekst IS NULL");
my $sth8 = $dbh->prepare("INSERT INTO vmxov SELECT i.eis,i.di,i.status,i.moment_van_aantonen,i.bewijsvoeringsmethode,i.bewijsdocument,i.paragraaf from vmxov_import as i left join vmxov as e on (e.eis=i.eis and e.di=i.di) where e.eis is null and i.moment_van_aantonen is not null");
my $sth9 = $dbh->prepare("UPDATE vmxov as d SET status=i.status,moment_van_aantonen=i.moment_van_aantonen,bewijsvoeringsmethode=i.bewijsvoeringsmethode,bewijsdocument=i.bewijsdocument,paragraaf=i.paragraaf FROM vmxov_import as i WHERE i.eis=d.eis and i.di=d.di and i.moment_van_aantonen is not null");

my $status = shift;

my $dir = shift;
die "Aanroep:   pkmimport <status> <import dirnaam>\n" unless defined($dir) && defined($status);

$sth1->execute();
my $rv = $sth1->rows();
print "$rv rows verwijderd uit pkmimport\n";

opendir(my $dh,$dir) || die "opendir faalt\n";
while(readdir($dh)) {
#$_ = "Checklist NUR VTTI 10.xml";
    next unless /\.xml$/;
    print $_," ";
    my $ref = XMLin("$dir/$_");
#print Dumper $ref;
    my $rows = $ref->{Worksheet}->{Table}->{Row};
    my $checklist = ($ref->{Worksheet}->{Table}->{"ss:ExpandedColumnCount"} == 43);
    print $#$rows," ";
    print $ref->{Worksheet}->{Table}->{"ss:ExpandedColumnCount"}," ";
    print $ref->{Worksheet}->{Table}->{"ss:ExpandedRowCount"},"\n";
    my $data=0;
    for (my $row=0;$row<=$#$rows;$row++) {
	my $cells = $rows->[$row]->{Cell};
	if (ref $cells eq "ARRAY") {
	    if ($data && defined($cells->[3]->{Data}->{content})) {
		$sth2->execute(($checklist ? "checklist": "vmx"),
			       $cells->[2]->{Data}->{content},
			       $cells->[3]->{Data}->{content},
			       $cells->[7]->{Data}->{content},
			       $cells->[8]->{Data}->{content},
			       $cells->[14]->{Data}->{content},
			       $cells->[15]->{Data}->{content},
			       $cells->[28]->{Data}->{content},
			       $cells->[30]->{Data}->{content},
			       $status);
		if ($sth2->err) {
		    print $sth2->errstr,"\n";
		    print $row," ",1+$#$cells," ",($checklist ? "checklist": "vmx")," ";
		    print $cells->[2]->{Data}->{content}," ";
		    print $cells->[3]->{Data}->{content}," ";
		    print $cells->[7]->{Data}->{content}," ";
		    print $cells->[8]->{Data}->{content}," ";
		    print $cells->[14]->{Data}->{content}," ";
		    print $cells->[15]->{Data}->{content}," ";
		    print $cells->[28]->{Data}->{content}," ";
		    print $cells->[30]->{Data}->{content}," ";
		    print "\n";
		}
	    }
	} else {
	    $data=1 if (exists($cells->{Data}->{content}) &&$cells->{Data}->{content} eq "__DATA_BELOW");
#	    print $row," 0\n";
	}
    }
}
closedir($dh);

$sth3->execute();
$rv = $sth3->rows();
print "$rv rows toegevoegd aan unieke_eisen\n";
$sth4->execute();
$rv = $sth4->rows();
print "$rv rows toegevoegd aan eis_di\n";
$sth5->execute();
$rv = $sth5->rows();
print "$rv rows aangepast in unieke_eisen\n";
$sth6->execute();
$rv = $sth6->rows();
print "$rv rows aangepast in eis_di\n";
$sth7->execute();
$rv = $sth7->rows();
print "$rv eisen vervallen\n";
$sth8->execute();
$rv = $sth8->rows();
print "$rv rows toegevoegd aan vmxov\n";
$sth9->execute();
$rv = $sth9->rows();
print "$rv rows aangepast in vmxov\n";
