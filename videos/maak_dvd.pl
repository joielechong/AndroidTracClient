#! /usr/bin/perl -w

{
	package XML::Simple::Sorted;
	require Exporter;
	@ISA = qw(XML::Simple);
	use strict;
	use Data::Dumper;


	BEGIN {
		$XML::Simple::Sorted::VERSION = "0.1";
	}
	
	sub inside {
		my ($val,$arr) = @_;
		
		foreach my $i (0 .. $#$arr) {
			return 1 if $val eq $arr->[$i];
		}
		
		return 0;
	}
	
	sub combine {
		my $ref1 = shift;
		my $ref2 = shift;
		my @temp;
		
#		print "In combine\n",Dumper $ref1,$ref2;
		
		foreach my $index (0 .. $#$ref1) {
			push @temp,$ref1->[$index] if inside($ref1->[$index],$ref2);
		}
#		print Dumper(\@temp),"Uit combine\n";

		return @temp;
	}
	
	sub sorted_keys {
		my $self = shift;
		my $naam= shift;
		my $ref = shift;
#		print "Sorted keys: naam: $naam ";
#		print "ref\n",Dumper $ref;
		my @keys = keys(%$ref);
#		print "keys ref: ",join(", ",@keys),"\n";
		return combine(['dest','jumppad','allgprm','vmgm','titleset'],\@keys) if $naam eq 'dvdauthor';
		return combine(['fpc','menus'],\@keys) if $naam eq 'vmgm';
		return combine(['lang','video','audio','subpicture','pgc'],\@keys) if $naam eq 'menus';
		return combine(['menus','titles'],\@keys) if $naam eq 'titleset';
		return combine(['video','audio','subpicture','pgc'],\@keys) if $naam eq 'titles';
		return combine(['entry','palete','pause','pre','vob','button','post'],\@keys) if $naam eq 'pgc';
		return combine(['format','lang','channels','dolby','quant','samplerate'],\@keys) if $naam eq 'audio';
		return combine(['format','aspect','resolution','caption','widescreen'],\@keys) if $naam eq 'video';
#		print "default\n";
		return @keys; #default
	};
	
	1;
}

use strict;

use DBI;
use GD;
use GD::Text::Align;
use Data::Dumper;
use XML::Simple;
use File::Path;
use DateTime;
use DateTime::Format::ISO8601;
use DateTime::Duration;
use IO::Handle;

#STDOUT->autoflush(1); 
my $dvdnr = shift;

die "Aanroep maak_dvd.pl <dvdnr>" unless defined($dvdnr);

my $prefix_in = "/data/JVC-20060819/SD_VIDEO/";
my $prefix_in1 = "/data/pictures/photos/Divers/Konica/";
my $workdir = "/mnt/sdb1/";
my $prefix_pict_in = $workdir;
my $prefix_out= $workdir."dvd".$dvdnr."_files/";
my $destination = $workdir."dvd".$dvdnr;

mkpath($prefix_out);
my $xs = XML::Simple->new();

sub mkpict {
	my $dvdnr = shift;
	my $menutype = shift;
	my $itemref = shift;
	my $title = shift;
	
	my $filename = "$prefix_pict_in/dvd$dvdnr-$menutype";
	
	my $in = GD::Image->new($filename."-in.jpg");
	die "Kan ".$filename."-in.jpg niet openen\n" unless defined($in);
	my $out = GD::Image->new(720,576,1);
	my $mask = GD::Image->new(720,576,0);
	my $high = GD::Image->new(720,576,0);
	my $sel = GD::Image->new(720,576,0);
	$out->copyResampled($in,0,0,0,0,720,576,$in->width,$in->height);
	my $yellow = $out->colorAllocate(255,255,0);
	my $whitem = $mask->colorAllocate(255,255,255);
	my $whiteh = $high->colorAllocate(255,255,255);
	my $whites = $sel->colorAllocate(255,255,255);
	my $yellowm = $mask->colorAllocate(255,255,0);
	my $red = $sel->colorAllocate(255,0,0);
	my $green = $high->colorAllocate(0,255,0);
	$mask->transparent($whitem);
	$high->transparent($whiteh);
	$sel->transparent($whites);

	my $has_fontconfig = $out->useFontConfig(1);

	my $x = 20;
	my $y = 90;
	my @bounds = GD::Image->stringFT($yellow,'/home/mfvl/.spumux/arial.ttf',32,0,$x,$y,$title);
	print join(",",@bounds),"\n";
	$x += 360-($bounds[0]+$bounds[2])/2;
	$out->stringFT($yellow,'/home/mfvl/.spumux/arial.ttf',32,0,$x,$y,$title);
	
#	print $mask->colorsTotal," ";
#	print $high->colorsTotal," ";
#	print $sel->colorsTotal,"\n";
	
	my $sep = 48;
	my $top = $bounds[3] +$sep/4;
	my $bot = 576-$sep/4;
	my $yoffset = (($bot+$top)-$sep*($#$itemref))/2;
	foreach my $i (0 .. $#$itemref) {
		$x = 180;
		$y = $yoffset+$sep*$i;
		print $y," ",$itemref->[$i],"\n";
		$mask->stringFT(-$yellowm,'/home/mfvl/.spumux/arial.ttf',24,0,$x,$y,$itemref->[$i]);
		$sel->stringFT(-$red,'/home/mfvl/.spumux/arial.ttf',24,0,$x,$y,$itemref->[$i]);
		$high->stringFT(-$green,'/home/mfvl/.spumux/arial.ttf',24,0,$x,$y,$itemref->[$i]);
	}
	open X,">$filename-out.jpg";
	binmode X;
	print X $out->jpeg(90);
	close X;
	open X,">$filename-mask.png";
	binmode X;
	print X $mask->png();
	close X;
	open X,">$filename-high.png";
	binmode X;
	print X $high->png();
	close X;
	open X,">$filename-sel.png";
	binmode X;
	print X $sel->png();
	close X;
	
#	print $mask->colorsTotal," ";
#	print $high->colorsTotal," ";
#	print $sel->colorsTotal,"\n";
	
	my $subpicture;
	$subpicture->{stream}->[0]->{spu}->[0]->{start}="00:00:00";
	$subpicture->{stream}->[0]->{spu}->[0]->{image}="$filename-mask.png";
	$subpicture->{stream}->[0]->{spu}->[0]->{highlight}="$filename-high.png";
	$subpicture->{stream}->[0]->{spu}->[0]->{select}="$filename-sel.png";
	$subpicture->{stream}->[0]->{spu}->[0]->{transparent}="ffffff";
	$subpicture->{stream}->[0]->{spu}->[0]->{autooutline}="infer";
	$subpicture->{stream}->[0]->{spu}->[0]->{outlinewidth}="17";
	$subpicture->{stream}->[0]->{spu}->[0]->{autoorder}="columns";
	$subpicture->{stream}->[0]->{spu}->[0]->{force}="yes";
	my $xml = $xs->XMLout($subpicture,RootName=>'subpictures');
	open XML,">$filename.xml";
	print XML $xml;
	close XML;
}

sub mk_info_sub {
	my $naam = shift;
	my $directory = shift;
	my $tijdstip = shift;
	my $duur = shift;
	my $onderwerp = shift;
	my $subpictures;
	my $sub1 = "$prefix_out/$naam.srt";
	
	$subpictures->{stream}->{textsub}->[0]->{filename}=$sub1;
	$subpictures->{stream}->{textsub}->[0]->{characterset}="ISO8859-1";
	$subpictures->{stream}->{textsub}->[0]->{fontsize}="20.0";
	$subpictures->{stream}->{textsub}->[0]->{font}="arial.ttf";
	$subpictures->{stream}->{textsub}->[0]->{"horizontal-alignment"}="left";
	$subpictures->{stream}->{textsub}->[0]->{"vertical-alignment"}="bottom";
	$subpictures->{stream}->{textsub}->[0]->{"left-margin"}="60";
	$subpictures->{stream}->{textsub}->[0]->{"right-margin"}="60";
	$subpictures->{stream}->{textsub}->[0]->{"top-margin"}="20";
	$subpictures->{stream}->{textsub}->[0]->{"bottom-margin"}="30";
	$subpictures->{stream}->{textsub}->[0]->{"subtitle-fps"}="25";
	$subpictures->{stream}->{textsub}->[0]->{"movie-fps"}="25";
	$subpictures->{stream}->{textsub}->[0]->{"movie-width"}="720";
	$subpictures->{stream}->{textsub}->[0]->{"movie-height"}="570";
	$subpictures->{stream}->{textsub}->[0]->{force}="yes";	

	my $xml = $xs->XMLout($subpictures,RootName=>'subpictures');
	open XML,">$prefix_out/$naam.id.xml";
	print XML $xml;
	close XML;
	
	open SRT,">$sub1";
	print SRT "1\n00:00:00,00 --> 00:00:06,00\n$directory $naam $tijdstip $duur\n$onderwerp\n";
	close SRT;
}

sub mk_tc_sub {
	my $naam = shift;
	my $tijdstip = shift;
	my $duur = shift;
	my $subpictures;
	my $sub2 = "$prefix_out/$naam.tc.srt";
	
	$subpictures->{stream}->{textsub}->[0]->{filename}=$sub2;
	$subpictures->{stream}->{textsub}->[0]->{fontsize}="20.0";
	$subpictures->{stream}->{textsub}->[0]->{"horizontal-alignment"}="right";
	$subpictures->{stream}->{textsub}->[0]->{"vertical-alignment"}="top";
	$subpictures->{stream}->{textsub}->[0]->{force}="no";	
	
	my $xml = $xs->XMLout($subpictures,RootName=>'subpictures');
	open XML,">$prefix_out/$naam.tc.xml";
	print XML $xml;
	close XML;
	
	my $ts=$tijdstip;
	$ts =~ s/\ /T/;
	my $duration = DateTime::Duration->new(seconds=>1);
	my $dt = DateTime::Format::ISO8601->parse_datetime($ts);
	my $tel=DateTime->new(year=>2009,month=>1,day=>1,hour=>0,minute=>0,second=>0);
	open SRT,">$sub2";
	for (my $t=0;$t <= $duur;$t++) {
		print SRT $t+1,"\n";
		print SRT $tel->strftime("%T").",00 --> ";
		$tel += $duration;
		print SRT $tel->strftime("%T").",00\n";
		print SRT $dt->strftime("%F %T"),"\n\n";
		$dt += $duration;
	}
	close SRT;	
}

open BATCH,">$prefix_out/maak_mpg.sh";

print BATCH "#! /bin/sh\n";
print BATCH "function call_ffmpeg()\n";
print BATCH "{\n";
print BATCH " 	ffmpeg -v 0 -i $prefix_in/\$2/\$1.MOD  -target pal-dvd -aspect 16:9 -flags ilme -vb 9000k -ab 384k  - | ";
print BATCH "spumux -s0 $prefix_out/\$1.id.xml | ";
print BATCH "spumux -s1 $prefix_out/\$1.tc.xml > $prefix_out/\$1.mpg\n";
print BATCH "}\n\n";

print BATCH "function call_ffmpeg1()\n";
print BATCH "{\n";
print BATCH "	ffmpeg -v 0 -i $prefix_in1/\$2/\$1  -target pal-dvd -aspect 4:3 -vb 9000k -ab 384k -ac 2 - | ";
print BATCH "spumux -s0 $prefix_out/\$1.id.xml | ";
print BATCH "spumux -s1 $prefix_out/\$1.tc.xml > $prefix_out/\$1.mpg\n";
print BATCH "}\n\n";

print BATCH "function call_ffmpeg2()\n";
print BATCH "{\n";
print BATCH "	ffmpeg -v 0 -i \$2/\$1  -target pal-dvd -aspect 4:3 -vb 9000k -ab 384k -ac 2 - | ";
print BATCH "spumux -s0 $prefix_out/\$2.id.xml | ";
print BATCH "spumux -s1 $prefix_out/\$2.tc.xml > $prefix_out/\$1.mpg\n";
print BATCH "}\n\n";

print BATCH "function call_mkmenu()\n";
print BATCH "{\n";
print BATCH "  echo $prefix_pict_in/\$1-out.jpg | ";
print BATCH "jpeg2yuv -f 25 -n 25 -I p -A 4:3 | ";
print BATCH "ffmpeg -i - -i $prefix_pict_in/silence.ac3 -target pal-dvd -aspect 4:3 -t 1 -ab 384k - | ";
print BATCH "spumux $prefix_pict_in/\$1.xml >$prefix_out/\$1.mpg\n";
print BATCH "}\n\n";


my $dbh = DBI->connect("dbi:Pg:dbname=mfvl");

my $sth2 = $dbh->prepare("SELECT onderwerp,min(date(tijdstip)) FROM videos WHERE dvdnr=? and duur > 3 GROUP BY onderwerp ORDER BY min(tijdstip),1");
my $sth3 = $dbh->prepare("SELECT distinct date(tijdstip) FROM videos where onderwerp=? and dvdnr=? and duur > 3 order by 1;");
my $sth4 = $dbh->prepare("SELECT directory,naam,tijdstip,duur,aspect FROM videos WHERE date(tijdstip)=? and onderwerp=? and dvdnr=? and duur > 3 ORDER by tijdstip");
my $sth5 = $dbh->prepare("SELECT min(date(tijdstip)),max(date(tijdstip)) FROM videos WHERE dvdnr=? and duur > 3");

my $dvdauth;
$dvdauth->{dest}=$destination;
#
# DISK TITLE MENU
#
$dvdauth->{vmgm}->[0]->{menus}->[0]->{lang}="nl";
#$dvdauth->{vmgm}->[0]->{fpc}->[0]="g5=0;g6=0;g7=0;g8=0;jump vmgm menu entry title;";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{video}->{format}="pal";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{video}->{aspect}="4:3";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{audio}->{format}="ac3";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{audio}->{lang}="nl";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{subpicture}->{lang}="nl";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[0]->{entry}="title";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[0]->{pause}="inf";
$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[0]->{vob}->[0]->{file}=$prefix_out."dvd".$dvdnr."-titlemenu.mpg";

my @titlemenu;
my @daterange;
$sth5->execute($dvdnr);
die "Problemen om $dvdnr op te halen\n" unless  (@daterange=$sth5->fetchrow_array());
print join(",",@daterange),"\n";

my $titlesetcount=0;
my $globtitlecount=0;
$sth2->execute($dvdnr);
while (my ($onderwerp,$firstdate) = $sth2->fetchrow_array()) {
#
# TITLE ROOT MENU
#
	push @titlemenu,$onderwerp;
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{lang}="nl";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{video}->{format}="pal";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{video}->{aspect}="4:3";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{audio}->{format}="ac3";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{audio}->{lang}="nl";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{subpicture}->[0]->{lang}="nl";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{entry}="root";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{pause}="inf";
#	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{pre}->[0]="if (g7 eq 1) {g6=1;jump title 1;}";
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{vob}->[0]->{file}=$prefix_out."dvd".$dvdnr."-menu-".($titlesetcount+1).".mpg";
#
# Add entry to Disk menu
#	
	$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[0]->{button}->[$titlesetcount]="g5=0;g6=0;jump titleset ".($titlesetcount+1)." menu;";
	$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[$titlesetcount]->{pre}->[0]="g5=1;g6=1;jump title ".($globtitlecount+1).";" if $globtitlecount > 0;
#
# Titleset header
#
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{video}->{format}="pal";
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{video}->{aspect}="4:3";
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{audio}->{format}="ac3";
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{audio}->{lang}="nl";
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{subpicture}->[0]->{lang}="nl";
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{subpicture}->[1]->{lang}="nl";

	$sth3->execute($onderwerp,$dvdnr);
	my $titlecount=0;
	my @rootmenu;
	while (my $datum = $sth3->fetchrow_array()) {
		push @rootmenu, $datum;
		$sth4->execute($datum,$onderwerp,$dvdnr);
		my $vobcnt=0;
		
		while (my ($directory,$naam,$tijdstip,$duur,$aspect) = $sth4->fetchrow_array()) {
#
# vob file definition
# set aspectratio to the 1st video
#			
			$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{video}->{aspect}=$aspect if $vobcnt == 0;
			$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{pgc}->[$titlecount]->{vob}->[$vobcnt++]->{file}="$prefix_out$naam".".mpg";
#
# add entry to root menu
#
			$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{button}->[$titlecount]="g5=0;g6=0;jump title ".($titlecount+1).";";
			mk_info_sub($naam,$directory,$tijdstip,$duur,$onderwerp);
			mk_tc_sub($naam,$tijdstip,$duur);
			if ($directory =~ /^PRG/) {
				print BATCH "call_ffmpeg $naam $directory\n";
			} else {
				print BATCH "call_ffmpeg1 $naam $directory\n";
			}
		}
#
# add jump to next title
#		
		$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{pgc}->[$titlecount-1]->{post}->[0]="if (g6 eq 1) jump title ".($titlecount+1)."; else call menu;" if $titlecount > 0;
		$titlecount++;
		$globtitlecount++;
	}
#
# add jump back to root menu
#
	$dvdauth->{titleset}->[$titlesetcount]->{titles}->[0]->{pgc}->[$titlecount-1]->{post}->[0]="if (g5 eq 1) call vmgm menu ".($titlesetcount+2)."; else call menu;";
#
# button to play all
#
	if ($#rootmenu > 0) {
		$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{button}->[$titlecount++]="g5=0;g6=1;jump title 1;";
		push @rootmenu,"Speel alles";
	}
	push @rootmenu,"Hoofdmenu";
	print "Onderwerpen op root menu $titlesetcount ($onderwerp)\n",join(", ",@rootmenu),"\n";
	mkpict($dvdnr,"menu-".($titlesetcount+1),\@rootmenu,$onderwerp);
	print BATCH "call_mkmenu dvd$dvdnr-menu-".($titlesetcount+1)."\n";
#
# button to title menu
#
	$dvdauth->{titleset}->[$titlesetcount]->{menus}->[0]->{pgc}->[0]->{button}->[$titlecount]="g5=0;g6=0;jump vmgm menu;";
	$titlesetcount++;
}

if ($#titlemenu > 0) {
	$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[0]->{button}->[$titlesetcount]="g5=1;g6=1;jump title 1;";
	push @titlemenu,"Speel alles";
}
$dvdauth->{vmgm}->[0]->{menus}->[0]->{pgc}->[$titlesetcount]->{pre}->[0]="g5=0;g6=0;jump menu entry title;";

print "Onderwerpen op title menu\n",join(", ",@titlemenu),"\n";
mkpict($dvdnr,"titlemenu",\@titlemenu,$dvdnr.". ".$daterange[0]." -- ".$daterange[1]);
print BATCH "call_mkmenu dvd$dvdnr-titlemenu\n";

close BATCH;

#print Dumper $dvdauth;
my $xss = XML::Simple::Sorted->new();
open XML,">dvd$dvdnr.xml";
print XML $xss->XMLout($dvdauth,RootName=>'dvdauthor');
close XML;
