#!/usr/bin/perl
# Free For All Link Script
# Created by Matt Wright        (mattw@misha.net)
# Created On: 5/14/95           Last Modified: 10/15/95
# Version: 2.1

# Define Variables
$filename = "/home/mattw/public_html/links/newlinks.html";
$linksurl = "http://your.host.xxx/links/newlinks.html";
$linkspl = "http://your.host.xxx/cgi-bin/links.pl";

$datecom = '/usr/bin/date';
$date = `$datecom +"%r on %A, %B %d, %Y %Z"`; chop($date);

# Get the input
read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});

# Split the name-value pairs
@pairs = split(/&/, $buffer);

foreach $pair (@pairs) {
   ($name, $value) = split(/=/, $pair);

   $value =~ tr/+/ /;
   $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
   $value =~ s/<([^>]|\n)*>//g;

   $FORM{$name} = $value;
}

if ($FORM{'url'} eq 'http://') { &no_url; }
&no_url unless $FORM{'url'};
&no_title unless $FORM{'title'};

# Enter our tags and sections into an associative array

%sections = ("busi","Business","comp","Computers","educ","Education",
	     "ente","Entertainment","gove","Government",
	     "pers","Personal","misc","Miscellaneous");

# Suck previous link file into one big string
$response = `grep '<li><a href' $filename`;
@data = split(/\n/,$response);

$i=1;

foreach $line (@data) { # For every line in our data
  $i++;
}

open (FILE,"$filename");
@LINES=<FILE>;
close(FILE);
$SIZE=@LINES;

open (FILE,">$filename");
for ($a=0;$a<=$SIZE;$a++) {
   $_=$LINES[$a];
   if (/<!--number-->/) {
      print FILE "<!--number--><b>There are <i>$i</i> links on this ";
      print FILE "page.</b><br>\n";
   }
   elsif (/<!--time-->/) {
      print FILE "<!--time--><b>Last link was added at $date</b><hr>\n";
   }
   else { 
      print FILE $_;
   }
}
close (FILE);

open (FILE,"$filename");

while (<FILE>) {
   $raw_data .=  $_;
}

close(FILE);

# Make a normal array out of this data, one line per entry.  NOTE: This
# eats up our newlines, so be sure to add them back when we print back to
# the file.

undef $/;
@proc_data = split(/\n/,$raw_data);

# Open Link File to Output
open (FILE,">$filename");

foreach $line (@proc_data) { # For every line in our data

   print FILE "$line\n";   # Print the line.  We have to do this no
   			   # matter what, so let's get it over with.

   foreach $tag (keys(%sections)) { # For every tag 
      if ( ($FORM{section} eq $sections{$tag}) && 
         ($line =~ /<!--$tag-->/) ) {

         print FILE "<li><a href=\"$FORM{'url'}\">$FORM{'title'}</a>\n"; 
      }
   }
}

close (FILE);

# Return Link File
print "Location: $linksurl\n\n";

sub no_url {
   print "Content-type: text/html\n\n";
   print "<html><head><title>NO URL</title></head>\n";
   print "<body><h1>ERROR - NO URL</h1>\n";
   print "You forgot to enter a url you wanted added to the Free for ";  
   print "all link page.<p>\n";
   print "<form method=POST action=\"$linkspl\">\n";
   print "<input type=hidden name=\"title\" value=\"$FORM{'title'}\">\n";
   print "<input type=hidden name=\"section\""; 
   print "value=\"$FORM{'section'}\">\n";
   print "URL: <input type=text name=\"url\" size=50><p>\n";
   print "<input type=submit> * <input type=reset>\n";
   print "<hr>\n";
   print "<a href=\"$linksurl\">Back to the Free for all Link"; 
   print "Page</a>\n";
   print "</form></body></html>\n";

   exit;
}

sub no_title {
   print "Content-type: text/html\n\n";
   print "<html><head><title>NO TITLE</title></head>\n";
   print "<body><h1>ERROR - NO TITLE</h1>\n";
   print "You forgot to enter a title you wanted added to the Free for ";
   print "all link page.<p>\n";
   print "<form method=POST action=\"$linkspl\">\n";
   print "<input type=hidden name=\"url\" value=\"$FORM{'url'}\">\n"; 
   print "<input type=hidden name=\"section\"";
   print "value=\"$FORM{'section'}\">\n";
   print "TITLE: <input type=text name=\"title\" size=50><p>\n";
   print "<input type=submit> * <input type=reset>\n";
   print "<hr>\n";
   print "<a href=\"$linksurl\">Back to the free for all links";
   print "page</a>\n";
   print "</form></body></html>\n";

   exit;
}
