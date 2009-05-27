#! /usr/local/bin/perl
##############################################################################
# Random Link Generator
# Created By Matt Wright
# Version 1.0
# Created on 7/15/95            Last Modified On 7/30/95
# I can be reached at:          mattw@misha.net
# Scripts Found at:             http://worldwidemart.com/scripts/
# The file README contains more information and installation instructions.

# Define Variables
$linkfile = "/home/mattw/public_html/links/database";

# Options
$uselog = 1;            # 1 = YES; 0 = NO
   $logfile = "/home/mattw/public_html/links/rand_log";

$date = `date +"%D"`; chop($date);

open (LINKS, "$linkfile");

srand();                        # kick rand
$nlines=@file=<LINKS>;          # inhale file & get # of lines
print "Location: $file[int rand $nlines]\n\n";  # print a random line

close (LINKS);

if ($uselog eq '1') {
   open (LOG, ">>$logfile");
   print LOG "$ENV{'REMOTE_HOST'} - [$date]\n";
   close (LOG);
}

exit;
