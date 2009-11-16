{
    package Demo;
    BEGIN { 
	@INC = split (',',"usr/local/lib/perl5/5.10.0/i586-linux-thread-multi-ld,/usr/local/lib/perl5/5.10.0,/usr/local/lib/perl5/site_perl/5.10.0/i586-linux-thread-multi-ld,/usr/local/lib/perl5/site_perl/5.10.0,.");
    }
    
    use DBI;
    
#    sub new { bless {},shift;}
    
    sub hi {                     
	return "hello, world";     
    } 
    
    sub bye {                    
	return "goodbye, cruel world";
    } 
    
    sub echo {
	my $arg = join(", ",@_);
	my %antwoord;
	
	$antwoord{klas} = shift;
	$antwoord{arg} = shift;
	return \%antwoord;
    }	
    
    sub stock {
	my $class = shift;
	my $stock = shift;
	
	my $dbh = DBI->connect("dbi:Pg:dbname=koersdata");
	my $sth1 = $dbh->prepare("SELECT * FROM koersen_vandaag where naam=?");
	$sth1->execute($stock);
	my $lijst = $sth1->fetchrow_hashref();
	
	return $lijst;
    }
    
    sub stocks {
	my $dbh = DBI->connect("dbi:Pg:dbname=koersdata");
	my $sth1 = $dbh->prepare("SELECT naam FROM koersen_vandaag");
	$sth1->execute();
	my @lijst=();
	while (my @row=$sth1->fetchrow_array()) {
	    push @lijst,$row[0];
	}
#		my $lijst = $sth1->fetchall_arrayref();
	
	return \@lijst;
    }
    
    1;	
}				
