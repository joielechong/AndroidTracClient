#! /usr/bin/perl -w

if ($#ARGV + 1 != 1 ) {
    die "Usage: koersen_mount.pl /path/to/mount/point";
}

my $mountpoint = shift;

use strict;
use Fuse;
use IO::File;
use POSIX qw(ENOENT ENOSYS EEXIST EINVAL EISDIR EPERM O_RDONLY O_RDWR O_APPEND O_CREAT);
use Fcntl ':mode';
use DBI;
use File::Basename;
use strict;
use Data::Dumper;

my @directories = ("fonds","datum",".","..",0);
my %filecache;
my $dbh;
my ($sth_year,$sth_month,$sth_day,$sth_naam,$sth_idn,$sth_idd,$sth_fnaam,$sth_fdatum);
my $sth1;
my $sth2;
my $sth3;

sub getFiles_datum {
    my $path = shift;
    my @files;
    
    if ($path eq "") {
		$sth_year->execute();
		while ( my @row = $sth_year->fetchrow_array ) {
			push(@files,shift(@row));
		}
    } elsif ($path =~ /^\/\d+$/) {
		$path =~s/^\///;
		$sth_month->execute("$path-%");
		while ( my @row = $sth_month->fetchrow_array ) {
			push(@files,shift(@row));
		}        
    } elsif ($path =~ /^\/(\d+)\/(\d+)$/) {
        my $jaar = $1;
        my $maand = $2;
		$sth_day->execute(sprintf("%4.4d-%2.2d-%%",$jaar,$maand));
		while ( my @row = $sth_day->fetchrow_array ) {
			my $datum = sprintf("%4.4d-%2.2d-%2.2d",$jaar,$maand,shift(@row));
			push(@files,$datum.".csv");
#	    push(@files,$datum.".html");
		}        
    }
    push @files,".";
    push @files,"..";
    return @files;
}

sub getFiles_fonds {
    my $path = shift;
    my @files;

    if ($path eq "") {
	$sth_naam->execute();
	while ( my @row = $sth_naam->fetchrow_array ) {
	    my $naam=shift(@row);
	    push(@files,$naam.".csv");
#	    push(@files,$naam.".html");
	}
    }
    push @files,".";
    push @files,"..";
    return @files;
}

no strict 'refs';

sub tagsfs_getdir {
    my $path = shift;
#    print STDERR "tagsfs_getdir path = $path\n";

    $path =~ s/\/$//;
    my (@files,$sth);
    if ($path eq "") {
	return @directories;
    }
    foreach my $dir (@directories) {
	next if ($dir eq "." || $dir eq "..");
	if ($path =~ /^\/$dir/) {
	    $path =~ s/^\/$dir//;
	    my $sub = "getFiles_$dir";
	    @files = &$sub($path,$dbh);
	}
    }
    return (@files,0);
}

sub is_file {
    my $file = shift;
#    return ($file =~ /\.csv$/i || $file =~ /\.html$/i);
    return ($file =~ /\.csv$/i);
}

sub load_file {
	my $fake_file = shift;
	return 0 if defined $filecache{$fake_file};
	
    my $ishtml = 0;#($fake_file =~ /\.html$/);
    my $iscsv = ($fake_file =~ /\.csv$/);
	return -ENOENT() unless ($ishtml || $iscsv);
	
    my @row;
    my $file = $fake_file;
    my $datastore;
    my $fields;
	
    $fake_file =~ s/\.(csv|html)$//;
#    print STDERR "fake_file = $fake_file\n";

    if ($fake_file =~ /^\/fonds\//) {
		$fake_file =~ s/^\/fonds\///;
#		print STDERR "fake_file = $fake_file\n";
		$sth_idn->execute($fake_file);
		@row=$sth_idn->fetchrow_array();
		return -ENOENT() unless defined $row[0];
		$sth_fnaam->execute($fake_file);
		$datastore = $sth_fnaam->fetchall_arrayref();
		$fields = $sth_fnaam->{NAME};
    } elsif ($fake_file =~ /^\/datum\//) {
		$fake_file =~ s/^\/datum\/\d+\/\d+\///;
#		print STDERR "fake_file = $fake_file\n";
		$sth_idd->execute($fake_file);
		@row=$sth_idd->fetchrow_array();
		return -ENOENT() unless $row[0] > 0;
		$sth_fdatum->execute($fake_file);
		$datastore = $sth_fdatum->fetchall_arrayref();
		$fields = $sth_fdatum->{NAME};
    } else {
		return -ENOENT();
    }
	
    my $buffer = join(",",@$fields)."\n";
    foreach my $row (@$datastore) {
		$buffer .= join(",",@$row)."\n";
#		print Dumper($row);
    }
    $filecache{$file}=$buffer;
	return 0;
}

use strict 'refs';
    
sub tagsfs_getattr {
    my $fake_file = shift;
    my @list =(0,0,0,0,0,0,0,0,time(),time(),time(),1024,0);
	my $isdir=0;
	
#    print STDERR "tagsfs_getattr file = $fake_file\n";

	if ($fake_file eq "/") {
		$list[2] = S_IFDIR | 0555;
		$list[3] = $#directories;
		$list[7] = 4096;
		$isdir=1;
	} elsif ($fake_file eq "/fonds" || $fake_file eq "/datum") {
		$list[2] = S_IFDIR | 0555;
		$list[3] = 2;  
		$list[7] = 4096;
		$isdir=1;
	} elsif ($fake_file =~ /^\/datum\/\d+$/ ) {
		$list[2] = S_IFDIR | 0555;
		$list[3] = 2;  # kan geen kwaad
		$list[7] = 4096;
		$isdir=1;
	} elsif ($fake_file =~ /^\/datum\/\d+\/\d+$/ ) { 
		$list[2] = S_IFDIR | 0555;
		$list[3] = 2;  # kan geen kwaad
		$list[7] = 4096;
		$isdir=1;
	} else {
		$filecache{$fake_file} = undef;

# the regular files
		my $ret = load_file($fake_file);
		return $ret unless $ret == 0;
		my $length = length($filecache{$fake_file});

		$list[2] = S_IFREG|0444;
		$list[3] = 1;
		$list[7]= $length;
	}
#	print STDERR $fake_file, " ",join(",",@list),"\n";
    return @list;
}


sub tagsfs_open {
    my $fake_file = shift;
    my ($mode) = shift;
 
#    print STDERR "tagsfs_open file = $fake_file mode =$mode\n";
    if(($mode & 3) != O_RDONLY) {
        return -EACCES();
    }	
    return load_file($fake_file);
}


sub tagsfs_read {
    my ($fake_file,$bufsize,$off) = @_;
    my $ishtml = 0;#($fake_file =~ /\.html$/);
    my $iscsv = ($fake_file =~ /\.csv$/);
    my($dbh,$sth1,$sth2);
    my @row;
    my $datastore;
    my $fields;
    my $file = $fake_file;

#    print STDERR "tagsfs_read file = $fake_file bufsize=$bufsize off=$off\n";

    my $buffer = $filecache{$file};
    return -ENOENT() unless defined($buffer);
    my $length = length($buffer);

#    print STDERR "lengte = $length\n";
    return -EINVAL() if $off > $length || $off < 0;
#    print STDERR "$off <= $length\n";
    return 0 if $off == $length;
#    print STDERR " $off != $length\n";
    my $buf = substr($buffer,$off,$bufsize);
#    print STDERR $buf;
    return $buf;
}

# kludge
sub tagsfs_statfs {return 255,1,0,1,0,4096}
sub tagsfs_flush { my $path=shift;return 0;}
sub tagsfs_release { my $path=shift;$filecache{$path}=undef;return 0;}

fork and exit;

$dbh = DBI->connect("dbi:Pg:dbname=koersdata host=localhost","mfvl","gotect03");
$sth_year = $dbh->prepare(qq{SELECT DISTINCT year(datum) FROM koersid WHERE NOT datum IS NULL ORDER BY year(datum)});
$sth_month = $dbh->prepare(qq{SELECT DISTINCT month(datum) FROM koersid WHERE datum like ? ORDER BY month(datum)});
$sth_day = $dbh->prepare(qq{SELECT DISTINCT day(datum) FROM koersid WHERE datum like ? ORDER by day(datum)});
$sth_naam = $dbh->prepare(qq{SELECT DISTINCT naam FROM rekening order by naam});
$sth_idn=$dbh->prepare(qq{SELECT id FROM rekening WHERE naam=?});
$sth_idd=$dbh->prepare(qq{SELECT count(id) FROM koersid WHERE datum=?});
$sth_fnaam = $dbh->prepare(qq{SELECT naam,datum,volume,open,hoog,laag,slot FROM koers where naam=? order by datum});
$sth_fdatum = $dbh->prepare(qq{SELECT naam,datum,volume,open,hoog,laag,slot FROM koers where datum=? order by naam});

# Execute main on child process
Fuse::main(
	mountpoint=>$mountpoint,
	getattr=>\&tagsfs_getattr,
	getdir=>\&tagsfs_getdir,
	open=>\&tagsfs_open,
	statfs=>\&tagsfs_statfs,
	read=>\&tagsfs_read,
	mountopts=>"ro,allow_other",
	flush=>\&tagsfs_flush,
	release=>\&tagsfs_release,
	debug=>0,
);
