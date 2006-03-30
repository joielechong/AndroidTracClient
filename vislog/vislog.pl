#! /usr/bin/perl -w

use Convert::ASN1;
use Data::Dumper;
use XML::Simple;

$Data::Dumper::Indent=1;

my $xs = new XML::Simple;
my $asn = Convert::ASN1->new;
my %xml;

$asn->prepare_file("vptbev.txt");

my $asn1=$asn->find("VPT-Bev21-Message");
# $asn1->dump;

my $file = shift;
    
open VL,$file or die "Kan file $file niet openen\n";
my $start=shift;
my $end=shift;
$start=0 unless $start;
$end=999999999 unless $end;
my $lc=0;

while () {
    my $dt="";
    my $type=0;
    my $hent="";
    my $c;
    my $ncs="";
    
    $lc++;
    last unless read(VL,$c,1);
    while ( $c ne ";") {
	$dt = $dt.$c;
	exit unless read(VL,$c,1);
    }
    read(VL,$c,1);
    $type=$c;
    if ($type eq "0") {
	readline(*VL);
    } else {
	read(VL,$c,1);
	read(VL,$c,1);
	while ( $c ne ";") {
	    $hent = $hent.$c;
	    read(VL,$c,1);
	}
	read(VL,$c,1);
	while ( $c ne ";") {
	    $ncs = $ncs.$c;
	    read(VL,$c,1);
	}
	my $nc=read(VL,$buffer,$ncs);
	if ($lc >= $start && $lc <= $end){
	    my $out = $asn1->decode($buffer,{time=>'raw'});
	    if ($out) {
		my %out=%{$out};
		unless (
			exists $out{'vptprl-bev21-live-check-message'} ||
			exists $out{'vptprl-bev21-live-check-response'} ) {   
#		    print "$file $lc $dt $hent ",join(",",keys(%out)),"\n";
#		    print Dumper($out);
		    $xml{sprintf("%08d",$lc)}={'ts' => $dt, 'ip' => $hent, %out};
		}
	    } else {
		print "$file $lc $dt error:",$asn1->error,"\n";
		$xml{sprintf("%08d",$lc)}={'ts' => $dt, 'ip' => $hent, 'asn1_error' =>  {error_message => $asn1->error}};
	    }
	}
	read(VL,$c,1);
    }
}

close VL;

my %all;
$all{"msg"}=\%xml;

my $logfile=$file.".xml";
my $xml=$xs->XMLout(\%all,RootName=>'VisLog',OutPutFile=>"$logfile",KeyAttr=>'nr');
