#! /usr/local/bin/perl
# Random Image Displayer
# Version 1.2 Created by: Matt Wright
# Created On: 7/1/95            Last Modified: 7/17/95
# I can be reached at:  mattw@misha.net
# Scripts Archive at:	http://worldwidemart.com/scripts/
# The file README contains more information and installation instructions.

# Necessary Variables
  $basedir = "http://alpha.pr1.k12.co.us/~mattw/image/pics/";
  @files = ("waterfalls.gif","test.gif","random.gif","neat.jpg");

# Options
  $uselog = 0; # 1 = YES; 0 = NO
        $logfile = "/home/mattw/public_html/image/pics/piclog";

  srand;
  $num = rand(@files); # Pick a Random Number

# Print Out Header With Random Filename and Base Directory
  print "Location: $basedir$files[$num]\n\n";

# Log Image
  if ($uselog eq '1') {
     open (LOG, ">>$logfile");
     print LOG "$files[$num]\n";
     close (LOG);
  }
exit;
