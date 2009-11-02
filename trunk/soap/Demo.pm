{
    package Demo;
	
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
