{
    package Demo;
    
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
    1;	
}				
