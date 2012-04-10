#! /usr/bin/perl -w

use strict;
use CGI;
use CGI::Ajax;
use DBI;
use Encode;
use Data::Dumper;

sub dbi_connect {
    my $dbh = DBI->connect_cached("dbi:Pg:dbname=mfvl");
    $dbh->do("SET search_path TO school");
    return $dbh;
}

sub dummy {
    return "";
}

sub Show_Form {
    my $html;
    $html = <<EOT;
 <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
 <html>
 <head>
 <meta charset="utf-8">
 <title>Aftekenen</title>
 </head>
 <body>
 <div id="tabel">
 <table>
EOT

my $dbh=dbi_connect("dbi:Pg:dbname=mfvl");
    
    my $leerling_ref = $dbh->selectall_arrayref("select leerling,groep,id from groepen join leerling on (leerling=naam) where leerjaar=2012 order by groep,leerling",{ Slice=>{} });
    my $vakken_ref = $dbh->selectall_arrayref("select * from vakken order by id",{ Slice=>{} });
#    $html .= "<pre>\n";
#    $html .= Dumper($leerling_ref);
#    $html .= Dumper($vakken_ref);
#    $html .= "</pre>\n";
    $html .= "<tr><th>Groep</th><th>Leerling</th>";
    foreach my $v (@$vakken_ref) {
	$html .= "<th>".$v->{vak}."</th>";
    }
    $html .="</tr>\n";

    foreach my $l (@$leerling_ref) {
	$html .= "<tr><td align=center>".$l->{groep}."</td><td>".$l->{leerling}."</td>";
	foreach my $v (@$vakken_ref) {
	    $html .= qq!<td align=right><input id="inp_!.$l->{id}."_".$l->{groep}."_".$v->{id}.qq!"></td>!;
	}
	$html .="</tr>\n";
    }

    $html .= <<EOT;    
</table>
</div>
</body>
</html>
EOT
    
return $html;
}

my $q = new CGI;

my $pjx = CGI::Ajax->new(
    dummy => \&dummy
    );
$pjx->JSDEBUG(2);
$pjx->DEBUG(0);

# not show the html, which will include the embedded javascript code 
# to handle the ajax interaction 
print $pjx->build_html($q,\&Show_Form); 
# this outputs the html for the page
