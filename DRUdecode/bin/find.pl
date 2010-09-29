#! D:\Perl\bin\perl.exe -w
    eval 'exec D:\Perl\bin\perl.exe -S $0 ${1+"$@"}'
        if 0; #$running_under_some_shell
use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;


use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;
use File::Basename;
use File::Find ();

# Set the variable $File::Find::dont_use_nlink if you're using AFS,
# since AFS cheats.

# for the convenience of &wanted calls, including -eval statements:
use vars qw/*name *dir *prune/;
*name   = *File::Find::name;
*dir    = *File::Find::dir;
*prune  = *File::Find::prune;

sub wanted;

my $fhcsv;
my $find_nid_message_wanted;

sub find_all {
    $find_nid_message_wanted=shift;
    # Traverse desired filesystems
    File::Find::find({wanted => \&wanted}, 'I:\\B&B BR\\Werkruimte\\Team IV\\Testomgevingen');
}
    
sub wanted {
    /^.*\.jdta\z/s
    && process_file($name,$find_nid_message_wanted);
}

sub process_file{
    my $name = shift;
    my $nid_message_wanted =shift;
    print("$name\n");
    
    if ($name =~ m:.*[/\\]JRU[^/\\]*jdta$:) {
        print "Dit is een JRU file\n";
        process_jru($name,$nid_message_wanted);
    } elsif ($name =~ m:.*[/\\]DRU[^/\\]*jdta$:) {
        process_dru($name,$nid_message_wanted);
    } else {
        print "Dit is iets anders\n";
    }
}

sub resync {
    my ($buffer,$pos,$size) = @_;
    
    my @magic = ("0106","0A0C","0A0A","0909","1108","0B07","020D","0006","1407","0307","0F07");
    my $newpos = $size;
        
    foreach my $gms (@magic) {
        my $gm = pack("H4",$gms);
        my $nextgm = index($$buffer,$gm,$pos);
        print "Eerst volgende $gms = $nextgm\n";
        if ($nextgm > 0) {
            $newpos = $nextgm if $nextgm < $newpos;
        }
    }
    return $newpos;
}
sub print_header {
    my $fhcsv = shift;
    my $custom = shift;
    
    print $fhcsv "Filename";
    print $fhcsv ",J,Mnd,D,U,Min,S,TTS,LEVEL,MODE";
    print $fhcsv ",$custom" if defined($custom);
    print $fhcsv ",J,Mnd,D,U,Min,S,TTS,LEVEL,MODE";
    print $fhcsv ",NID_LRBG,D_LRBG,Q_DIRLRBG,Q_DLRBG,DOUBTOVER,DOUBTUNDER,V\n";
}

sub print_first_part {
    my $fhcsv = shift;
    my $filename = shift;
    my $message = shift;
    my $custom = shift;

    my $file = basename($filename,('.jdta'));
    print $fhcsv '"',$file,'",';
    print $fhcsv $message->{Header}->{Fields}->[2]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[3]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[4]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[5]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[6]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[7]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[8]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[19]->{Text},",";
    print $fhcsv $message->{Header}->{Fields}->[20]->{Text},",";
    print $fhcsv $custom,"," if defined($custom);
}

sub print_second_part {
    my $fhcsv = shift;
    my $message = shift;

    print $fhcsv $message->{Header}->{Fields}->[2]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[3]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[4]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[5]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[6]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[7]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[8]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[19]->{Text},",";
    print $fhcsv $message->{Header}->{Fields}->[20]->{Text},",";
    print $fhcsv '"',$message->{Header}->{Fields}->[10]->{Text},'",';
    print $fhcsv $message->{Header}->{Fields}->[11]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[12]->{Text},",";
    print $fhcsv $message->{Header}->{Fields}->[13]->{Text},",";
    print $fhcsv $message->{Header}->{Fields}->[14]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[15]->{Decimal},",";
    print $fhcsv $message->{Header}->{Fields}->[16]->{Decimal},",";
    print $fhcsv "\n";
}

sub process_dru {
    my $filename = shift;
    my $nid_message_wanted=shift;
    my $header = 0;
   
    open my $JRUfh,$filename or die "Kan DRU file $filename niet openen\n";
    binmode($JRUfh);
    my $DRU = ETCS::DRU::new($JRUfh);
    print "Aantal records: ",$DRU->{reccount}."\n";
    $DRU->setFilterMessage($nid_message_wanted);
    
    while (my $message=$DRU->next()) {
        if ($message->{Header}->{Fields}->[0]->{Decimal} == $nid_message_wanted) {
            if ($header == 1) {
                print $fhcsv "\n";
            }
            my $custom;
            $custom = $message->{PredefinedTextMessage}->{Fields}->[2]->{Text} if $nid_message_wanted == 15;
            $custom = $message->{PlainTextMessage}->{Fields}->[3]->{Text} if $nid_message_wanted == 16;
            print_first_part($fhcsv,$filename,$message,$custom);
            $header = 1;
        }
        if (defined($message->{Header}->{Fields}->[10]->{Decimal})) {
            print_second_part($fhcsv,$message);
            $header = 0;
            $DRU->stopProcessing();
        }
    }
    close $JRUfh;
}

sub process_jru {
    my $filename = shift;
    my $nid_message_wanted=shift;
    
    my $buffer;

    my ($dev,$ino,$mode,$nlink,$uid,$gid,$rdev,$size,
           $atime,$mtime,$ctime,$blksize,$blocks)
               = stat($filename);
               
    open FILE,"<$filename" or die "kan JRU file $filename niet openen\n";
    binmode FILE;
    
    read(FILE,$buffer,$size);
    my $pos=0;
    my $reccnt = 0;
    
    ETCS::Debug::debugOff();
    
    my $oldlength = 0;
    my $oldmessage = undef;
    my $searching = 0;
    my $header = 0;
    
    while ($pos < $size) {
        $reccnt++;
        my $nid_message = unpack("C",substr($buffer,$pos,1));
        my $length = unpack("n",substr($buffer,$pos+1,2))>>6;
        if (($length < 27) || ($nid_message > 25) || (($nid_message==1) && ($length != 27))) {
            
            print "\nIllegale frame. NID_MESSAGE = $nid_message. Lengte = $length\n";
            print "Huidige record = $reccnt\n";
            print "Positie in file = $pos\n";
            print "Vorige frame:\n",Dumper($oldmessage)if defined($oldmessage);
            
            my $newpos = resync(\$buffer,$pos-$oldlength+1,$size);
            
            print "Hersynchronisatie naar $newpos, oude pos = $pos, reccnt = $reccnt\n";
            $pos = $newpos;
            $oldlength = 0;
        } else {
            if (($nid_message_wanted == $nid_message) || $searching) {
                $searching = 1;  # voor het gemak ook al is het al 1
                my $message = ETCS::JRU::new();
                my $msgstat = $message->setMessage(unpack("B*",substr($buffer,$pos,$length)));
                if ($msgstat >= 0) {
#                   print DB Dumper($message);
                    if ($nid_message_wanted == $nid_message) {
                        if ($header == 1) {
                            print $fhcsv "\n";
                        }
                        my $custom;
                        $custom = $message->{PredefinedTextMessage}->{Fields}->[2]->{Text} if $nid_message_wanted == 15;
                        $custom = $message->{PlainTextMessage}->{Fields}->[3]->{Text} if $nid_message_wanted == 16;
                        print_first_part($fhcsv,$filename,$message,$custom);
                        $header = 1;
                    }                        
                    if (defined($message->{Header}->{Fields}->[10]->{Decimal})) {
                        print_second_part($fhcsv,$message);
                        $searching = 0;  #eerste veld met gedefinieerde positie
                        $header = 0;
                    }
                    $oldlength=$length;
                    $oldmessage = $message;
                    $pos += $length;
                } else {
                    $oldlength=$length;
                    $oldmessage = $message;
                    $pos += $length;
                }
            } else {
                $oldlength=$length;
                $oldmessage = undef;
                $pos += $length;
            }
        }
    }
#    print $fhxml "</JRULog>\n";
#    close $fhxml;
    close FILE;
#   close DB;
}

my $nid_message_wanted=shift;
die "Aanroep: $0 <message nummer> [filenaam]\n" unless defined($nid_message_wanted);
die "Message nummer moet integer zijn\n" unless $nid_message_wanted =~ m/^\d+$/;

my $filename = shift;

if (defined($filename)) {
    my $file = basename($filename,('.jdta'));
    open $fhcsv,">$file.$nid_message_wanted.csv" or die "Kan CSV file niet openen\n";
    if (($nid_message_wanted == 15) || ($nid_message_wanted == 16)) {
        print_header($fhcsv,"Text Message");
    } else {
        print_header($fhcsv);
    }
    process_file($filename,$nid_message_wanted);
} else {
    open $fhcsv,">allfiles.$nid_message_wanted.csv" or die "Kan CSV file niet openen\n";
    if (($nid_message_wanted == 15) || ($nid_message_wanted == 16)) {
        print_header($fhcsv,"Text Message");
    } else {
        print_header($fhcsv);
    }
    find_all($nid_message_wanted);
}
close $fhcsv;