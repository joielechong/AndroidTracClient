#! /usr/bin/perl -w

use strict;
use File::Scan;
use Email::MIME;
use Email::MIME::Attachment::Stripper;

my $file;
my $fs;
$fs=File::Scan->new();

while ($file = shift) {
    print "$file\n";
    open IN,"<$file" or die "Kan $file niet open\n";
    my @contents = <IN>;
    close IN;

    my $contents=join("",@contents);
    
    my $mail=Email::MIME->new($contents);
    
    my $stripper = Email::MIME::Attachment::Stripper->new($mail);
    
    my @parts=$stripper->attachments;
    
    my $cnt=0;
    foreach my $part (@parts) {
	my %part=%$part;
	open OUT,">/tmp/x.$cnt.x" ;
	binmode(OUT);
	print OUT $part{'payload'};
	close OUT;
	my $virus = $fs->scan("/tmp/x.$cnt.x");
	if ($virus eq "") {
	    open PIJP,"uvscan /tmp/x.$cnt.x|";
	    while (<PIJP>) {
		next unless /.*Found the (.*) virus.*/;
		$virus = $1." (uvscan)";
	    }
	}
	print "  $cnt--> ",$part{'content_type'}," virus = $virus\n" if $virus ne "";
#	unlink ("/tmp/x.$cnt.x");
	$cnt++;
    }
}
