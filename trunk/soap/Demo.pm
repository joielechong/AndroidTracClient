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
	
	return "Ik ontving: $arg\n";
    }	
    1;	
}				
