#!/usr/bin/perl
# Animation Perl Script
# Written by Matt Wright
# Created on: 9/28/95   Last Modified on: 11/21/95
# Version 1.2
# I can be reached at:	mattw@misha.net
# Scripts Archive at:	http://www.worldwidemart.com/scripts/
# Consult the file README for more information and Installation Instructions.

#########################################################
# Variables

$times = "1";
$basefile = "/WWW/images/animation/";
@files = ("begin.gif","second.gif","third.gif","last.gif");
$con_type = "gif";

# Done
#########################################################

# Unbuffer the output so it streams through faster and better

select (STDOUT);
$| = 1;

# Print out a HTTP/1.0 compatible header. Comment this line out if you 
# change the name to not have an nph in front of it.

print "HTTP/1.0 200 Okay\n";

# Start the multipart content

print "Content-Type: multipart/x-mixed-replace;boundary=myboundary\n\n";
print "--myboundary\n";

# For each file print the image out, and then loop back and print the next 
# image.  Do this for all images as many times as $times is defined as.

for ($num=1;$num<=$times;$num++) {
   foreach $file (@files) {
      print "Content-Type: image/$con_type\n\n";
      open(PIC,"$basefile$file");
      print <PIC>;
      close(PIC);
      print "\n--myboundary\n";
   }
}
