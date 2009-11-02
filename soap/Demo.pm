{
    package Demo;	
	use lib "/home/mfvl/lib/perl/";
	use lib "/usr/local/lib/perl5/5.10.0/i586-linux-thread-multi-ld";
	use lib "/usr/local/lib/perl5/5.10.0";
	use lib "/usr/local/lib/perl5/site_perl/5.10.0/i586-linux-thread-multi-ld";
	use lib "/usr/local/lib/perl5/site_perl/5.10.0";
	use lib ".";
	
	use DBI;
    
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
	
	sub stocks {
		my $dbh = DBI->connect("dbname=koersdata");
		my $sth1 = $dbh->prepare("SELECT naam FROM koersen_vandaag");
		my $lijst = $sth1->fetchall_arrayref();
		
		return $lijst;
	}
	
    1;	
}				
