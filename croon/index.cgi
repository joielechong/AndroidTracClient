#! /usr/bin/perl -w

use strict;
use CGI;
use CGI::Ajax;
use DBI;
use Encode;
use Data::Dumper;

my $q = new CGI;

my %width = ("eis"                              => 20,
             "eistekst"                         => 70,
             "document id"                      => 20,
             "imandra id"                       => 11,
             "protocol id"                      => 18,
             "titel"                            => 40,
             "document titel"                   => 40,
             "prototol titel"                   => 40,
             "di"                               => 5,
             "ontwerp verificatie"              => 10,
             "keuring"                          => 10,
             "beproeving"                       => 10,
             "inspectie"                        => 10,
             "moment_van_aantonen"              => 10,
             "bewijsvoeringsmethode"            => 15,
             "subbewijsvoeringsmethode"         => 15,
             "testfase"                         => 9,
             "valide_methode"                   => 18,
             "type_criteria"                    => 19,
             "waarde_criteria"                  => 19,
             "bron_criteria"                    => 14,
             "documentnummer bewijsdocument"    => 18,
             "waar in bewijsdocument"           => 25,
             "verificatie_akkoord"              => 10,	
             "verificatie akkoord"              => 10,	
             "verificatie niet akkoord omdat"   => 15,
             "wie heeft verificatie uitgevoerd" => 14,
             "verificator"                      => 14,
             "opmerking_bij_eis"                => 40,
             "opmerking bij eis"                => 40,
             "opmerking_bij_verificatie"        => 40,
             "opmerking bij verificatie"        => 40,
             "opmerking"                        => 40,
             "opmerkingen"                      => 40,
             "locatie"                          => 30,
             "locatie omschrijving"             => 40,
             "fasering"                         => 12,
             "faseid"                           => 12,
             "fase"                             => 12,
             "milestone"                        => 11,
             "fase omschrijving"                => 32,
             "seqnr"                            => 5);
             
sub dbi_connect {
    return DBI->connect_cached("dbi:Pg:dbname=croon");
}

sub remove_escapes {
    my $searchterm = shift;
    return $searchterm unless defined $searchterm;
    $searchterm =~ s/%20/ /g;
    $searchterm =~ s/%2C/,/g;
    $searchterm =~ s/%2A/:/g;   
    $searchterm =~ s/&amp;/&/g;
    return $searchterm;
}

my %htsql = (
    "laad_protocollen" => qq!/htsql/documents{document_id,document_titel}.sort(document_id)?rapporttype='TS'!,
    "laad_fasering" => qq!/htsql/fases.sort(milestone)!,
    "laad_lokaties" => qq!/htsql/locaties{locnaam,beschrijving}.sort(locnaam)!,
    "laad_rapporten" => qq!/htsql/rapport_fase!,
    "info_fases" => qq!/htsql/fase_loc{faseid,locatie,locaties.beschrijving,opmerkingen}.sort(locatie)?faseid='%arg1%'!,
    "info_fases_locs" => qq!/htsql/locatie_protocol{fase,locnaam,protocol_doc_id,documents.ImandraID,documents.di,documents.testfase,documents.document_titel,documents.docstatus}.sort(protocol_doc_id)?fase='%arg1%'&locnaam='%arg2%'!,
    "info_fases_locs_reps" => qq!/htsql/locatie_report{faseid,locnaam,report_doc_id,documents.ImandraID,documents.di,documents.testfase,documents.document_titel,documents.docstatus}.sort(report_doc_id)?faseid='%arg1%'&locnaam='%arg2%'!,
    "vmx_di" => qq!https://van-loon.xs4all.nl/htsql/eis_di{di,eis,unieke_eisen.eistekst,status,vmxov,ovsd,ovdo,ovuo,vmxke,vmxbp,bpfat,bpifat,bpsat,bpisat,bpsit,vmxin}.sort(di,eis)?di='%arg1%'!
    );

sub printbaar {
    my $naam = shift;
    
    my $html = "";
    if (defined($htsql{$naam})) {
        my $url = $htsql{$naam};
        my $i = 1;
        foreach (@_) {
            my $vn = "%arg$i%";
            $url =~ s/$vn/$_/g;
            $i++;
        }
        $html .= qq!<a class="select" href="$url" target="Rapport">Printbaar overzicht</a> !;
    }
    return $html;
}

sub tabel_header { 
    my $th = qq!<tr valign=top>!;
    my $col = qq!<colgroup>!;
    foreach my $f (@_) {
        if(defined($f)) {
            my $flc = lc($f);
            my $w =  (exists($width{$flc}) ? $width{$flc}*8 : "*");
            $w .= "px" unless $w eq "*";
            $col .= qq!<col width='$w'>!;
            $th .= qq!<th class="veldnaam">$f</th>! if defined($f);
        }
    }
    $col .= qq!</colgroup>\n!;
    $th .= qq!</tr>\n!;
    return $col.$th;
}

sub toontabel {
    return toontabel_alles(undef,@_);
}

sub toontabel_alles {
    my $contextmenu=shift;
    my $sth = shift;
    my $titel = shift;
    my $selectveld = shift;
    my $naam = shift;
    my $spveld = shift;
    my @spveld = split(",",$spveld) if defined($spveld);
    my @names = @{ $sth->{NAME} };
    delete $names[$spveld[1]] if defined($spveld);
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = '';
#    $html .= "\n<!-- ".Dumper($contextmenu)." -->\n\n";
    $html .= "<table border=1>\n";
    $html .= qq!<tr><th class="titel" colspan=$numfields>$titel</th></tr>\n! if defined($titel);
    $html .= tabel_header(@names);
    my $rownr = 0;
    while (my $row=$sth->fetchrow_arrayref()) {
	$html .= qq!<tr valign="top"!.(($rownr & 1)==1 ? qq! class="alt" ! : "").qq!>!;
	my $i=0;
        my $url;
        my $typedoc;
        if (defined($spveld)) {
            $url = $row->[$spveld[1]];
            if (defined($url)) {
                $url =~ s/ /\%20/g;
                $url =~ s/,/\%2C/g;
                $url =~ s/:/\%2A/g;   
                $url =~ s/\&/&amp;/g;
            }
        }
	foreach my $f (@{$row}) {
	    unless (defined($spveld) && $i == $spveld[1]) {
		$typedoc="Email" if (defined($f) && $f =~ m/^071500-E/);
		$typedoc="Documents" if (defined($f) && $f =~ m/^071500-D/);
		my $f1="";
		my $f2="";
		my $f3="";
		if ((defined($selectveld) && $selectveld==$i) || (defined($url) && $i==$spveld[0] && defined($typedoc))){
		    $f1 = qq! class="select"!;
		    if (defined($url) && $i==$spveld[0]) {
			$f2 = qq!<a target="document" href="http://projects.croon.net/projects/071500/$typedoc/$url">!;
		    } else {
			$f2 = qq!<a onclick="return ${naam}_onclick('$f');">!;
		    }
		    $f3 = qq!</a>!;
		}
		if (defined($contextmenu)) {
		    my $functie = $contextmenu->{functie};
		    my $kolom = $contextmenu->{kolom};
		    my $key = $contextmenu->{key};
		    $f1 .= qq! oncontextmenu="return $functie(event,'$key',$kolom,['!.join("','", map {(defined($_)?$_:'')} @{$row}).qq!']);"! if $kolom == $i;
		}
		if (defined($f)) {
		    $f =~ s/[\xE2][\x84][\xA6]/&#x2126/g;
		    $f =~ s/([\xC2\xC3])([\x80-\xBF])/chr(ord($1)<<6&0xC0|ord($2)&0x3F)/eg;
#                   $f =~ s/([\x80-\xFF])/chr(0xC0|ord($1)>>6).chr(0x80|ord($1)&0x3F)/eg;
		    $f =~ s/\n/<br>/g;
		}
		$html .= "<td$f1>$f2".((defined($f)&&$f ne '')?$f:"&nbsp;")."$f3</td>";
	    }	    
	    $i++;
	}
	$html .= "</tr>\n";
	$rownr++;
    }
    $html .= "</table>\n";
    return $html;
}

    
my @sql = (qq< select distinct eis from unieke_eisen where eis ilike ? order by 1>,
	   qq< select distinct eis from eis_di where di = ? order by eis>,
           qq< select distinct eis from unieke_eisen where eistekst ilike ? order by eis>,
	   qq< select distinct eis from vmx_fasering_alles  where faseid = ? order by eis>,
           qq< select distinct eis from locatie_protocol as l join protocol_eis as p on (l.protocol_doc_id=p.protocol_doc_id) where locnaam=? order by eis>
    );

sub exported_fx {
    my $function = shift;
    my $exact = shift;
    my $searchterm = remove_escapes(shift);
    my $sql = $sql[$function];
    my $html = "";
    
    my $dbh = dbi_connect();	
    my $sth = $dbh->prepare( $sql );
    my $default;
    
    if ($exact == 1) {
        $sth->execute( $searchterm );
    } else {
        $sth->execute( '%'.$searchterm . '%');
    }
    
    
    # start off the div contents with select init
    $html .= qq!<select id="eisselect" name="eisselect" SIZE=36 style="width:270px;" onclick="eisen_detail();return true;">\n!;
    
    # dot on each option from the db
    while ( my $row = $sth->fetch() ) {
        my $f = $$row[0];
        $f = (defined($f)&&$f ne ''?$f:"&nbsp;");
        $default = $f unless defined ($default);
        $html .= qq!<option>! . $f . qq!</option>\n!;
    }
	
    # close off the select and return
    $html .= qq!</select>\n!;
    if (defined($default)) {
        $html .= qq!<script type="text/javascript">\n!;
        $html .= qq!document.getElementById(eisselect).value='$default';\n!;
        $html .= qq!eisen_detail();return true;\n!;
        $html .= qq!</script>\n!;
    }
    return($html);
}

sub exported_fx1 {
     exported_fx(0,0,@_);
}

sub exported_fx2 {
     exported_fx(1,1,@_);
}

sub searchtekst {
     exported_fx(2,0,@_);
}

sub exported_fx4 {
     exported_fx(3,1,@_);
}

sub exported_fx5 {
     exported_fx(4,1,@_);
}

my @vmx_table = (qq<vmx_fasering_alles>, qq<vmx_fasering_alles>,qq<vmx_di>);
my @vmx_inputfield = ('locatie','faseid','di');

sub vmx_tabel {
    my $functie = shift;
    my $searchterm = shift;
    my $tabel=$vmx_table[$functie];
    
    my $veld=$vmx_inputfield[$functie];

    my $dbh = dbi_connect();	
    my $sql=qq< select * from $tabel WHERE $veld=? ORDER BY locatie,eis,faseid>;
    my $sth = $dbh->prepare( $sql );
    $sth->execute($searchterm);
	return toontabel($sth,qq!Verificatiematrix voor $searchterm!);
}

sub vmxlokatie {
    vmx_tabel(0,$_[0],0);
}

sub vmxfasering {
    vmx_tabel(1,$_[0],0);
}

sub vmxdetails {
    my $eis = shift;
    my $sql = q!select d.di as "DI",d.status,vmxov as "Ontwerp Verificatie",vmxke as "Keuring",vmxbp as "Beproeving",vmxin as "Inspectie",vmxlc as "Lock",ovuo,ovdo,ovsd,(select count(usecase)>0 as ovuc from features join feat_uc on (feature=feat and features.eis=e.eis and d.di='86'))::boolean as ovuc,bpfat,bpifat,bpsat,bpisat,bpsit,ovch from eis_di as d join unieke_eisen as e on (e.eis=d.eis) where e.eis=? order by d.di!;
    my $dbh = dbi_connect();	
    my $sth = $dbh->prepare( $sql );
    $sth->execute( $eis );
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    for (my $i=7;$i<$numfields;$i++) {
        $names[$i] = undef;
    }
    my $html = '';
    $html .= "<table border=1>\n";
    $html .= qq!<tr><th class="titel" colspan=7>Verificatieoverzicht voor eis $eis</th></tr>\n!;
    $html .= tabel_header(@names);
	my $rownr = 0;
    while (my $row=$sth->fetchrow_arrayref()) {
        my $di = $$row[0];
        my $status = $$row[1];
        my $ov = $$row[2];
        my $keur = $$row[3];
        my $bepr = $$row[4];
        my $insp = $$row[5];
        my $lock = $$row[6];
        my $ovch = $$row[16];
        $html .= qq!<tr valign="top"!.(($rownr & 1)==1 ? qq! class="alt" ! : "").qq!>!;

        $html .= qq!<td>$di</td><td class='$status'>$status</td>!;
        $html .= qq!<td!;
        $html .= qq! class='ovch'! if ($ovch == 1);
        $html .= qq!><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_ov'><input type=checkbox name='${eis}_ov_chk' onclick="altvmx('$eis','$di','ov')"!.($ov==1?' CHECKED':'').qq!>!;
        if ($ov == 1 or $$row[10]==1) {
            $html .= do_vmxov($eis,$di,$$row[7],$$row[8],$$row[9],$$row[10]);
        }
        $html .= qq!</div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_ke'><input type=checkbox name='${eis}_ke_chk' onclick="altvmx('$eis','$di','ke')"!.($keur==1?' CHECKED':'').qq!></div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_bp'><input type=checkbox name='${eis}_bp_chk' onclick="altvmx('$eis','$di','bp')"!.($bepr==1?' CHECKED':'').qq!>!;
        if ($bepr == 1) {
            $html .= do_altvmxbp($eis,$di,$$row[11],$$row[12],$$row[13],$$row[14],$$row[15],$lock);
        }
        $html .= q!</div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_in'><input type=checkbox name='${eis}_in_chk' onclick="altvmx('$eis','$di','in')"!.($insp==1?' CHECKED':'').qq!></div></td>!;
        $html .= qq!<td><div id='d_${eis}_${di}_lc'><input type=checkbox name='${eis}_lc_chk' onclick="altvmxlc('$eis','$di','$lock')"!.($lock==1?' CHECKED':'').qq!></div></td>!;

		$html .= "</tr>\n";
		$rownr++;
    }
    $html .= q!</table>!;
    return $html;
}

sub vmxdi {
#    vmx_tabel(2,$_[0],0);
    my $di = shift;
    my $sql = q!select e.eis,eistekst,d.status,vmxov as "Ontwerp Verificatie",vmxke as "Keuring",vmxbp as "Beproeving",vmxin as "Inspectie",vmxlc as "Lock",ovuo,ovdo,ovsd,(select count(usecase)>0 as ovuc from features join feat_uc on (feature=feat and features.eis=e.eis and d.di='86'))::boolean as ovuc,bpfat,bpifat,bpsat,bpisat,bpsit,ovch from eis_di as d join unieke_eisen as e on (e.eis=d.eis) where d.di=? order by d.eis!;
    my $sqldi = q!select objname from objecten where objid=?!;
    my $dbh = dbi_connect();	
    my $sth = $dbh->prepare( $sql );
    my $sthdi = $dbh->prepare($sqldi);
    $sthdi->execute($di);
    my $row=$sthdi->fetch();
    my $diname="";
    $diname = $$row[0] if defined($row);;
        
    $sth->execute( $di );
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    for (my $i=8;$i<$numfields;$i++) {
        $names[$i] = undef;
    }
    my $html = '';
    $html .= "<table border=1>\n";
    $html .= qq!<tr><th class="titel" colspan=8>Verificatieoverzicht voor Deelinstallatie $di!.($diname ne '' ? " ($diname)":"").qq!</th></tr>\n!;
    $html .= tabel_header(@names);
	my $rownr = 0;
    while ($row=$sth->fetchrow_arrayref()) {
        my $eis = $$row[0];
        my $eistekst = $$row[1];
        my $status = $$row[2];
        my $ov = $$row[3];
        my $keur = $$row[4];
        my $bepr = $$row[5];
        my $insp = $$row[6];
        my $lock = $$row[7];
        my $ovch = $$row[17];
        $html .= qq!<tr valign="top"!.(($rownr & 1)==1 ? qq! class="alt" ! : "").qq!>!;

	$eistekst =~ s/[\xE2][\x84][\xA6]/&#x2126/g;
        $eistekst =~ s/([\xC2\xC3])([\x80-\xBF])/chr(ord($1)<<6&0xC0|ord($2)&0x3F)/eg;
        $eistekst =~ s/\n/<br>/g;
        $html .= qq!<td>$eis</td><td>$eistekst</td><td class='$status'>$status</td>!;
        $html .= qq!<td!;
        $html .= qq! class='ovch'! if ($ovch == 1);
        $html .= qq!><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_ov'><input type=checkbox name='${eis}_ov_chk' onclick="altvmx('$eis','$di','ov')"!.($ov==1?' CHECKED':'').qq!>!;
        if ($ov == 1 or $$row[11]==1) {
            $html .= do_vmxov($eis,$di,$$row[8],$$row[9],$$row[10],$$row[11]);
        }
        $html .= qq!</div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_ke'><input type=checkbox name='${eis}_ke_chk' onclick="altvmx('$eis','$di','ke')"!.($keur==1?' CHECKED':'').qq!></div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_bp'><input type=checkbox name='${eis}_bp_chk' onclick="altvmx('$eis','$di','bp')"!.($bepr==1?' CHECKED':'').qq!>!;
        if ($bepr == 1) {
            $html .= do_altvmxbp($eis,$di,$$row[12],$$row[13],$$row[14],$$row[15],$$row[16],$lock);
        }
        $html .= q!</div></td>!;
        $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_in'><input type=checkbox name='${eis}_in_chk' onclick="altvmx('$eis','$di','in')"!.($insp==1?' CHECKED':'').qq!></div></td>!;
        $html .= qq!<td><div id='d_${eis}_${di}_lc'><input type=checkbox name='${eis}_lc_chk' onclick="altvmxlc('$eis','$di',$lock)"!.($lock==1?' CHECKED':'').qq!></div></td>!;

		$html .= "</tr>\n";
		$rownr++;
    }
    $html .= <<EOT;
</table>
EOT
    $html .= printbaar('vmx_di',$di);
    return $html;
}

sub do_altvmxbp {
    my ($eis,$di,$fat,$ifat,$sat,$isat,$sit,$lock) = @_;
    my $html = '';
    $html .= q!<table border='0'><tr><td>FAT</td><td>IFAT</td><td>SAT</td><td>ISAT</td><td>SIT</td></tr><tr>!;
    $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_fat'><input type=checkbox name='${eis}_fat_chk' onclick="altvmx('$eis','$di','fat')"!.($fat==1?' CHECKED':'').qq!></div></td>!;
    $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_ifat'><input type=checkbox name='${eis}_ifat_chk' onclick="altvmx('$eis','$di','ifat')"!.($ifat==1?' CHECKED':'').qq!></div></td>!;
    $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_sat'><input type=checkbox name='${eis}_sat_chk' onclick="altvmx('$eis','$di','sat')"!.($sat==1?' CHECKED':'').qq!></div></td>!;
    $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_isat'><input type=checkbox name='${eis}_isat_chk' onclick="altvmx('$eis','$di','isat')"!.($isat==1?' CHECKED':'').qq!></div></td>!;
    $html .= qq!<td><div!.($lock==1 ? qq! class='locked'!:' ').qq!id='d_${eis}_${di}_sit'><input type=checkbox name='${eis}_sit_chk' onclick="altvmx('$eis','$di','sit')"!.($sit==1?' CHECKED':'').qq!></div></td>!;
    $html .= q!</tr></table>!;
    return $html;
}

sub do_vmxov {
    my ($eis,$di,$uo,$do,$sd,$uc) = @_;
    my $html = '';
    $html .= q!<table border='0'><tr><td>UO</td><td>DO</td><td>SD</td><td>UC</td></tr><tr>!;
    $html .= qq!<td>!.($uo==1?'X':'').qq!</td>!;
    $html .= qq!<td>!.($do==1?'X':'').qq!</td>!;
    $html .= qq!<td>!.($sd==1?'X':'').qq!</td>!;
    $html .= qq!<td>!.($uc==1?'X':'').qq!</td>!;
    $html .= q!</tr></table>!;
    return $html;
}

sub do_altvmx {
    my $eis = shift;
    my $di = shift;
    my $fase = shift;
    my $html = "";
    my $row;
#    $html .= "<pre>$eis $di $fase</pre>\n";

    my $field="vmx$fase";
    $field="bp$fase" if length($fase) > 2;   #hek hek hek
    
    my $sql1 = qq!update eis_di set $field=NOT $field,ovch=false where eis=? and di=?!;
#    $html .= "<pre>$sql1</pre>\n";
    my $dbh = dbi_connect();	
    my $sth1 = $dbh->prepare($sql1);
    $sth1->execute($eis,$di);
    my $sql2 = qq!select $field from eis_di where eis=? and di=?!;
#    $html .= "<pre>$sql2</pre>\n";
    my $sth2=$dbh->prepare($sql2);
    $sth2->execute($eis,$di);
    $row=$sth2->fetch();
    my $val = $$row[0];
#    $html .= "<pre>$eis $di $fase</pre>\n";
    if ($fase ne 'lc') {
        $html .= qq!<input type=checkbox name='${eis}_${fase}_chk' onclick="altvmx('$eis','$di','$fase')"!.($val==1?' CHECKED':'').qq!>!;
    }
    else {
        $html .= qq!<input type=checkbox name='${eis}_${fase}_chk' onclick="altvmxlc('$eis','$di','$val')"!.($val==1?' CHECKED':'').qq!>!;
    }
    $sth2->finish();
    if ($fase eq "bp" && $val == 1) {
        my $sql3 = qq!select bpfat,bpifat,bpsat,bpisat,bpsit,vmxlc from eis_di where eis=? and di=?!;
#        $html .= "<pre>$sql3</pre>\n";
        my $sth3=$dbh->prepare($sql3);
        $sth3->execute($eis,$di);
        $row=$sth3->fetch();
        $html .= do_altvmxbp($eis,$di,$$row[0],$$row[1],$$row[2],$$row[3],$$row[4],$$row[5]);  # let $$row[5] is het lock veld
        $sth3->finish();
    }
    return $html;
}

sub didetails {
    my $eis = shift;
    $eis =~ s/&amp;/&/;
    
    my $sql = qq<select di as "DI nr",objname as "Deelinstallatie",protocol_doc_id as "Testprotocol Id", "Testprotocol",locnaam as "Locatie", testfase as "Testfase",fase as "Fasering",report_doc_id as "Testrapport Id","Testrapport",verificatie_akkoord as "Verificatie akkoord",niet_akkoord_reden as "Niet akkoord omdat",verificator as "Wie heeft verificatie uitgevoerd",opmerking_bij_verificatie as "Opmerking bij verificatie" from  toewijzing where eis=? >;
    my $dbh = dbi_connect();	
    my $sth = $dbh->prepare( $sql );
    $sth->execute( $eis );
    return toontabel_alles({key=>"$eis",kolom=>2,functie=>'voerprotoin'},$sth,"Toewijzingen van $eis");
}

sub details {
    my $eisid = shift;
    $eisid =~ s/&amp;/&/;
    
    my $sql = qq< select * from unieke_eisen where eis = ? >;
    my $dbh = dbi_connect();	
    my $sth = $dbh->prepare( $sql );
    $sth->execute( $eisid );
    return toontabel($sth,"Eis details");
}

sub info_fases_locs_eisen {
    my $faseid = remove_escapes(shift);
    my $locnaam = remove_escapes(shift);
    my $protid = remove_escapes(shift);
    
    my $sql = qq< select p.di as "DI",u.eis as "Eis",u.eistekst as "Eistekst",u.eistype as "Eistype",u.soort as "Eissoort" from protocol_eis as p join unieke_eisen as u on (u.eis=p.eis) where p.protocol_doc_id=? order by p.di,p.eis>;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($protid);
	my $html = toontabel($sth,qq!Eisen voor prototol $protid!);
	$html .= qq!<input type='submit' id='faselocproteisenrefresh' value='Refresh' onclick="return info_fases_locs_eisen_refresh();">\n!;
    return $html;    
}

sub do_info_fases_locs_reps {
    my $faseid = shift;
    my $locnaam = shift;
    my $dbh = shift;

    my $sql = qq< select l.report_doc_id as "Rapport id",d."ImandraID" as "Imandra Id",d.di as "DI",testfase as "Testfase",document_titel as "Document titel", docstatus as "Document status",link from locatie_report as l join documents as d on(l.report_doc_id=d.document_id)  where faseid = ? and locnaam=? order by l.report_doc_id>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($faseid,$locnaam);
	my $html = toontabel($sth,qq!Rapporten voor fase $faseid en locatie $locnaam!,undef,"info_fases_locs_reps","4,6");
    $html .= <<EOT;
<div id="d_addfaselocrep"></div>
<input type='submit' id='faselocreprefresh' value='Refresh' onclick="return info_fases_locs_reps_refresh();">
EOT
    $html .= printbaar('info_fases_locs_reps',$faseid,$locnaam);
    $html .= <<EOT;
<a class="select" onclick="return info_fases_locs_reps_add();">Koppel rapport</a>
EOT
    return $html;    
}

sub info_fases_locs_reps {
    return do_info_fases_locs_reps(remove_escapes(shift),remove_escapes(shift),dbi_connect());
}

sub do_info_fases_locs {
    my $faseid = shift;
    my $locnaam = shift;
    my $dbh = shift;

    my $sql = qq< select l.protocol_doc_id as "Protocol id",d."ImandraID" as "Imandra Id",d.di as "DI",testfase as "Testfase",document_titel as "Document titel", docstatus as "Document status",link from locatie_protocol as l join documents as d on(l.protocol_doc_id=d.document_id)  where fase = ? and locnaam=? and l.protocol_doc_id != 'Dummyprotocol' order by l.protocol_doc_id>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($faseid,$locnaam);
	my $html = toontabel($sth,qq!Protocollen voor fase $faseid en locatie $locnaam!,0,"info_fases_locs","4,6");
    $html .= <<EOT;
<input type=hidden id='faselocproteisval'>
<div id="d_addfaselocprot"></div>
<input type='submit' id='faselocprotrefresh' value='Refresh' onclick="return info_fases_locs_refresh();">
EOT
    $html .= printbaar('info_fases_locs',$faseid,$locnaam);
    $html .= <<EOT;
<a class="select" onclick="return info_fases_locs_add();">Koppel protocol</a>
EOT
    return $html;    
}

sub info_fases_locs {
    return do_info_fases_locs(remove_escapes(shift),remove_escapes(shift),dbi_connect());
}

sub insfaselocprot {
    my $invoer = remove_escapes(shift);
    my $faseid = remove_escapes(shift);
    my $locnaam = remove_escapes(shift);
    my $sqlins = qq< insert into locatie_protocol (fase,locnaam,protocol_doc_id) values (?,?,?) >;
    my ($protocol,$titel) = split(' : ',$invoer);
    my $dbh = dbi_connect();
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($faseid,$locnaam,$protocol);
    return do_info_fases_locs($faseid,$locnaam,$dbh);
}

sub loadaddfaselocprot {
    my $faseid = remove_escapes(shift);
    my $locnaam = remove_escapes(shift);;
    my $sql = qq< select document_id,document_titel from documents left join locatie_protocol on (document_id=protocol_doc_id and locnaam=? and fase=?) where rapporttype='TS' and locnaam is null order by document_id>;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($locnaam,$faseid);
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select name="addfaselocprotsel" id="addfaselocprotsel" >
EOT2

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq! : ! . $$row[1] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= qq!</select>\n!;
    $html .= <<EOT3;
<a class ="select" onclick="return loadaddfaselocprot_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT3
    return($html);
}

sub do_info_fases { 
    my $faseid = shift;
    my $dbh = shift;
    my $sql = qq< select f.locatie as "Locatie",beschrijving as "Locatie omschrijving",opmerkingen as "Opmerking bij verificatie" from fase_loc as f join locaties as l on(f.locatie=l.locnaam) where faseid = ? order by f.locatie>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($faseid);
	my $html = toontabel($sth,"Locaties in fasering $faseid",0,"info_fases");
    $html .= <<EOT;
<input type="hidden" name="faselocprotval" id="faselocprotval">
<div id= "d_addfaseloc"></div>
<input type='submit' value='Refresh' onclick="return info_fases_refresh();">
EOT
    $html .= printbaar('info_fases',$faseid);
    $html .= <<EOT;
<a class="select" onclick="return info_fases_add();">Koppel locatie</a>
EOT
    return $html;    
}

sub info_fases {
    return do_info_fases(remove_escapes(shift),dbi_connect());
}

sub insfaseloc {
    my $locnaam = remove_escapes(shift);
    my $remark = remove_escapes(shift);
    my $faseid = remove_escapes(shift);
    my $sqlins = qq< insert into fase_loc (faseid,locatie,opmerkingen) values (?,?,?) >;
    my $dbh = dbi_connect();
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($faseid,$locnaam,$remark);
    return do_info_fases($faseid,$dbh);
}

sub loadaddfaseloc {
    my $faseid = remove_escapes(shift);
    my $sql = qq< select locnaam from locaties left join fase_loc on (locnaam=locatie and faseid=?) where locatie is null order by locnaam>;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($faseid);
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select name="addfaselocsel" id="addfaselocsel" >
EOT2

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= <<EOT3;
</select>
<input type="text" id="addfaselocrem" width=20>
<a class="select" onclick="return loadaddfaseloc_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT3
    return($html);
}

my %sql_tab = (
	"laad_protocollen"=> qq< select di as "DI",document_id as "Document Id",document_titel as "Titel" from documents where rapporttype='TS' and document_id != 'Dummyprotocol' order by di,document_id>,
	"laad_fasering"=> qq< select faseid as "Faseid",fasename as "Fase omschrijving",milestone as "Milestone" from fases order by milestone >,
    "laad_rapporten" => qq< select di,document_id,document_titel from documents where rapporttype='TR' order by di,document_id>,
    "laad_lokaties" => qq< select locnaam,beschrijving from locaties order by locnaam>
	);

sub laad_tab {
	my ($naam, $veld, $hidden) = @_;
    $naam = remove_escapes($naam);
    $veld = remove_escapes($veld);
    $hidden = remove_escapes($hidden) if defined($hidden);
	my $sql = $sql_tab{$naam};
	
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = toontabel($sth,undef,$veld,$naam);
	$html .= qq!<input type="hidden" id="$hidden">\n! if defined($hidden) && $hidden ne '';
    $html .= qq!<input type='submit' value='Refresh' onclick="return !.$naam.qq!_refresh();">\n!;
    $html .= printbaar($naam);
    return $html;
}

sub laad_fasering {
	return laad_tab("laad_fasering",0,"faselocval");
}

sub laad_tabsel {
    my $naam = remove_escapes(shift);
    my $veld1 = remove_escapes(shift);
    my $veld2 = remove_escapes(shift);
    my $w = remove_escapes(shift);
    my $addbut = remove_escapes(shift);
    
	my $sql = $sql_tab{$naam};
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = qq!<select id="$veld2" name="$veld2" size=20 style="width:$w; height:600px" onclick="return !.$naam.qq!_onclick();">\n!;
    while (my @row=$sth->fetchrow_array()) {
        foreach (@row) {
            $_ = (defined($_)?$_:'&nbsp');
        }
        $html .= qq!<option>!.join(" : ",@row).qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<input type="hidden" id="$veld1">
EOT
    $html .= qq!<div id="d_add$addbut"></div>\n! if defined $addbut;
    $html .= <<EOT;
<p>
<input type='submit' value='Refresh' onclick="return ${naam}_refresh();">
EOT
    $html .= printbaar($naam);
    if (defined($addbut)) {
        $html .= <<EOT;
<a class="select" onclick="return ${naam}_add();">Voeg $addbut toe</a>
EOT
    }
    return $html;    
}

sub addlocatie {
    my $dummy = shift; # dummy argument
    my $html = <<EOT;
<table border=0>
<tr><th>Locatie id</th><th>Beschrijving</th><th></th></tr>
<tr>
<td><input id="addlocatie_id" type="text" width="20"></td>
<td><input id="addlocatie_beschr" type="text" width="40"></td>
<td><a class="select" onclick="return loadaddlocatie();">Voeg toe</a></td>
</tr>
</table>
EOT
    return $html;
}

sub inslocatie {
    my $locid = remove_escapes(shift);
    my $locbeschr = remove_escapes(shift);
    my $sql = qq< insert into locaties (locnaam,beschrijving) values (?,?) >;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid,$locbeschr);
    return laad_tabsel("laad_lokaties","locid","lokatie","398px","locatie");
}

sub laad_lokaties {
    return laad_tabsel("laad_lokaties","locid","lokatie","398px","locatie");
}

sub laad_rapporten {
    return laad_tabsel("laad_rapporten","reportid","rapport","670px");
}

sub laad_protocollen {
	return laad_tabsel("laad_protocollen",'protocolid',"protocol","670px");
}

sub do_info_prot_eisen {
    my $protid = shift;
    my $dbh = shift;
	my $sql = qq< select eis.eis as "Eis", eis.eistekst as "Eis tekst", pe.di as "DI",soort as "Soort eis", eistype as "Type eis", status as "Status" from protocol_eis as pe join unieke_eisen as eis on (pe.eis=eis.eis) where protocol_doc_id=? order by eis.eis,pe.di>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($protid);
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = toontabel($sth,"Te verifieren eisen van protocol $protid");
    $html .= <<EOT;
<div id= "d_addproteis"></div>
<div id= "d_addproteis_di"></div>
<input type='submit' value='Refresh' onclick="return info_prot_eisen_refresh();">
EOT
    return $html;
}

sub info_prot_eisen {
    return do_info_prot_eisen(remove_escapes(shift),dbi_connect());
}

sub loadprotstatus {
    my $protocolid = remove_escapes(shift);
    my $sql = qq< select status from docstatus >;
    
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my $html = <<EOT;
<table>
<tr>
<td>
<select id="modprotstatussel" >
EOT

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= qq!</select>\n!;
    $html .= <<EOT;
<a class="select" onclick="return loadmodprotstatus_mod();">Wijzig</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_prot_loc {
    my $protocolid=shift;
    my $dbh=shift;
    
    my $sql = qq< select lp.locnaam as "Locatie",beschrijving as "Locatie omschrijving",fase as "Fasering" from locatie_protocol as lp  join locaties as l on (l.locnaam=lp.locnaam) where protocol_doc_id=? order by 1,3 >;
    my $sth = $dbh->prepare($sql);
    $sth->execute($protocolid);
	my $html = toontabel($sth,"Lokaties voor protocol $protocolid");
    $html .= <<EOT;
<div id= "d_addprotloc"></div>
<input type='submit' value='Refresh' onclick="return info_prot_loc_refresh();">
<a class="select" onclick="return info_prot_loc_add();">Koppel locatie</a>
EOT
    return $html;
};

sub info_prot_loc {
    return do_info_prot_loc(remove_escapes(shift),dbi_connect());
}

sub insprotloc {
    my $locid=shift;
    my $faseid = shift;
    my $protocolid=shift;
    my $dbh = dbi_connect();
    my $sql = qq < insert into locatie_protocol (locnaam,fase,protocol_doc_id) values (?,?,?) >;
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid,$faseid,$protocolid);
    return do_info_prot_loc($protocolid,$dbh);
}

sub loadaddprotloc {
    my $protocolid = remove_escapes(shift);
    my $sqlp = qq< select l.locnaam,l.beschrijving from locaties as l order by l.locnaam>;
    my $dbh = dbi_connect();
    my $sthp = $dbh->prepare($sqlp);
    $sthp->execute();
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select id="addprotlocsel" onclick="return info_prot_loc_fase_add();">
EOT2
    while ( my $row = $sthp->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
	$html .= <<EOT;
</select>
</td>
<td>
<div id= "d_addprotloc_fase"></div>
</td>
</table>
EOT
    return $html;
}

sub loadaddprotlocfase {
    my $protocolid = remove_escapes(shift);
    my $locid = remove_escapes(shift);
    my $sqlf =qq<select distinct faseid from fase_loc where locatie=?>;
    my $dbh = dbi_connect();
    my $sthf = $dbh->prepare($sqlf);
    my $html ="";
    $sthf->execute($locid);
    $html .= <<EOT;
<select id="addprotlocfase">
EOT
    while ( my $row = $sthf->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
EOT
    if ($sthf->rows > 0) {
        $html .= <<EOT;
</td><td><a class="select" onclick="return loadaddprotloc_add();">Voeg toe</a>
EOT
    }
    return($html);
}

sub do_info_prot_info {
    my $protocolid = shift;
    my $dbh = shift;
    
    my $sql = qq< select di as "DI",testfase as "Testfase",document_id as "Document Id","ImandraID" as "Imandra Id",document_titel as "Titel", docstatus as "Document status",link from documents where document_id= ? >;
    my $sth = $dbh->prepare($sql);
    $sth->execute($protocolid);
	my $html = toontabel($sth,"Informatie over testprotocol $protocolid\n",undef,undef,"4,6");
    $html .= <<EOT1;
<div id="d_protocol_status"></div>
<input type='submit' value='Refresh' onclick="return info_prot_info_refresh();">
<a class="select" onclick="return info_prot_status_mod();">Pas status aan</a>
EOT1
    return $html;
}

sub info_prot_info {
    return do_info_prot_info(remove_escapes(shift),dbi_connect());
}

sub modprotstatus {
    my $status=remove_escapes(shift);
    my $protocolid=remove_escapes(shift);

    my $sqlupd = qq< update documents set docstatus=? where document_id=? >;
    my $dbh = dbi_connect();
    my $sthupd = $dbh->prepare($sqlupd);
    $sthupd->execute($status,$protocolid);
    return do_info_prot_info($protocolid,$dbh);
}

sub do_info_report_loc {
    my $reportid = shift;
    my $dbh = shift;
    my $sql = qq< select lr.locnaam as "Locatie",l.beschrijving as "Beschrijving",lr.faseid as "Fasering", fl.opmerkingen as "Opmerkingen" from locatie_report as lr join locaties as l on (lr.locnaam=l.locnaam) join testrapport as tr on (trid=report_doc_id and tr.faseid=lr.faseid) join fase_loc as fl on (fl.faseid=tr.faseid and lr.locnaam=locatie) where report_doc_id=? order by 1>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
	my $html = toontabel($sth,"Lokaties voor rapport $reportid");
    $html .= <<EOT;
<div id= "d_addreportloc"></div>
<input type='submit' value='Refresh' onclick="return info_report_loc_refresh();">
<a class="select" onclick="return info_report_loc_add();">Koppel locatie</a>
EOT
    return $html;
}

sub info_report_loc {
    return do_info_report_loc(remove_escapes(shift),dbi_connect());
}

sub insreportloc {
    my $locid = remove_escapes(shift);
	my $faseid= remove_escapes(shift);
    my $invoer = remove_escapes(shift);
    my ($reportid,$titel) = split(' : ',$invoer);
    my $sqlins = qq< insert into locatie_report(report_doc_id,locnaam,faseid) values (?,?,?) >;
    my $dbh = dbi_connect();
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($reportid,$locid,$faseid);
    return do_info_report_loc($reportid,$dbh);
}

sub loadaddreportloc {
    my $reportid = remove_escapes(shift);
    my $sqlp = qq< select l.locnaam,l.beschrijving from locaties as l join fase_loc as fl on (l.locnaam=fl.locatie) join testrapport as tr on (tr.faseid=fl.faseid and trid=?) order by l.locnaam>;
    my $sqlf =qq<select distinct t.faseid as "Fasering",fasename as "Fasering omschrijving",milestone as "Milestone" from testrapport as t join fases as f on (t.faseid=f.faseid)  where trid=? order by milestone>;
    my $dbh = dbi_connect();
    my $sthp = $dbh->prepare($sqlp);
    my $sthf = $dbh->prepare($sqlf);
    $sthp->execute($reportid);
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select id="addreportlocsel" >
EOT2
    while ( my $row = $sthp->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
	$html .= <<EOT;
</select>
<select id="addreportlocfase">
EOT
    $sthf->execute($reportid);
    while ( my $row = $sthf->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<a class="select" onclick="return loadaddreportloc_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_report_prot {
    my $reportid = shift;
    my $dbh = shift;
    my $sql = qq< select prot_id as "Protocol Id",document_titel as "Titel",testfase as "Testfase", faseid as "Fasering",link from testrapport join documents as tp on (prot_id=document_id) where trid=? and prot_id != 'Dummyprotocol' order by prot_id,faseid>;

    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = toontabel($sth,"Protocol voor rapport $reportid",undef,undef,"1,4");
    $html .= <<EOT;
<div id= "d_addreportprot"></div>
<input type='submit' value='Refresh' onclick="return info_report_prot_refresh();">
<a class="select" onclick="return info_report_prot_add();">Koppel protocol</a>
EOT
    return $html;
}

sub info_report_prot {
    my $reportid = remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_report_prot($reportid,$dbh);
}

sub insreportprot {
    my $invoer = remove_escapes(shift);
    my $reportid = remove_escapes(shift);

    my ($protid,$titel,$faseid) = split(' : ',$invoer);

    my $dbh = dbi_connect();
	my $sqlupd = qq< update testrapport set prot_id = ? where trid=? and faseid=? and prot_id ='Dummyprotocol' >;
	my $sthupd = $dbh->prepare($sqlupd);
	$sthupd->execute($protid,$reportid,$faseid);
    # Todo: controleren of geslaagd dan volgende stap niet nodig
    my $sqlins = qq< insert into testrapport(trid,prot_id,faseid) values (?,?,?) >;
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($reportid,$protid,$faseid);
    return do_info_report_prot($reportid,$dbh);
}

sub loadaddreportprot {
    my $reportid = remove_escapes(shift);
    my $sql = qq< select distinct document_id , document_titel, tr.faseid from testrapport as tr join locatie_report as lr on (trid=report_doc_id and tr.faseid=lr.faseid) join locatie_protocol as fl on ( tr.faseid=fl.fase and fl.locnaam=lr.locnaam) join documents as d on (fl.protocol_doc_id=document_id) where trid=? and not protocol_doc_id in (select prot_id from testrapport where trid=tr.trid and prot_id != 'Dummyprotocol' and faseid=tr.faseid) order by document_id >;
    
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
    my $html = <<EOT;
<table>
<tr>
<td>
<select id="addreportprotsel" >
EOT
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] .' : '.$$row[1].' : '.$$row[2]. qq!</option>\n!;
    }
    # close off the select and return
    $html .= <<EOT;
</select>
EOT
    $html .= <<EOT;
<a class="select" onclick="return loadaddreportprot_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_report_eisen {
    my $reportid = shift;
    my $dbh = shift;
	my $sql = qq< select eis.eis as "Eis", eis.eistekst as "Eis tekst", pe.di as "DI",soort as "Soort eis", eistype as "Type eis", status as "Status", lr.locnaam as "Locatie", lr.faseid as "Fasering", verificatie_akkoord as "Verificatie akkoord", niet_akkoord_reden as "Niet akkoord omdat", verificator as "Wie heeft verificatie uitgevoerd", opmerking_bij_verificatie as "Opmerkingen bij verificatie" from testrapport as tr join locatie_report as lr on (trid=report_doc_id and lr.faseid=tr.faseid) join fase_loc as fl on (fl.faseid=tr.faseid and fl.locatie=lr.locnaam) join locatie_protocol as lp on (lp.locnaam=lr.locnaam and lp.protocol_doc_id=tr.prot_id and lp.fase=tr.faseid) join protocol_eis as pe on (pe.protocol_doc_id=lp.protocol_doc_id) join documents as dr on (tr.trid=dr.document_id) join unieke_eisen as eis on (pe.eis=eis.eis) join report_locatie_eis as rle on (trid=rle.report_doc_id and lr.locnaam=rle.locnaam and eis.eis=rle.eis) where trid=? order by lr.locnaam,eis.eis,fl.faseid>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = toontabel($sth,"Geverifieerde eisen van rapport $reportid");
    $html .= <<EOT;
<div id= "d_addreporteis"></div>
<div id= "d_addreporteis_di"></div>
<div id= "d_addreporteis_loc"></div>
<div id= "d_addreporteis_fase"></div>
<div id= "d_addreporteis_v_acc"></div>
<div id= "d_addreporteis_v_n"></div>
<div id= "d_addreporteis_v_v"></div>
<div id= "d_addreporteis_v_o"></div>
<input type='submit' value='Refresh' onclick="return info_report_eisen_refresh();">
<a class="select" onclick="return info_report_eisen_add();">Nieuwe verificatie</a>
EOT
    return $html;
}

sub info_report_eisen {
    my $reportid = remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_report_eisen($reportid,$dbh);
}


sub do_info_report_fase {
    my $reportid = shift;
    my $dbh = shift;
    my $sql = qq< select distinct t.faseid as "Fasering",fasename as "Fasering omschrijving",milestone as "Milestone" from testrapport as t join fases as f on (t.faseid=f.faseid)  where trid=? order by milestone>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
	my $html = toontabel($sth,"Faseringen voor rapport $reportid");
    $html .= <<EOT;
<div id= "d_addreportfase"></div>
<input type='submit' value='Refresh' onclick="return info_report_fase_refresh();">
<a class="select" onclick="return info_report_fase_add();">Koppel fasering</a>
EOT
    return $html;
}

sub info_report_fase {
    return do_info_report_fase(remove_escapes(shift),dbi_connect());
}

sub insreportfase {
    my $faseid = remove_escapes(shift);
    my $reportid = remove_escapes(shift);

    my $dbh = dbi_connect();
    my $sqlupd = qq< update testrapport set faseid=? where faseid is NULL and trid = ? >;
    my $sthupd = $dbh->prepare($sqlupd);
    $sthupd->execute($faseid,$reportid);
    # Todo check of gelukt, dan is volgende niet nodig.
    my $sqlins = qq< insert into testrapport (faseid,trid) values (?,?) >;
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($faseid,$reportid);
    return do_info_report_fase($reportid,$dbh);
}

sub loadaddreportfase {
    my $reportid = remove_escapes(shift);
    my $sql = qq< select f.faseid from fases as f left join testrapport as tr on (f.faseid=tr.faseid and trid=?) where tr.faseid is null order by f.faseid>;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select id="addreportfasesel" >
EOT2

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= qq!</select>\n!;
    $html .= <<EOT3;
<a class="select" onclick="return loadaddreportfase_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT3
    return($html);
}

sub loadreportstatus {
    my $reportid = remove_escapes(shift);
    my $sql = qq< select status from docstatus >;
    
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my $html = <<EOT;
<table>
<tr>
<td>
<select id="modreportstatussel" >
EOT

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= qq!</select>\n!;
    $html .= <<EOT;
<a class="select" onclick="return loadmodreportstatus_mod();">Wijzig</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_report_info {
    my $reportid = shift;
    my $dbh = shift;
    
    my $sql = qq< select di as "DI",testfase as "Testfase",document_id as "Document Id","ImandraID" as "Imandra Id",document_titel as "Titel", docstatus as "Document status",link from documents where document_id= ? >;
    my $sth = $dbh->prepare($sql);
    $sth->execute($reportid);
	my $html = toontabel($sth,"Informatie over testrapport $reportid\n",undef,undef,"4,6");
    $html .= <<EOT1;
<div id="d_report_status"></div>
<input type='submit' value='Refresh' onclick="return info_report_info_refresh();">
<a class="select" onclick="return info_report_status_mod();">Pas status aan</a>
EOT1
    return $html;
}

sub info_report_info {
    my $reportid = remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_report_info($reportid,$dbh);
}

sub modreportstatus {
    my $status=remove_escapes(shift);
    my $reportid=remove_escapes(shift);

    my $sqlupd = qq< update documents set docstatus=? where document_id=? >;
    my $dbh = dbi_connect();
    my $sthupd = $dbh->prepare($sqlupd);
    $sthupd->execute($status,$reportid);
    return do_info_report_info($reportid,$dbh);
} 

sub do_info_locreport {
    my ($locid,$dbh) = @_;
    my $sql = qq< select d.di as "DI",report_doc_id as "Document Id",d.document_titel as "Titel",d."ImandraID" as "Imandra Id",d.testfase as "Testfase",l.faseid as "Fasering",link from locatie_report as l join testrapport as t on (t.trid=l.report_doc_id and l.faseid=t.faseid) join documents d on (d.document_id=trid) join fase_loc as f on (f.faseid=t.faseid and locatie=locnaam) where locnaam = ? order by d.di,report_doc_id>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid);
    my @names = @{ $sth->{NAME} };
    my $numfields = $sth->{NUM_OF_FIELDS};
    my $html = toontabel($sth,"Rapporten voor locatie $locid",undef,undef,"2,6");
    $html .= <<EOT;
<div id= "d_addlocreport"></div>
 <input type='submit' id='locreportrefresh' value='Refresh' onclick="return info_locreport_refresh();">
 <a class="select" onclick="return info_locreport_add();">Koppel Rapport</a>
EOT
    return $html;    
}

sub info_locreport {
    my $locid = remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_locreport($locid,$dbh);
};

sub inslocreport {
  my $invoer=remove_escapes(shift);
  my $faseid=remove_escapes(shift);
  my $locnaam=remove_escapes(shift);
  my $sqlins = qq< insert into locatie_report (locnaam,report_doc_id,faseid) values (?,?,?) >;
  my ($rapport,$titel) = split(' : ',$invoer);
  my $dbh = dbi_connect();
  my $sthins = $dbh->prepare($sqlins);
  $sthins->execute($locnaam,$rapport,$faseid);
#  print STDERR "$sqlins : $invoer : $locnaam : $rapport\n";
  return do_info_locreport($locnaam,$dbh);
}

sub loadaddlocreport {
    my $locnaam = remove_escapes(shift);

    my $sqlp = qq< select distinct document_id,document_titel from documents where rapporttype='TR' order by document_id>;
    my $sqlf = qq<select distinct faseid from fase_loc where locatie = ? order by faseid>;

    my $dbh = dbi_connect();
    my $sthp = $dbh->prepare($sqlp);
    my $sthf = $dbh->prepare($sqlf);
    $sthp->execute();
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select id="addlocreportsel" >
EOT2
    while ( my $row = $sthp->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq! : !. $$row[1] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<select id="addlocreportfase" >
EOT
    $sthf->execute($locnaam);
    while ( my $row = $sthf->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<a class="select" onclick="return loadaddlocreport_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_locprot {
    my $locid = shift;
    my $dbh = shift;
    my $sql = qq< select d.di as "DI",d.document_id as "Document Id",d.document_titel as "Titel","ImandraID" as "Imandra Id",testfase as "Testfase",fase as "Fasering",link from locatie_protocol as l join documents as d on (d.document_id=protocol_doc_id) WHERE locnaam=? AND document_id != 'Dummyprotocol' order by d.di,document_id,fase>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid);
	my $html = toontabel($sth,"Protocollen voor locatie $locid",undef,undef,"2,6");
    $html .= <<EOT1;
<div id= "d_addlocprot"></div>
<input type='submit' value='Refresh' onclick="return info_locprot_refresh();">
<a class="select" onclick="return info_locprot_add();">Koppel Protocol</a>
EOT1
    return $html;    
}

sub info_locprot {
    my $locid=remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_locprot($locid,$dbh);
}

sub inslocprot {
  my $invoer=remove_escapes(shift);
  my $faseid=remove_escapes(shift);
  my $locnaam=remove_escapes(shift);
  my $sqlins = qq< insert into locatie_protocol (fase,locnaam,protocol_doc_id) values (?,?,?) >;
  my ($protocol,$titel) = split(' : ',$invoer);
  my $dbh = dbi_connect();
  my $sthins = $dbh->prepare($sqlins);
  $sthins->execute($faseid,$locnaam,$protocol);
  return do_info_locprot($locnaam,$dbh);
}

sub loadaddlocprot {
    my $locnaam = remove_escapes(shift);
#	my $sql = qq< select f.faseid from fases as f left join fase_loc as fl on (f.faseid=fl.faseid and locatie=?) where fl.faseid is null order by f.faseid>;
    my $sqlp = qq< select distinct document_id,document_titel from documents where rapporttype='TS' order by document_id>;
    my $sqlf = qq<select distinct faseid from fase_loc where locatie = ? order by faseid>;
    my $dbh = dbi_connect();
    my $sthp = $dbh->prepare($sqlp);
    my $sthf = $dbh->prepare($sqlf);
    $sthp->execute();
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select name="addlocprotsel" id="addlocprotsel" >
EOT2
    while ( my $row = $sthp->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq! : !. $$row[1] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<select name="addlocprotfase" id="addlocprotfase" >
EOT
    $sthf->execute($locnaam);
    while ( my $row = $sthf->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    $html .= <<EOT;
</select>
<a class="select" onclick="return loadaddlocprot_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT
    return($html);
}

sub do_info_locfase {
    my $locid = shift;
    my $dbh = shift;
    my $sql = qq< select l.faseid as "Fasering",fasename as "Fasering omschrijving",opmerkingen as "Opmerkingen",milestone as "Milestone" from fase_loc as l join fases as f on (l.faseid=f.faseid)  where locatie=? order by milestone>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid);
	my $html = toontabel($sth,qq!Faseringen voor locatie $locid!);
    $html .= <<EOT1;
<div id= "d_addlocfase"></div>
<input type='submit' value='Refresh' onclick="return info_locfase_refresh();">
<a class="select" onclick="return info_locfase_add();">Koppel fasering</a>
EOT1
    return $html;
}

sub info_locfase {
    my $locid = remove_escapes(shift);
    my $dbh = dbi_connect();
    return do_info_locfase($locid,$dbh);
}

sub inslocfase {
    my $faseid = remove_escapes(shift);
    my $remark = remove_escapes(shift);
    my $locnaam = remove_escapes(shift);
    my $sqlins = qq< insert into fase_loc (faseid,locatie,opmerkingen) values (?,?,?) >;
    my $dbh = dbi_connect();
    my $sthins = $dbh->prepare($sqlins);
    $sthins->execute($faseid,$locnaam,$remark);
    return do_info_locfase($locnaam,$dbh);
};

sub loadaddlocfase {
    my $locnaam = remove_escapes(shift);
    my $sql = qq< select f.faseid from fases as f left join fase_loc as fl on (f.faseid=fl.faseid and locatie=?) where fl.faseid is null order by f.faseid>;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($locnaam);
    my $html = <<EOT2 ;
<table>
<tr>
<td>
<select name="addlocfasesel" id="addlocfasesel" >
EOT2

    # dot on each option from the db
    while ( my $row = $sth->fetchrow_arrayref() ) {
        $html .= qq!<option>! . $$row[0] . qq!</option>\n!;
    }
    # close off the select and return
    $html .= qq!</select>\n!;
    $html .= <<EOT3;
<input type="text" id="addlocfaserem" width=20>
<a class="select" onclick="return loadaddlocfase_add();">Voeg toe</a>
</td>
</tr>
</table>
EOT3
    return($html);
}

sub info_locinfo {
    my $locid = remove_escapes(shift);
    my $dbh = dbi_connect();
    my $sql = qq< select locnaam as "Locatie",beschrijving as "Beschrijving" from locaties where locnaam=?>;
    my $sth = $dbh->prepare($sql);
    $sth->execute($locid);
	my $html = toontabel($sth,"Informatie over locatie $locid");
    $html .= <<EOT1;
<input type='submit' value='Refresh' onclick="return info_locinfo_refresh();">
EOT1
    return $html;
}

my %laad_sql = (
			'searchtermfasering' => q!select faseid,fasename,milestone from fases order by milestone!,
			'searchtermdi' => q!select distinct objid,objname from objecten join eis_di on (di=objid) order by objid!,
			'searchtermlokatie' => q!select locnaam,beschrijving from locaties order by locnaam!
			);

sub laad_search{
	my $naam = shift;
    my $dbh = dbi_connect();
    
    my $sql = $laad_sql{$naam};
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my @fases;
    push @fases,$naam;
    while (my @row=$sth->fetchrow_array()) {
        push @fases,shift @row;
        push @fases,join(" - ",map {(defined($_)?$_:'')} @row);
    }
    return @fases;
}

sub laad_searchfasering {
	return laad_search('searchtermfasering');
}

sub laad_searchlocatie {
	return laad_search('searchtermlokatie');
}

sub laad_searchdi {
	return laad_search('searchtermdi');
}

sub refresh_func {
    my ($naam,$divload,$divwis,$veld) = @_;
	if (substr($naam,0,1) eq "-") {
		return "";
	}

    my $html = "function ".$naam."_refresh() {\n";

    if (defined($divload)) {
      if (ref($divload) eq "ARRAY") {
        $html .= "  loaddivs(['".join("','",@{$divload})."']);\n";
      } else {
        $html .= "  loaddiv('$divload');\n";
      }
    }
    
    if (defined($divwis)) {
      if (ref($divwis) eq "ARRAY") {
        $html .= "  wisdivs(['".join("','",@{$divwis})."']);\n";
      } else {
        $html .= "  wisdiv('$divwis');\n";
      }
    }
    $html .= "  ".$naam."(['";
    if (ref($veld) eq "ARRAY") {
        $html .= join("','",@{$veld});
    } else {
        $html .= $veld;
    }
    $html .= "'],['$divload'],'POST');\n";
    $html .= "  return true;\n}\n\n";
    return $html;
}

sub onclick_func {
    my ($naam,$divload,$divwis,$veldnaam,$velden,$functie,$splits,$splitsvld) = @_;
 
    my @d;
    my $html = "function ".$naam."_onclick(";
    $html .= "veld" unless defined $splits;
    $splitsvld = 0 unless defined $splitsvld;
    $html .= ") {\n";
    if (defined($divload)) {
      if (ref($divload) eq "ARRAY") {
        @d=@{$divload};
        $html .= "  loaddivs(['".join("','",@{$divload})."']);\n";
      } else {
        push @d,$divload;
        $html .= "  loaddiv('$divload');\n";
      }
    }

    if (defined($divwis)) {
      if (ref($divwis) eq "ARRAY") {
        $html .= "  wisdivs(['".join("','",@{$divwis})."']);\n";
      } else {
        $html .= "  wisdiv('$divwis');\n";
      }
    }
    
    if (defined($splits)) {
      $html .= "  var doc = document.getElementById('$splits');\n";
      $html .= "  var ind = doc.selectedIndex;\n";
      $html .= "  var t = doc.options[ind].text;\n";
      $html .= "  var t1 = t.replace(/%20/gi,' ').replace(/%2A/gi,':').replace(/%2C/gi,',').replace(/&amp;/gi,'&');\n";
      $html .= "  var temp = t1.split(' : ');\n";
      $html .= "  document.getElementById('$veldnaam').value=temp[$splitsvld];\n";
    } else {
      $html .= "  document.getElementById('$veldnaam').value=veld;\n";
    }
    $velden = $veldnaam unless defined($velden);

    my @f;
    if (ref($functie) eq "ARRAY") {
      @f = @{$functie};
    } else {
      $f[0] = $functie;
    }
    
    my $count = $#f;
    $count = $#d if $#d < $count;
    
    for(my $i=0;$i<=$count;$i++) {
	  my $f1 = $f[$i];
	  $f1 = substr($f1,1) if (substr($f1,0,1) eq '-');
      $html .= "  ".$f1."(['";
      if (ref($velden) eq "ARRAY") {
          $html .= join("','",@{$velden});
      } else {
          $html .= $velden;
      }
      $html .= "'],['".$d[$i]."'],'POST');\n";
    }
    $html .= "  return true;\n}\n\n";
    return $html;
}

sub dummy {
  return "<p>Nog niet geimplementeerd</p>";
};

sub deftab {
    my $tabdefs = shift;
    my $html = qq!<script type="text/javascript">\n!;
    
    foreach my $naam (keys %$tabdefs) {
        my %defs = %{$tabdefs->{$naam}};
        my $hoofdveld = $defs{hoofdveld};
        my $laadvelden = $defs{laadvelden};
        my $wisvelden = $defs{wisvelden};
        my $functions = $defs{functions};
        my $args = $defs{args};
        my $invoer1 = $defs{invoer1};
        my $invoer2 = $defs{invoer2};
        my $veldnr = $defs{veldnr};
    
        $html .= onclick_func($naam,$laadvelden,$wisvelden,$invoer1,$args,$functions,$invoer2,$veldnr);
        my $allevelden;
        if (defined($wisvelden)) {
            $allevelden = [ @$laadvelden, @$wisvelden];
        } else {
            $allevelden = $laadvelden;
        }
        my @v=();
        if (defined($laadvelden)) {
            my $count1 = $#{$args};
            for (my $i=0;$i<$count1;$i++) {
                push @v,$$args[$i];
            }
        }
        if ($#v < 0) {
			push @v,'searchterm';
        }
        $html .= refresh_func($naam,$hoofdveld,$allevelden,\@v);
        if (defined($laadvelden)) {
            my $count = $#$laadvelden;
            for (my $i=0;$i<=$count;$i++) {
                $html .= refresh_func($$functions[$i],$$laadvelden[$i],undef,(defined($args)&&$#$args>=0?$args:$invoer1)) if (defined($$laadvelden[$i]) && defined($$functions[$i]));
            }
        }
    }
	$html .= "</script>\n";
    return $html;
}

sub Show_Form {
    my $html = "";
    $html .= <<EOT;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<HTML>
    <HEAD>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>VTTI Eisen</title>
	<link rel="stylesheet" href="css/tab-view.css" type="text/css" media="screen">
    <link rel="stylesheet" href="croon.css" type="text/css" media="screen">
	<script type="text/javascript" src="js/ajax.js"> </script>
	<script type="text/javascript" src="js/tab-view.js"> </script>
    <script type="text/javascript" src="croon.js"></script>
EOT

    my %tabdefs = (
        'laad_protocollen' => {hoofdveld =>'d_proto',
                               laadvelden=>['d_prot_info','d_prot_fase','d_prot_loc','d_prot_report','d_prot_eisen'],
                               functions=>['info_prot_info','info_prot_fase','info_prot_loc','info_prot_report','info_prot_eisen'],
                               invoer1=>'protocolid',
                               invoer2=>'protocol',
                               veldnr=>1},
        'laad_lokaties' =>    {hoofdveld=>'d_loc',
                               laadvelden=>['d_loc_info','d_loc_fase','d_loc_prot','d_loc_report'],
                               functions=>['info_locinfo','info_locfase','info_locprot','info_locreport'],
                               invoer1=>'locid',
                               invoer2=>'lokatie',
                               veldnr=>0},
        'laad_rapporten' =>   {hoofdveld=>'d_report',
                               laadvelden=>['d_report_info','d_report_fase','d_report_loc','d_report_prot','d_report_eisen'],
                               functions=>['info_report_info','info_report_fase','info_report_loc','info_report_prot','info_report_eisen'],
                               invoer1=>'reportid',
                               invoer2=>'rapport',
                               veldnr=>1},
        'laad_fasering'=>     {hoofdveld=>'d_fase',
                               laadvelden=>['d_faseloc'],
                               wisvelden=>['d_faselocprot','d_faselocrep','d_faselocproteis'],
                               functions=>['-info_fases'],
                               invoer1=>'faselocval'},
	    'info_fases'=>        {hoofdveld=>'d_faseloc',
                               laadvelden=>['d_faselocprot','d_faselocrep'],
                               wisvelden=>['d_faselocproteis'],
                               functions=>['-info_fases_locs','info_fases_locs_reps'],
                               args=>['faselocval','faselocprotval'],
                               invoer1=>'faselocprotval'},
	    'info_fases_locs'=>   {hoofdveld=>'d_faselocprot',
                               laadvelden=>['d_faselocproteis'],
                               functions=>['info_fases_locs_eisen'],
                               args=>['faselocval','faselocprotval','faselocproteisval'],
                               invoer1=>'faselocproteisval'}
                 );
    $html .= deftab(\%tabdefs);   
	
    $html .= <<EOT;
    </HEAD>
    <BODY onload="croon_initialize();">
	<div id="dhtmlgoodies_tabView1">
	<div class="dhtmlgoodies_aTab">
        Vul een (deel van de) eis of een DI nummer in het vak hieronder en klik daarna op de gewenste eis (ook als het er maar 1 is).
        <br>
        <form>
            <p>
            <input type="hidden" name="nieuweeis" id="nieuweeis">
            Eiscode: <input type="text" name="searchterm" id="searchterm" size="16">
            <a onclick="return eisen_zoek_eiscode();" style="color: #0000FF" class="select"> Zoek </a>
            DI: <select id="searchtermdi" name="searchtermdi" SIZE=1 style="width:200px;">
            <option>Loading....</option>
            </select>
            <a onclick="return eisen_zoek_di();" style="color: #0000FF" class="select"> Zoek </a>
            <a onclick="return eisen_vmx_di();" style="color: #0000FF" class="select"> VMX </a>
            Eistekst: <input type="text" name="searchtermtekst" id="searchtermtekst" size="16">
            <a onclick="return eisen_zoek_eistekst();" style="color: #0000FF" class="select"> Zoek </a>
            Fasering: <select id="searchtermfasering" name="searchtermfasering" SIZE=1 style="width:200px;">
            <option>Loading....</option>
            </select>
            <a onclick="return eisen_zoek_fase();" style="color: #0000FF" class="select"> Zoek </a>
            <a onclick="return eisen_vmx_fase();" style="color: #0000FF" class="select"> VMX </a>
            Lokatie: <select id="searchtermlokatie" name="searchtermlokatie" SIZE=1 style="width:200px;">
            <option>Loading....</option>
            </select>
            <a onclick="return eisen_zoek_lokatie();" style="color: #0000FF" class="select"> Zoek </a>
            <a onclick="return eisen_vmx_lokatie();" style="color: #0000FF" class="select"> VMX </a>
            </p>
            <table><tr valign='top'>
                <td><div id="d_inpeis" style="width: 270px; height: 600px; overflow: auto"></div></td>
                <td><table border=0>
                <tr><td><div id="d_eisprop" style="width: 1300px" ></div><br>
                <tr><td><div id="d_eisvmx" style="width: 1300px" ></div><br>
                <tr><td><div id="d_eisdet" style="width: 1300px" ></div></td></tr>
                </table></td>
                </tr>
            </table>
        </form>
    	<div id="d_vmx"  style="width: 1200px" ></div>
	</div>
	<div class="dhtmlgoodies_aTab">
	<table border=0>
	<tr valign='top'>
	<td rowspan=5><div id="d_loc" style="width:400px">Loading....</div></td>
	<tr valign='top'><td><div id='d_loc_info'></div></td>
	<tr valign='top'><td><div id='d_loc_fase'></div></td>
	<tr valign='top'><td><div id='d_loc_prot'></div></td>
	<tr valign='top'><td><div id='d_loc_report'></div></td>
	</table>
	</div>
	<div class="dhtmlgoodies_aTab">
	<table border=0>
	<tr valign='top'>
	<td rowspan=7><div id="d_comp" style="width:675px">Loading....</div></td>
	<tr valign='top'><td><div id='d_comp_info'></div></td>
	<tr valign='top'><td><div id='d_comp_fase'></div></td>
	<tr valign='top'><td><div id='d_comp_loc'></div></td></tr>
	<tr valign='top'><td><div id='d_comp_report'></div></td>
	<tr valign='top'><td><div id='d_comp_eisen'></div></td></tr>
	</table>
	</div>
	<div class="dhtmlgoodies_aTab">
	<table border=0>
	<tr valign='top'>
	<td rowspan=5><div id="d_proto" style="width:675px">Loading....</div></td>
	<tr valign='top'><td><div id='d_prot_info'></div></td>
	<tr valign='top'><td><div id='d_prot_fase'></div></td>
	<tr valign='top'><td><div id='d_prot_loc'></div></td></tr>
	<tr valign='top'><td><div id='d_prot_report'></div></td>
	<tr valign='top'><td colspan=2><div id='d_prot_eisen'></div></td></tr>
	</table>
	</div>
	<div class="dhtmlgoodies_aTab">
    <table border=0>
    <tr valign="top">
	<td rowspan=5><div id="d_report" style="width: 675px">Loading....</div></td>
	<tr valign='top'><td><div id='d_report_info'></div></td></tr>
	<tr valign='top'><td><div id='d_report_fase'></div></td></tr>
	<tr valign='top'><td><div id='d_report_loc'></div></td></tr>
	<tr valign='top'><td><div id='d_report_prot'></div></td></tr>
	</table>
 	<div id='d_report_eisen' style="width: 1596px"></div>
	</div>
	<div class="dhtmlgoodies_aTab">
	<table border=0>
	<tr valign='top'>
	<td valign='top'><div id="d_fase" style="border: 1px; width: 450px;">Loading....</div></td>
	<td valign='top'><div id="d_faseloc" style="width: 550px"></div></td>
	<td valign='top'><div id="d_faselocprot" style="border: 1px; width: 450px;"></div>
    <br>
	<div id="d_faselocproteis"></div>
    <br>
	<div id="d_faselocrep" style="border: 1px; width: 450px;"></div>
    </td>
	</tr>
	</table>
	</div>
	<div class="dhtmlgoodies_aTab">
    <h1>Rapportages</h1>
    <ul>
    <li><a href="/htsql/fase_loc{faseid,locatie,opmerkingen}.sort(fases.milestone,locatie)" target=rapporten>Locaties per fase</a></li>
    <li><a href="/htsql/documents.sort(docstatus.seq,document_id)?rapporttype='TR'" target=rapporten>Testrapporten op status</a></li>
    <li><a href="/htsql/vmx_fasering_alles.sort(eis,di,faseid)" target=rapporten>Verificatiematrix beproevingen tot nu toe</a></li>
    <li><a href="/htsql/vmx_fasering_alles.sort(eis,di,faseid)?verificatie_akkoord='n'" target=rapporten>verificatie niet akkoord</a></li>
    <li><a href="/htsql/eis_di{di,eis,unieke_eisen.eistekst,vmxbp,bpfat,bpifat,bpsat,bpisat,bpsit}.sort(di,eis)?vmxbp" target=rapporten>Beproevingen VMX</a></li>
    <li><a href="/htsql/eis_di{di,eis,unieke_eisen.eistekst,vmxke}.sort(di,eis)?vmxke" target=rapporten>Keuringen VMX</a></li>
    </ul>
	</div>
    </div>
    <div id='dummy'></div>
<input type=hidden id=altvmx_eis>
<input type=hidden id=altvmx_di>
<input type=hidden id=altvmx_fase>
<input type=hidden id=altvmx_tf>
<input type=hidden id=altvmx_val>
	</BODY>
	</HTML>
EOT
    return $html;
}

sub store_eisen_proto {
    my $eis = shift;
    my $di = shift;
    my $protocol = shift;
    my ($id,$naam,$fase) = split(':',$protocol);
    my $html = "";

    my $sql = q!insert into protocol_eis (eis,di,protocol_doc_id) VALUES(?,?,?)!;
    my $dbh = dbi_connect();
    my $sth = $dbh->prepare($sql);
    $sth->execute($eis,$di,$id);

    $html .= "Gereed!";
    $html .= q!<a onclick="self.close();">Sluit venster</a>!;
    return $html;
}

sub Show_Pop1 {
    my $html = "";
    $html .= <<EOT;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<HTML>
    <HEAD><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>VTTI eisen : Kopppel eis aan protocol</title>
    <script type="text/javascript" src="croon.js"></script>
    <link rel="stylesheet" href="croon.css" type="text/css" media="screen">
    </HEAD>
    <BODY>
    <h1>Koppel eis aan protocol</h1>
EOT
    my $dbh = dbi_connect();
    my $eis = $q->param('eis');
    my $di = $q->param('di');
    my $sql1 = q!select distinct eistekst,objname from toewijzing as t join unieke_eisen as e on (e.eis=t.eis) where t.eis=? and t.di=?!;
    my $sth1 = $dbh->prepare($sql1);
    my $sql2 = q!select document_id,document_titel,testfase,"ImandraID" from testprotocol order by document_id!;
    my $sth2 = $dbh->prepare($sql2);
    $sth1->execute($eis,$di);
    my $row=$sth1->fetch();
    
    $html .= <<EOT;
<form>
<table border=1><tr><th class=veldnaam>Veld</th><th colspan=2 class=veldnaam>Waarde</th></tr>
<tr><td>Eis</td><td>$eis</td><td>$$row[0]</td></tr>
<tr><td>DI</td><td>$di</td><td>$$row[1]</td></tr>
<tr><td>Protocol</td><td colspan=2><select size=36 id=veisdiprot name=veisdiprot>;
EOT
    $sth2->execute();
    while ( my $row = $sth2->fetch() ) {
        my $f = $$row[0] . ":" . $$row[1] . ":" . $$row[2];
        $f = (defined($f)&&$f ne ''?$f:"&nbsp;");
        $f =~ s/ /_/g;
        $html .= qq!<option>! . $f . qq!</option>\n!;
    }
	
    # close off the select and return
    $html .= <<EOT;
</select>
<input type=hidden id=vprotoeis_eis>
<input type=hidden id=vprotoeis_di>
<a class="select" onclick="eisen_proto('$eis','$di');">Voeg toe</a>
</form>
<div id=d_vproteis></div>
</td></tr>
</table></BODY>
</HTML>
EOT
    return $html;
}

my $cmd = $q->param('cmd');
my $pjx = CGI::Ajax->new(
        searchcode  => \&exported_fx1,
        searchdi  => \&exported_fx2,
        searchtekst  => \&searchtekst,
        searchfasering  => \&exported_fx4,
        searchlokatie  => \&exported_fx5,
        vmxlokatie => \&vmxlokatie,
        vmxfasering => \&vmxfasering,
        vmxdi => \&vmxdi,
        details => \&details,
        didetails => \&didetails,
        vmxdetails => \&vmxdetails,
        laad_searchdi => \&laad_searchdi,
        laad_searchfasering => \&laad_searchfasering,
        laad_searchlocatie => \&laad_searchlocatie,
        laad_fasering => \&laad_fasering,
        laad_protocollen => \&laad_protocollen,
        laad_rapporten => \&laad_rapporten,
        laad_lokaties => \&laad_lokaties,

        info_fases => \&info_fases,
        info_fases_locs_eisen => \&info_fases_locs_eisen,
        info_fases_locs => \&info_fases_locs,
        info_fases_locs_reps => \&info_fases_locs_reps,
        loadaddfaseloc => \&loadaddfaseloc,
        insfaseloc => \&insfaseloc,
        loadaddfaselocprot => \&loadaddfaselocprot,
        loadaddreportfase => \&loadaddreportfase,
        loadaddreportloc => \&loadaddreportloc,
        loadaddreportprot => \&loadaddreportprot,
        insfaselocprot => \&insfaselocprot,

        info_locfase => \&info_locfase,
        loadaddlocfase => \&loadaddlocfase,
        inslocfase => \&inslocfase,
        loadaddlocprot => \&loadaddlocprot,
        inslocprot => \&inslocprot,
        loadaddlocreport => \&loadaddlocreport,
        inslocreport => \&inslocreport,
        insreportfase => \&insreportfase,
        insreportprot => \&insreportprot,
        insreportloc => \&insreportloc,
        info_locprot => \&info_locprot,
        info_locinfo => \&info_locinfo,
        info_locreport => \&info_locreport,
        addlocatie => \&addlocatie,
        inslocatie => \&inslocatie,

        info_report_info => \&info_report_info,
        info_report_fase => \&info_report_fase,
        info_report_prot => \&info_report_prot,
        info_report_loc => \&info_report_loc,
        info_report_eisen => \&info_report_eisen,
        loadreportstatus => \&loadreportstatus,
        modreportstatus => \&modreportstatus,
        loadaddreporteis => \&dummy,
    
        info_prot_info => \&info_prot_info,
        info_prot_fase => \&dummy,
        info_prot_report => \&dummy,
        info_prot_eisen => \&info_prot_eisen,
        loadprotstatus => \&loadprotstatus,
        modprotstatus => \&modprotstatus,
        info_prot_loc => \&info_prot_loc,
        loadaddprotloc => \&loadaddprotloc,
        loadaddprotlocfase => \&loadaddprotlocfase,
        insprotloc => \&insprotloc,
        
        store_eisen_proto => \&store_eisen_proto,
        do_altvmx => \&do_altvmx
);
$pjx->JSDEBUG(2);
$pjx->DEBUG(0);

# not show the html, which will include the embedded javascript code 
# to handle the ajax interaction 
if (defined($cmd)) {
    print $pjx->build_html($q,\&Show_Pop1); 
} else {
    print $pjx->build_html($q,\&Show_Form); 
# this outputs the html for the page
}
