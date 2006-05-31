#! /usr/bin/perl -w
use strict;
use File::Find ();
use vislog;

sub dovislog;

my $dir = shift;

# Set the variable $File::Find::dont_use_nlink if you're using AFS,
# since AFS cheats.

# for the convenience of &wanted calls, including -eval statements:
use vars qw/*name *dir *prune/;
*name   = *File::Find::name;
*dir    = *File::Find::dir;
*prune  = *File::Find::prune;

sub wanted;



# Traverse desired filesystems
File::Find::find({wanted => \&wanted}, $dir);
exit;


sub wanted {
    /^\d\d\.[lL][oO][gG](;1)?$/ && dovislog("$name");
}

sub dovislog {
	my $name = shift;
	unless (-e "$name.xml") {

	my $curdir;
        chomp($curdir=`cd`);
	chdir ("d:/src/vislog");
	print "$name\n";
	vislog "$name";
	chdir ($curdir);
        }
}
