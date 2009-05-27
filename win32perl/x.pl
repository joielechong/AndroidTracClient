#! c:\perl\bin\perl.exe

use strict;

use CGI;

my $q = new CGI;

print $q->header;
print $q->start_html;
print $q->end_html;

