#!/usr/bin/perl -w
use strict;
use IO::Socket;

#get the port to bind to or default to 8000
my $port = shift || 8000;
my $search = shift;

#ignore child processes to prevent zombies
$SIG{CHLD} = 'IGNORE';

#create the listen socket
my $listen_socket = IO::Socket::INET->new(LocalPort => $port,
                                          Listen => 10,
                                          Proto => 'tcp',
                                          Reuse => 1);
#make sure we are bound to the port
die "Cant't create a listening socket: $@" unless $listen_socket;

warn "Server ready. Waiting for connections ... \n";

#wait for connections at the accept call
while (my $connection = $listen_socket->accept)
{   
    my $child;
    # perform the fork or exit
    die "Can't fork: $!" unless defined ($child = fork());
    if ($child == 0)
    {   #i'm the child!
    
        #close the child's listen socket, we dont need it.
        $listen_socket->close;
        
        #call the main child rountine
        play_songs($connection,$search);
        
        #if the child returns, then just exit;
        exit 0;
    } 
    else
    {   #i'm the parent!
    
        #who connected?
        warn "Connecton recieved ... ",$connection->peerhost,"\n";

        #close the connection, the parent has already passed
        #   it off to a child.
        $connection->close();
        
    }
    #go back and listen for the next connection!
} 

sub play_songs
{   
    my $socket = shift;
    my $search = shift;
    
    #get all the possible songs
    if (defined($search)) {
	warn "Searching for $search\n";
	open PLAYLIST, "find /data/Music -name \"*.[mM][pP]3\"| grep -i \"$search\"|" or die;
    } else {
	warn "Playing all\n";
	open PLAYLIST, "find /data/Music -name \"*.[mM][pP]3\"|" or die;
    }
    my @songs = <PLAYLIST>;   
    close PLAYLIST;
    chomp @songs;

    #seed the rand number generator
    srand(time / $$);
    
    #loop forever (or until the client closes the socket)
    while()
    {
        
        #print the HTTP header.  The only thing really necessary
        #   is the first line and the trailing "\n\n"
        #   depending on your client (like xmms) you can also
        #   send song title etc.
        print $socket "HTTP/1.0 200 OK\n";
        print $socket "Content-Type: audio/x-mp3stream\n";
        print $socket "Cache-Control: no-cache \n";
        print $socket "Pragma: no-cache \n";
        print $socket "Connection: close \n";
        
        #get a random song from your playlist
        my $song = $songs[ rand @songs ];
	my $s = `basename "$song"`;
        print $socket "x-audiocast-name: $s\n\n";
        
        #what song are we playing
        warn( "play song: $song\n");
        
        #open the song, or continue to try another one
        open (SONG, $song) || next;

        binmode(SONG); #for windows users

        my $read_status = 1;
        my $print_status = 1;
        my $chunk;

        # This parts print the binary to the socket
        #   as fast as it can.  The buffering will
        #   take place on the client side (it blocks when full)
        #   because this is *not* non-blocking IO
        #
        #the read will return 0 if it has reached eof
        #
        #the print will return undef if it fails
        #   (ie the client stopped listening)
        #
        while( $read_status  && $print_status )
        {   
            $read_status = read (SONG, $chunk, 1024);
            if( defined $chunk && defined $read_status)
            {   
                $print_status = print $socket $chunk;
            }
            undef $chunk;
        }
        close SONG;
        
        unless( defined $print_status )
        {   
            $socket->close();
            exit(0);
        }
    }
}
