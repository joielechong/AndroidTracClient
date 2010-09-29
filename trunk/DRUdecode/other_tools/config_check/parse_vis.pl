#! /usr/local/bin/perl -w

use strict;
use XML::Parser;
use DBI;
use Data::Dumper;

sub SQLExec {
    my ($dbh,$string) = @_;

    print $string,"\n";
    $dbh->do($string);
}

my $p = XML::Parser->new(Style=>'Subs', Pkg=>'ParseBev21');
$p->setHandlers('Char'=>\&ParseBev21::CatchText);
my $dbh = DBI->connect("DBI:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=BB21.mdb") or die "cannot open database\n";

my $rv;

my @table_names = $dbh->tables(undef,undef,undef,"TABLE",{ 
  ado_columns => 1, ado_trim_catalog => 0, ado_filter => q{TABLE_NAME LIKE 'vis%'}, }
);
#my @table_names = $dbh->tables;

foreach my $t (@table_names) {
	$rv=SQLExec($dbh,"DELETE FROM ".$t);
}

@table_names = $dbh->tables(undef,undef,undef,"TABLE",{ 
  ado_columns => 1, ado_trim_catalog => 0, ado_filter => q{TABLE_NAME LIKE 'rbc%'}, }
);
#my @table_names = $dbh->tables;

foreach my $t (@table_names) {
	$rv=SQLExec($dbh,"DELETE FROM ".$t);
}


my $filename = shift;
my $ref= $p->parsefile($filename);

my $currInstance;

{
    package ParseBev21;

    my $text;
    my $reftext = \$text;

    sub CatchText {
	my ($expat,$string) = @_;
	$$reftext .= $string;
    }

    sub Instance {
	my ($expat,$element,$attribute,$attval) = @_;

	$currInstance = Instantie->Create($attval);
    }

    sub Instance_ {
	my $name  = $currInstance->name;
	my $class = $currInstance->class;
	my $alias = $currInstance->alias;
	my $attr = $currInstance->attributes;
	my $SQLcmd = "INSERT INTO $class (name,classname,aliasname,".join(",",sort(keys %{$attr})).") VALUES ('$name','$class','$alias'";
	foreach my $a (sort(keys %{$attr})){
	    my $aa=$attr->{$a};
	    if ($aa eq "<NONE>") {
		$aa = "NULL";
	    } else {
		$aa = "'".$aa."'";
	    }
	    $SQLcmd .= ",".$aa;
	}
	$SQLcmd .= ")";
	my $rows=::SQLExec($dbh,$SQLcmd);
	unless (defined($rows)) {
	    print  STDERR "fout bij uitvoeren van $SQLcmd\n   ".$dbh->errstr; exit(4);
	}
	while (my $list=$currInstance->getList) {
	    $list->vulTabel;
	    }
    }

    sub List {
	my ($expat,$element,$attribute,$attval) = @_;
	
	my $list = Instantie->CreateList($attval,$currInstance);
	$currInstance->addList($list);
	$currInstance=$list;
    }

    sub List_ {
	$currInstance = $currInstance->parent;
    }

    sub Row {
	my ($expat,$element,$attribute,$attval) = @_;

	if ($attval != 1) {
	    my $parent = $currInstance->parent;
	    my $class = $currInstance->class;
	    my $newlist = Instantie->CreateList($class,$parent);
	    $parent->addList($newlist);
	    $currInstance=$newlist;
	}
	$currInstance->setRow($attval);
    }

    my $attributeName;
    my $valueString;

    sub Attribute {
	my ($expat,$element,$attribute,$a) = @_;
	$attributeName = $a;
    }

    sub Attribute_ {
	$currInstance->setAttribute($attributeName,$valueString);
    }

    sub Value {
	$valueString = "";
	$reftext = \$valueString;
    }

    sub Value_ {
	$reftext = \$text;
	$text="";
    }

    my $className;

    sub ClassName {
	$className="";
	$reftext = \$className;
    }

    sub ClassName_ {
	$currInstance->setClass($className);
	$reftext = \$text;
	$text="";
    }

    my $aliasName;

    sub AliasName {
	$aliasName="";
	$reftext = \$aliasName;
    }

    sub AliasName_ {
	$currInstance->setAlias($aliasName);
	$reftext = \$text;
	$text="";
    }
}

{
    package Instantie;

    sub Create {
	my ($type,$name) = @_;
	my $self = {};
	$self->{'Name'} = $name;
	$self->{'Attributes'} = {};
	$self->{'Lists'} = [];
	$self->{'Level'} = 0;
	bless $self;
    };

    sub CreateList {
	my ($type,$class,$parent) = @_;
	my $self={};
	$self->{'Class'} =$class;
	$self->{'Parent'} =$parent;
	$self->{'Attributes'} = {};
	$self->{'Lists'} = [];
	$self->{'Level'} = $parent->level+1;
	bless $self;
    };

    sub level {
	my $self = shift;
	
	return $self->{'Level'};
    }
	
    sub name {
	my $self=shift;
	return $self->{'Name'};
    }    

    sub parent {
	my $self = shift;
	return $self->{'Parent'};
    }

    sub setClass {
	my ($self,$class) = @_;
	$self->{'Class'} = $class;
    }

    sub class {
	my $self=shift;
	return $self->{'Class'};
    }

    sub setAlias {
	my ($self,$alias) = @_;
	$self->{'Alias'} = $alias;
    }

    sub alias {
	my $self=shift;
	return $self->{'Alias'};
    }

    sub setRow {
	my ($self,$row) = @_;
	$self->{'Row'} = $row;
    }

    sub row {
	my $self=shift;
	return $self->{'Row'};
    }    

    sub setAttribute {
	my ($self,$attribute,$value) = @_;

	$self->{'Attributes'}->{$attribute} = $value;
    }

    sub attributes {
	my $self = shift;

	return $self->{'Attributes'};
    }

    sub addList {
	my ($self,$list) = @_;

	my $a=$self->{'Lists'};
	push (@$a, $list);
    }

    sub getList {
	my $self = shift;

	my $a=$self->{'Lists'};
	return pop(@$a);
    }

    sub vulTabel {
	my $self = shift;

	my $parent = $self->{'Parent'};
	my $level = $self->{'Level'};
	if ($level > 1) {
#	    fprintf STDERR "Nested list nog niet klaar\n";
	    return;
	}
	my $attr = $self->{'Attributes'};
	my $parentClass=$parent->class;
	my $SQLcmd = "INSERT INTO ".$parentClass."_".$self->{'Class'}." ";
	my @fields = ($parentClass."_Name",'Row_id');
	my @values = ("'".$parent->name."'",$self->{'Row'});
	foreach my $a (sort(keys %{$attr})){
	    push @fields,$a;
	    push @values,"'".$attr->{$a}."'";
	}
	$SQLcmd .= "(".join(',',@fields).") VALUES (".join(',',@values).")";
	my $rows=::SQLExec($dbh,$SQLcmd);	unless (defined($rows)) {
	    print  STDERR "fout bij uitvoeren van $SQLcmd\n   ".$dbh->errstr; exit(4);
	}
	while (my $list1=$self->getList) {
	    $list1->vulTabel;
	}
    }
}
