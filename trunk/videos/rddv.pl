#! /usr/bin/perl -w

use strict;

use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");
my $sth_in = $dbh->prepare("INSERT INTO videos (tijdstip,directory,naam,duur,aspect,dvdnr) VALUES (?,?,?,?,?,?)");
my $sth_upasp = $dbh->prepare("UPDATE videos set aspect=? where directory=? and naam=?");
my $sth_sel = $dbh->prepare("SELECT aspect from videos where directory=? and naam=?");

my $dir = "/data/HomeVideos2/OudeJVC";

opendir (DIR1,$dir) || die "Kan $dir niet openen\n";
my @dirs = grep { /^[0-9]+/ } readdir(DIR1);
closedir(DIR1);

print join(",",@dirs),"\n";

foreach my $d (sort @dirs) {
    opendir (DIR,"$dir/$d") || die "Kan $dir/$d niet openen\n";
    my @files = grep { /.dv$/ } readdir(DIR);
    print join(",",@files),"\n";
    closedir(DIR);
    foreach my $f (sort @files) {
	$f =~ m/(...)-(....).(..).(..)_(..)-(..)-(..).dv/;
	my $film=$1;
	my $jaar=$2;
	my $maand=$3;
	my $dag=$4;
	my $uur=$5;
	my $minuut=$6;
	my $sec=$7;
 
	my $fn = "$dir/$d/$f";
	my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,$atime,$mtime,$ctime,$blksize,$blocks) = stat($fn);
	my $durat = $size/3600000.0;
	my $aspect = "4:3";
#	$f =~ s/.dv$//;
	$sth_sel->execute($d,$f);
	unless (my @row = $sth_sel->fetchrow_array()) {
	    $sth_in->execute("$jaar-$maand-$dag $uur:$minuut:$sec",$d,$f,$durat,$aspect,-$film);
	    print "$d $f $jaar $maand $dag $uur $minuut $sec $durat $aspect\n";
	}
    }
}
