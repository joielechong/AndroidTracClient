#! /usr/bin/perl -w

use strict;

use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");
my $sth_in = $dbh->prepare("INSERT INTO videos (tijdstip,directory,naam,duur,aspect) VALUES (?,?,?,?,?)");
my $sth_upasp = $dbh->prepare("UPDATE videos set aspect=? where directory=? and naam=?");
my $sth_sel = $dbh->prepare("SELECT aspect from videos where directory=? and naam=?");

my $dir = "/data/JVC-20060819/SD_VIDEO/";

opendir (DIR1,$dir) || die "Kan $dir niet openen\n";
my @dirs = grep { /^PRG/ } readdir(DIR1);
closedir(DIR1);

#print join(",",@dirs),"\n";

foreach my $d (sort @dirs) {
    opendir (DIR,"$dir/$d") || die "Kan $dir/$d niet openen\n";
    my @files = grep { /.MOI$/ } readdir(DIR);
#    print join(",",@files),"\n";
    closedir(DIR);
    foreach my $f (sort @files) {
	my $buffer;
	my $fn = "$dir/$d/$f";
	open F,"<$fn" or die "Kan $fn niet openen\n";
	binmode F;
	read F,$buffer,0x100;
	close F;
	if (substr($buffer,0,2) eq "V6") {
#	    print "$fn we zitten goed\n";
	    my $jaar = unpack("n",substr($buffer,6,2));
	    my $maand = unpack("C",substr($buffer,8,1));
	    my $dag = unpack("C",substr($buffer,9,1));
	    my $uur = unpack("C",substr($buffer,10,1));
	    my $minuut = unpack("C",substr($buffer,11,1));
	    my $sec = unpack("n",substr($buffer,12,2))/1000.0;
	    my $durat = unpack("N",substr($buffer,14,4))/1000.0;
	    my $aspect = unpack("C",substr($buffer,0x80,1));
	    if ($aspect == 0x55) {
		$aspect = "16:9";
	    } else {
		$aspect = "4:3";
	    }
	    $f =~ s/.MOI$//;
	    $sth_sel->execute($d,$f);
	    unless (my @row = $sth_sel->fetchrow_array()) {
		$sth_in->execute("$jaar-$maand-$dag $uur:$minuut:$sec",$d,$f,$durat,$aspect);
		print "$d $f $jaar $maand $dag $uur $minuut $sec $durat $aspect\n";
	    }
	}
    }
}
