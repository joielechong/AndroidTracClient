#! /usr/bin/perl -w

use strict;
use Data::Dumper;
use XML::LibXML;
use XML::LibXML::SAX::Builder;
use XML::Generator::DBI;
use XML::LibXSLT;
use DBI;

my $dbname = shift;
my $query = shift;
my $stylesheet = shift;

my $dbh = DBI->connect("dbi:Pg:dbname=$dbname");
my $builder = XML::LibXML::SAX::Builder->new();
my $gen = XML::Generator::DBI->new(Handler=>$builder, dbh=> $dbh);
$gen->execute($query);
my $doc = $builder->result();

if (defined($stylesheet)) {
    my $xslt = XML::LibXSLT->new();
    my $parser = XML::LibXML->new();
    my $style_doc = $parser->parse_file($stylesheet);
    my $stylesht = $xslt->parse_stylesheet($style_doc);
    
    my $results = $stylesht->transform($doc);
    print $stylesht->output_string($results);
} else {
    print $doc->serialize();
}
