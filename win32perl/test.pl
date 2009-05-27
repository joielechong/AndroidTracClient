#! c:\perl\bin\perl.exe -w

use strict;
use CGI;

my $q=new CGI;
print $q->header;

print $q->start_html('A simple example');
print $q->h1('A simple example');
