#! /usr/bin/perl -w

use strict;
use File::Find ();
use DBI;



my $dbh =  DBI->connect("dbi:Pg:dbname=mh") or die "Cannot connect: ".$DBI::errstr;
my $sth = $dbh->prepare("insert into tclog values (?,?,?,?,?)");

# Set the variable $File::Find::dont_use_nlink if you're using AFS,
# since AFS cheats.

# for the convenience of &wanted calls, including -eval statements:
use vars qw/*name *dir *prune/;
*name   = *File::Find::name;
*dir    = *File::Find::dir;
*prune  = *File::Find::prune;

sub wanted;
sub doexec ($@);


use Cwd ();
my $cwd = Cwd::cwd();


# Traverse desired filesystems
File::Find::find({wanted => \&wanted}, '/nb1404/d/offline/Testomgevingen/Maastricht-Heerlen/Uitgevoerde testen/2005 Endurance/tcp_mt_pem_endurance/filelogging/');
exit;


sub wanted {
    /^Event200.*\z/s &&
    doexec($name);
}


sub doinsert {
    $_[0] =~ s/\'/:/g;
    $_[0] =~ s/_/ /g;
    print join(", ",@_),"\n";
    my $ts=shift;
    my $system=shift;
    my $code=shift;
    my $type="";
    $type = shift if $system ne "LOG";
    my $tekst=join(" ",@_);
	
    $sth->execute($ts,$system,$code,$type,$tekst);
}

sub doexec ($@) {

    my $file = shift;
    print "=========================== $file ========================\n\n";
    sleep 5;
    open FILE, "<$file" or die "Cannot open $file, $!\n";

    my $x=<FILE>;
    while (<FILE>) {
	my @fields = split;  
	if ($fields[1] eq "LOG" ) {
	    if ($fields[2] == 98 || $fields[2]==51 || $fields[2]==40) { 
		doinsert(@fields);
	    }
	} else {
	    doinsert(@fields);
	}
    }
    close FILE;
}

$dbh->disconnect;
