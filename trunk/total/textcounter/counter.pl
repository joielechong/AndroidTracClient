#!/usr/local/bin/perl
##############################################################################
# WWWCount Text Version (Requires Server Side Includes) Version 1.1
# Created by Matt Wright	mattw@worldwidemart.com
# Created on: 3/14/96		Last Modified on: 4/25/96
# Installation Instructions found in README file.
##############################################################################
# Define Variables

# Data Dir is the directory on your server that you wish to store the 
# count files in.  Each page that has the counter on it will have it's own 
# file.  

$data_dir = "/path/to/data/";

# Valid-URI allows you to set up the counter to only work under specific 
# directories on your server.  Include any of these directories as they 
# appear in a URI, into this array.  More information on URI's available in 
# README.

@valid_uri = ('/');

# Invalid-URI allows the owner of this script to set up the counter so 
# that certain portions of the web server that may be included in Valid-URI 
# cannot use the program.  Leave this commented out if you wish not to 
# block out certain parts.

# @invalid_uri = ('/');

##############################################################################
# Set Options

# Show Link allows you to add a link around the counter to point to 
# either instructions explaining to users how to set this up on the system 
# (useful if a system administrator wants to allow anyone to set things up 
# themselves).  Setting it to 0 will make no link, otherwise put the URL
# you want linked to the count here.

$show_link = "http://www.worldwidemart.com/scripts/";

# When Auto-Create is enabled, users will be able to auto-create the 
# count on their home pages by simply imbedding the Server Side Includes 
# call.  Setting auto_create to 1 enables it, 0 will disable it. Only 
# users in @valid_uri will be allowed to auto create.

$auto_create = "1";

# Show Date will show the date of when the count began if you set this 
# option to 1.  It will appear in yor document as [Count] hits since [Date].
# Set this to 0 and it will simply return the [Count].

$show_date = "1";

##############################################################################

# Print Content Type Header For Browser
print "Content-type: text/html\n\n";

$count_page = "$ENV{'DOCUMENT_URI'}";
if ($count_page =~ /\/$/) {
   chop($count_page);
}
$count_page =~ s/\//_/g;

# Check Valid-URI
&check_uri;

if (-e "$data_dir$count_page") {
   open(COUNT,"$data_dir$count_page");
   $line = <COUNT>;
   chop($line) if $line =~ /\n$/;
   close(COUNT);
   ($date,$count) = split(/\|\|/,$line);
}
elsif ($auto_create == 1) {
   &create; 
}
else {
   &error('page_not_found');
}

$count++;
if ($show_date == 1) {
   if ($show_link =~ /http:\/\//) {
      print "<a href=\"$show_link\">$count</a> hits since $date";
   }
   else {
      print "$count hits since $date";
   }
}
else {
   if ($show_link =~ /http:\/\//) {
      print "<a href=\"$show_link\">$count</a>";
   }
   else {
      print "$count";
   }
}

open(COUNT,">$data_dir$count_page") || &error('could_not_increment');
print COUNT "$date\|\|$count";
close(COUNT);

sub check_uri {
   $uri_check = "0";

   foreach $uri (@valid_uri) {
      if ($ENV{'DOCUMENT_URI'} =~ /$uri/) {
         $uri_check = "1";
         last;
      }
   }

   foreach $uri (@invalid_uri) {
      if ($ENV{'DOCUMENT_URI'} =~ /$uri/) {
         $uri_check = "0";
	 last;
      }
   }

   if ($uri_check == 0) {
      &error('bad_uri');
   }
}

sub create {
   ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
   @months = ("January","February","March","April","May","June","July",
	      "August","September","October","November","December");
   $date = "@months[$mon] $mday, 19$year";
   $count = "0";
   open(COUNT,">$data_dir$count_page") || &error('count_not_created');
   print COUNT "$date\|\|$count";
   close(COUNT);
}

sub error {
   $error = shift(@_);

   if ($error eq 'page_not_found') {
      print "[WWWCount Fatal Error: This Page Not Found\; Auto-Create Option Disabled]";
   }
   elsif ($error eq 'bad_uri') {
      print "[WWWCount Fatal Error: This Page Not In Valid URI]";
   }
   elsif ($error eq 'count_not_created') {
      print "[WWWCount Fatal Error: Could Not Write to File $datadir$count_page]";
   }
   elsif ($error eq 'could_not_increment') {
      print "[WWWCount Fatal Error: Could Not Increment Counter]";
   }
   exit;
}
