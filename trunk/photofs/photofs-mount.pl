#! /usr/bin/perl
# /*************************************************************************** 
#  *   photofs -- 
#  *                                                                         * 
#  *   This program is free software; you can redistribute it and/or modify  * 
#  *   it under the terms of the GNU General Public License as published by  * 
#  *   the Free Software Foundation; either version 2 of the License, or     * 
#  *   (at your option) any later version.                                   * 
#  *                                                                         * 
#  *   This program is distributed in the hope that it will be useful,       * 
#  *   but WITHOUT ANY WARRANTY; without even the implied warranty of        * 
#  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         * 
#  *   GNU General Public License for more details.                          * 
#  *                                                                         * 
#  *   You should have received a copy of the GNU General Public License     * 
#  *   along with this program; if not, write to the                         * 
#  *   Free Software Foundation, Inc.,                                       * 
#  *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             * 
#  *                                                                         * 
#  *   Licensed under the GNU GPL                                            * 
#  ***************************************************************************/ 

# Script to mount jpg and other photo files as a virtual filesystem.
# Created from tagfs-mount.pl
# Created from loopback.pl example by Romain Beauxis <toots@rastageeks.org>

# Usage: photofs-mount  /path/to/mount/point


if ($#ARGV + 1 != 1 ) {
    die "Usage: photofs-mount /path/to/mount/point";
}

my $mountpoint = shift;

use strict;
use Fuse;
use IO::File;
use POSIX qw(ENOENT ENOSYS EEXIST EPERM O_RDONLY O_RDWR O_APPEND O_CREAT);
use DBI;
use File::Basename;
use strict;
use Data::Dumper;

my @filecache;

sub get_real_file {
    my $file = shift;
    my ($id,$format);
    if ($file =~ /\.[jJ][Pp][Gg]$/i || $file =~ /\.[jJ][Pp][eE][Gg]$/i || $file =~ /\.[Bb][Mm][Pp]$/i || $file =~ /\.[Gg][Ii][Ff]$/i) {
	$file =~ /^\/.*\/(\d*) - .*\.(.*)$/;
	($id,$format) = ($1,$2);
        if (defined($filecache[$id])) {
            return "/data/".$filecache[$id];
        } else {
            my $dbh = connection();
            my $sth = $dbh->prepare(qq{SELECT filename FROM fotos WHERE id = ?});
            $sth->execute($id);
            my @row = $sth->fetchrow_array;
            return "/data/".shift(@row);
        }
    }
    return "/";
}

sub connection {
    return DBI->connect("dbi:Pg:dbname=httpd host=localhost","mfvl","gotect03");
}

sub getFiles_datum {
    my ($path,$dbh) = @_;
    my (@files,$sth);
    
    if ($path eq "") {
	$sth = $dbh->prepare(qq{SELECT DISTINCT year(datum) FROM fotos WHERE NOT datum IS NULL});
	$sth->execute();
	while ( my @row = $sth->fetchrow_array ) {
	    push(@files,shift(@row));
	}
	push (@files,"onbekend");
    } elsif ($path eq "/onbekend") {
	$sth = $dbh->prepare(qq{SELECT DISTINCT id,filename FROM fotos WHERE datum IS NULL});
	$sth->execute();
	while ( my @row = $sth->fetchrow_array ) {
            $filecache[$row[0]] = $row[1];
	    my ($f,$dir,$suff) = fileparse($row[1]);
	    my $fakefile = $row[0]." - ".$f.$suff;
	    push(@files,$fakefile);
	}        
    } elsif ($path =~ /^\/\d+$/) {
	$path =~s/^\///;
	$sth = $dbh->prepare(qq{SELECT DISTINCT month(datum) FROM fotos WHERE datum like ?});
	$sth->execute("$path-%");
	while ( my @row = $sth->fetchrow_array ) {
	    push(@files,shift(@row));
	}        
    } elsif ($path =~ /^\/(\d+)\/(\d+)$/) {
        my $jaar = $1;
        my $maand = $2;
        $sth = $dbh->prepare(qq{SELECT DISTINCT day(datum) FROM fotos WHERE datum like ?});
	$sth->execute(sprintf("%4.4d-%2.2d-%%",$jaar,$maand));
	while ( my @row = $sth->fetchrow_array ) {
	    push(@files,shift(@row));
	}        
    } elsif ($path =~ /^\/(\d+)\/(\d+)\/(\d+)$/) {
        my $jaar = $1;
        my $maand = $2;
        my $dag = $3;
        $sth = $dbh->prepare(qq{SELECT DISTINCT id,filename FROM fotos WHERE datum=?});
	$sth->execute(sprintf("%4.4d-%2.2d-%2.2d",$jaar,$maand,$dag));
	while ( my @row = $sth->fetchrow_array ) {
            $filecache[$row[0]] = $row[1];
	    my ($f,$dir,$suff) = fileparse($row[1]);
	    my $fakefile = $row[0]." - ".$f.$suff;
	    push(@files,$fakefile);
	}        
    }
    return @files;
}

sub getFiles_album {
    my ($path,$dbh) = @_;
    my (@files,$sth);
    
    if ($path eq "") {
	$sth = $dbh->prepare(qq{SELECT DISTINCT naam FROM album });
	$sth->execute();
	while ( my @row = $sth->fetchrow_array ) {
	    push(@files,shift(@row));
	}
    } else {
	$path =~s/^\///;
	$sth = $dbh->prepare(qq{SELECT fotoid,filename from album,albumfoto,fotos WHERE album.naam=? and album.id=albumid and fotoid=fotos.id});
	$sth->execute($path);
	while ( my @row = $sth->fetchrow_array) {
            $filecache[$row[0]] = $row[1];
	    my ($f,$dir,$suff) = fileparse($row[1]);
	    my $fakefile = $row[0]." - ".$f.$suff;
	    push(@files,$fakefile);
	}
    }
    return @files;
}

sub getFiles_auteur {
    my ($path,$dbh) = @_;
    my (@files,$sth);

    if ($path eq "") {
	$sth = $dbh->prepare(qq{SELECT DISTINCT auteur FROM fotos WHERE NOT auteur IS NULL});
	$sth->execute();
	while ( my @row = $sth->fetchrow_array ) {
	    push(@files,shift(@row));
	}
    } else {
	$path =~s/^\///;
	$sth = $dbh->prepare(qq{SELECT id,filename from fotos WHERE auteur=?});
	$sth->execute($path);
	while ( my @row = $sth->fetchrow_array) {
            $filecache[$row[0]] = $row[1];
	    my ($f,$dir,$suff) = fileparse($row[1]);
	    my $fakefile = $row[0]." - ".$f.$suff;
	    push(@files,$fakefile);
	}
    }
    return @files;
}

my @directories = ("album","auteur","datum",0);

no strict 'refs';

sub tagsfs_getdir {
    my $path = shift;
    $path =~ s/\/$//;
    my (@files,$sth);
    if ($path eq "") {
	return @directories;
    }
    my $dbh = connection();
    foreach my $dir (@directories) {
	if ($path =~ /^\/$dir/) {
	    $path =~ s/^\/$dir//;
	    my $sub = "getFiles_$dir";
	    @files = &$sub($path,$dbh);
	}
    }
    return (@files,0);
}

use strict 'refs';
    
sub tagsfs_getattr {
    my $fake_file = shift;
    my ($file) = get_real_file($fake_file);
#    print STDERR "getattr file = $file\n";
    my (@list) = lstat($file);
    return -$! unless @list;
    if ($file eq "/") {
	$list[2] = "16749";
    } else {
	$list[2] = "33060";
    }
    return @list;
}


sub tagsfs_open {
    my ($file) = get_real_file(shift);
    my ($mode) = shift;
    return -$! unless sysopen(FILE,$file,$mode);
    close(FILE);
    return 0;
}

sub tagsfs_read {
    my ($file,$bufsize,$off) = @_;
    my ($rv) = -ENOSYS();
    my ($handle) = new IO::File;
    return -ENOENT() unless -e ($file = get_real_file($file));
    my ($fsize) = -s $file;
    return -ENOSYS() unless open($handle,$file);
    if(seek($handle,$off,SEEK_SET)) {
	read($handle,$rv,$bufsize);
    }
    return $rv;
}

# kludge
sub tagsfs_statfs {return 255,1,0,1,0,4096}

fork and exit;

# Execute main on child process
Fuse::main(
	   mountpoint=>$mountpoint,
	   getattr=>\&tagsfs_getattr,
	   getdir=>\&tagsfs_getdir,
	   open=>\&tagsfs_open,
	   statfs=>\&tagsfs_statfs,
	   read=>\&tagsfs_read,
	   mountopts=>"allow_other,ro",
           debug=>0,
	   );
