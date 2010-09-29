#! /usr/bin/perl -w

use strict;
use DBI;
use DBD::ADO::Const();
use Data::Dumper;
use Data::Diff;

my %dummy_keys = ('kbv_route_spoortakdeel'=> ['vanspoor',
                                              'naarspoor',
                                              'dwangcode',
                                              'row'],
                  'kbv_route'=> ['vanspoor',
                                 'naarspoor',
                                 'dwangcode'],
                  'kbv_melding_parameterorc'=> ['meldingbetekeniscode',
                                                'orc'],
                  'kbv_spoortak' => ['spoortakid_objectcode'],
                  'kbv_verbinding' => ['spoortakidbev21_objectcode',
                                       'steller_objectcode'],
                  'kbv_detector'=> ['additionaldetectorid_naamgebied',
                                    'additionaldetectorid_objecttypebev21',
                                    'additionaldetectorid_objectcode'],
                  );

my $vers1 = "044";
my $vers2 = "054";

my $db1 = "Bev21-$vers1.mdb";
my $db2 = "Bev21-$vers2.mdb";

open TEST ,">diff-$vers1-$vers2.html";

sub print_diff {
    my ($keys,$val,$ptr,$text) = @_;
    my @keys = @$keys;
    
    if ($#keys >= 0) {
        my $text1 = $text."<td>$val</td>";
        shift @keys;
        if (defined($ptr->{diff})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{diff}})) {
                print_diff(\@keys,$_,$ptr->{diff}->{$_},$text1);
            }
        }
        if (defined($ptr->{uniq_a})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{uniq_a}})) {
                print_uniq(\@keys,$_,$ptr->{uniq_a}->{$_},$text1." <td>verwijderd:</td> ");
            }
        }
        if (defined($ptr->{uniq_b})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{uniq_b}})) {
                print_uniq(\@keys,$_,$ptr->{uniq_b}->{$_},$text1." <td>toegevoegd:</td> ");
            }
        }
    } else {
        if ($val ne "row_id") {
            print TEST $text,"<td>$val gewijzigd</td><td>oude waarde = ",$ptr->{diff_a},"</td><td>nieuwe waarde = ",$ptr->{diff_b},"</td></tr>\n";
        }
    }
}

sub print_uniq {
    my ($keys,$val,$ptr,$text) = @_;
    my @keys=@$keys;    
    
    my $text1 = $text."<td>$val</td>";
    shift @keys;
    if ($#keys >= 0) {
        foreach (sort {$a cmp $b} keys(%{$ptr})) {
            print_uniq(\@keys,$_,$ptr->{$_},$text1);
        }
    } else {
        print TEST "$text1</tr>\n";
    }
}

sub print_same {
    my ($keys,$val,$ptr,$text) = @_;
    my @keys = @$keys;
    
    if ($#keys >= 0) {
        my $text1 = $text."<td>$val</td>";
        shift @keys;
        if (defined($ptr->{diff})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{diff}})) {
                print_diff(\@keys,$_,$ptr->{diff}->{$_},$text1);
            }
        }
        if (defined($ptr->{uniq_a})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{uniq_a}})) {
                print_uniq(\@keys,$_,$ptr->{uniq_a}->{$_},$text1." <td>verwijderd:</td> ");
            }
        }
        if (defined($ptr->{uniq_b})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{uniq_b}})) {
                print_uniq(\@keys,$_,$ptr->{uniq_b}->{$_},$text1." <td>toegevoegd:</td> ");
            }
        }
        if (defined($ptr->{same})) {
            foreach (sort {$a cmp $b} keys(%{$ptr->{same}})) {
                print_same(\@keys,$_,$ptr->{same}->{$_},$text1);
            }
        }
    }
}

my $dbh1 = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=$db1");
my @tables1 = $dbh1->tables();
my $dbh2 = DBI->connect("dbi:ADO:Provider=Microsoft.Jet.OLEDB.4.0;Data Source=$db2");
my @tables2 = $dbh2->tables();
my $difft = Data::Diff->new(\@tables1,\@tables2);

print TEST "<html>\n<head><Title>Vergelijking van $db1 en $db2</title></head>\n";
print TEST "<body><h1>Vergelijking van $db1 en $db2</h1>\n";
print TEST "<h2>Tabellen alleen in $db1</h2>\n<ul><li>",join("</li>\n<li>",@{$difft->{out}->{uniq_a}}),"</li></ul>\n" if defined($difft->{out}->{uniq_a});
print TEST "<h2>Tabellen alleen in $db2</h2>\n<ul><li>",join("</li>\n<li>",@{$difft->{out}->{uniq_b}}),"</li></ul>\n" if defined($difft->{out}->{uniq_b});

foreach (sort {lc($a->{same}) cmp lc($b->{same})} @{$difft->{out}->{same}}) {
    my $table = $_->{same};
    $table =~ s/`//g;
    next if $table =~ /^MSys/;

    my @keys=$dbh2->primary_key(undef,undef,$table);
    if ($#keys == -1) {
#        print TEST "Geen primary key voor $table\n";
        if (defined($dummy_keys{$table})) {
            @keys = @{$dummy_keys{$table}};
        }
    }

    my $sql = "SELECT * from $table";
    if ($#keys > -1 ) {
        $sql .= " ORDER BY ".join(",",@keys);
    }
    
#    print TEST "Query = $sql\n";

    my $rows1=$dbh1->selectall_hashref($sql,\@keys);
    my $rows2=$dbh2->selectall_hashref($sql,\@keys);

    my $diff = Data::Diff->new( $rows1, $rows2 );

    print TEST "<h3>Vergelijking tabel $table</h3>\n<table border=1>";
    print TEST "<tr><th>Actie</th><th>",join("</th><th>",@keys),"</th></tr>\n";
    
    if (defined($diff->{out}->{diff})) {
#        print TEST "\nVerschillen\n\n";
        foreach (sort {$a cmp $b} keys(%{$diff->{out}->{diff}})) {
            my $ptr = $diff->{out}->{diff}->{$_};
#            print TEST "Key = $_\n",Dumper($ptr);
            print_diff(\@keys,$_,$ptr,"<tr valign=top><td><b>Gewijzigd</b></td>");
        }
    }
        
    if (defined($diff->{out}->{uniq_a})) {
#        print TEST "\nAlleen in $db1\n\n";
        foreach (sort {$a cmp $b} keys(%{$diff->{out}->{uniq_a}})) {
            my $ptr = $diff->{out}->{uniq_a}->{$_};
#            print TEST "Key = $_\n",Dumper($ptr);
            print_uniq(\@keys,$_,$ptr,"<tr><td><b>Verwijderd</b></td>")
        }
    }
        
    if (defined($diff->{out}->{uniq_b})) {
#        print TEST "\nAlleen in $db2\n\n";
        foreach (sort {$a cmp $b} keys(%{$diff->{out}->{uniq_b}})) {
            my $ptr = $diff->{out}->{uniq_b}->{$_};
#            print TEST "Key = $_\n",Dumper($ptr);
            print_uniq(\@keys,$_,$ptr,"<tr><td><b>Toegevoegd</b></td>")
        }
    }
    
    if (defined($diff->{out}->{same})) {
#        print TEST "\nAlleen in $db2\n\n";
        foreach (sort {$a cmp $b} keys(%{$diff->{out}->{same}})) {
            my $ptr = $diff->{out}->{same}->{$_};
#            print TEST "Key = $_\n",Dumper($ptr);
            print_same(\@keys,$_,$ptr,"<tr><td></td>")
        }
    }
    print TEST "</table>\n";
}

print TEST "</body>\n</html>\n";

close TEST;