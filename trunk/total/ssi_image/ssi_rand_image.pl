#!/usr/bin/perl
# Random Image Displayer With Link Flexibility
# Built for use with Server Side Includes
# Created by: Matt Wright	mattw@misha.net
# Version 1.2
# Created On: 7/1/95            Last Modified: 11/4/95
# The file README contains more information and installation instructions.

###############################################
# Define Variables

$basedir = "http://your.host.xxx/path/to/images/";

@images = ("first_image.gif","second_image.jpg","third_image.gif");

@urls = ("http://url_linked/to/first_image",
         "http://url_linked/to/second_image",
         "http://url_linked/to/third_image");

@alt = ("First WWW Page","Second WWW Page","Third WWW Page");

# Done
###############################################

###############################################
# Options
$uselog = "1";            # 1 = YES; 0 = NO
   $logfile = "/path/to/log/file";
   $date = `/usr/bin/date`; chop($date);

$link_image = "1";        # 1 = YES; 0 = NO
$align = "left";
$border = "2";

# Done
###############################################

srand(time ^ $$);
$num = rand(@images); # Pick a Random Number

# Print Out Header With Random Filename and Base Directory
print "Content-type: text/html\n\n";
if ($link_image eq '1' && $urls[$num] ne "") {
   print "<a href=\"$urls[$num]\">";
}

print "<img src=\"$basedir$images[$num]\"";
if ($border ne "") {
   print " border=$border";
}
if ($align ne "") {
   print " align=$align";
}
if ($alt[$num] ne "") {
   print " alt=\"$alt[$num]\"";
}
print ">";

if ($link_image eq '1' && $urls[$num] ne "") {
   print "</a>";
}

print "\n";

# If You want a log, we add to it here.
if ($uselog eq '1') {
   open(LOG, ">>$logfile");
   print LOG "$images[$num] - $date - $ENV{'REMOTE_HOST'}\n";
   close(LOG);
}
