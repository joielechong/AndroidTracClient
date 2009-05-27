#! /usr/bin/perl -w

use strict;
use GD;
use GD::Text::Align;
use Data::Dumper;

my $mask = GD::Image->new(720,576);
my $white = $mask->colorAllocate(255,255,255);
my $yellow = $mask->colorAllocate(255,255,0);
my $has_fontconfig = $mask->useFontConfig(1);
print $has_fontconfig,"\n";

$mask->transparent($white);

print Dumper($mask);

my $gd_text = GD::Text::Align->new($mask);
#$gd_text->font_path('/home/mfvl/.spumux/');
$gd_text->font_path('/mnt/hdb1/OudeC/WINDOWS/FONTS/');
$gd_text->set_text('Probeersel');
$gd_text->set_font('arial.ttf',36);
print GD::Text::error(),"\n";
print Dumper($gd_text);

$gd_text->draw(120,240,0);

open IMG,">/tmp/test.png";
binmode IMG;
print IMG $mask->png;
close IMG;
