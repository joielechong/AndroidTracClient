#! /usr/local/bin/perl

$file = "/home/~yourname/public_html/links/links.html";

# Begin the Editing of the Links File
open (FIL,"$file");
@LINES=<FIL>;
close(FIL);
$SIZE=@LINES;

# Open Link File to Output
open (MSG,">$file");

for ($i=0;$i<=$SIZE;$i++) {
   $_=$LINES[$i];
  if (/<li><a href=(.*)>(.*)<\/a>/) {
    print MSG "<li><a href=\"$1\">$2</a>\n";
  }
   else { print MSG $_; }
}
close (MSG);

