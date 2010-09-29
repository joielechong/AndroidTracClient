#! /usr/bin/perl -w

use lib "I:/B&B BR/Werkruimte/Team IV/Testtools/Perl/lib"; # gebruik productie versie
use lib "./lib";   # gebruik lokale versie in subdirectory lib

use strict;
use Data::Dumper;
use ETCS;
use XML::Simple;
use IO::Handle;

my @csv_fields = ( 
	# main fields
	"NID_MESSAGE",
	"DATE",
	"TIME",
	"Q_SCALE",
	"NID_LRBG",
	"D_LRBG",
	"Q_DIRLRBG",
	"Q_DLRBG",
	"L_DOUBTOVER",
	"L_DOUBTUNDER",
	"V_TRAIN",
	"NID_OPERATIONAL",
	"M_LEVEL",
	"M_MODE",

	"BALISE_Q_UPDOWN",	# (6) telegram from balise
	"BALISE_M_VERSION",	# (6) telegram from balise
	"BALISE_Q_MEDIA",	# (6) telegram from balise
	"BALISE_N_PIG",		# (6) telegram from balise
	"BALISE_N_TOTAL",	# (6) telegram from balise
	"BALISE_M_DUP",		# (6) telegram from balise
	"BALISE_M_MCOUNT",	# (6) telegram from balise
	"BALISE_NID_C",		# (6) telegram from balise
	"BALISE_NID_BG",	# (6) telegram from balise
	"BALISE_Q_LINK",	# (6) telegram from balise
	
	"RBC_NID_MESSAGE",	# (9) message from RBC / (10) message to RBC
	"RBC_L_MESSAGE",	# (9) message from RBC / (10) message to RBC
	"RBC_T_TRAIN",		# (9) message from RBC / (10) message to RBC
	"RBC_M_ACK",		# (9) message from RBC
	"RBC_NID_LRBG",		# (9) message from RBC
	"RBC_NID_ENGINE",	# (10) message to RBC
	
	# other	
	"M_EVENTS",
	"M_DRIVERACTIONS",
	"L_TEXT",
	"V_MRSP",
	"V_LOA",
	"D_LOA",
	"V_RELEASE",
	"V_PERMITTED"
);
my %is_in_csv_fields = ();
for (@csv_fields) { $is_in_csv_fields{$_} = 1 }

#
# Main program
#

# open files
#open my $JRUfh,"D:\\TBI-PERL\\RRF18_TRU\\DRU 2007-03-23 10h 55m 45s.jdta" or die "Kan JRU file niet openen\n";
#open my $JRUfh,"DRU 2007-04-04 23h 07m 49s.jdta" or die "Kan JRU file niet openen\n";
open my $JRUfh,"DRU 2007-10-20 06h 10m 08s.jdta" or die "Kan JRU file niet openen\n";
open CSVFILE,">TBI_0003_rit1.csv";
open DBGFILE,">TEST.TXT";
open my $fhxml,">testdru.xml";
print $fhxml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DRULog>\n";

my $maxdate = "20071016";
my $mindate = "20071016";
my $mintime = "220820000";
my $maxtime = "221300000";

# initialize files
binmode($JRUfh);

ETCS::Debug::debugOff();

my $DRU = ETCS::DRU::new($JRUfh);
print "Aantal records: ",$DRU->{reccount}."\n";

#$DRU->seek(17200); # begin pas vanaf record 17200 te verwerken. werkt alleen bij ETCS::DRU

$DRU->setFilter($mindate,$mintime,$maxdate,$maxtime);

# write header data
print CSVFILE join(";",@csv_fields),"\n";

# read records

MAINLOOP: while (my $message=$DRU->next()) {
	my %csv_values = ();
	# parse JRU record

#	# filter op datum
#	my $msghdr = $message->{Header};

#	my $msgdate = 20000000 +
#		($msghdr->{Fields}->[2]->{Decimal} * 10000) +
#		($msghdr->{Fields}->[3]->{Decimal} * 100) +
#		$msghdr->{Fields}->[4]->{Decimal};
#	my $msgtime = 
#		($msghdr->{Fields}->[5]->{Decimal} * 10000000) +
#		($msghdr->{Fields}->[6]->{Decimal} * 100000) +
#		($msghdr->{Fields}->[7]->{Decimal} * 1000) +
#		($msghdr->{Fields}->[8]->{Decimal} * 50);
#	next MAINLOOP if ($msgdate < $mindate);
#	next MAINLOOP if ($msgdate > $maxdate);
#	next MAINLOOP if ($msgtime < $mintime);
#	next MAINLOOP if ($msgtime > $maxtime);
        
        print DBGFILE Dumper($message);
	XMLout($message, NoAttr=>0,OutputFile=>$fhxml,RootName=>$message->{MessageType});        

	if ($message->{MessageType} eq "JRUMessage") {
	        # copy message to csv_values
		$csv_values{"NID_MESSAGE"} = $message->{Header}->{Fields}->[0]->{Decimal}
			if $is_in_csv_fields{'NID_MESSAGE'};
				
		$csv_values{"L_MESSAGE"} = $message->{Header}->{Fields}->[1]->{Decimal}
			if $is_in_csv_fields{'L_MESSAGE'};
			
		$csv_values{"DATE"} = sprintf("%d-%02d-%02d",
			2000 + $message->{Header}->{Fields}->[2]->{Decimal},
			$message->{Header}->{Fields}->[3]->{Decimal},
			$message->{Header}->{Fields}->[4]->{Decimal})
				if $is_in_csv_fields{'DATE'};
					
		$csv_values{"TIME"} = sprintf("%02d:%02d:%02d.%03d",
			$message->{Header}->{Fields}->[5]->{Decimal},
			$message->{Header}->{Fields}->[6]->{Decimal},
			$message->{Header}->{Fields}->[7]->{Decimal},
			$message->{Header}->{Fields}->[8]->{Decimal})
				if $is_in_csv_fields{'TIME'};

		$csv_values{"Q_SCALE"} = $message->{Header}->{Fields}->[9]->{Decimal}
			if $is_in_csv_fields{'Q_SCALE'};
				
		$csv_values{"NID_LRBG"} = $message->{Header}->{Fields}->[10]->{Text}
			if $is_in_csv_fields{'NID_LRBG'};
				
		$csv_values{"D_LRBG"} = $message->{Header}->{Fields}->[11]->{Decimal}
			if $is_in_csv_fields{'D_LRBG'};
				
		$csv_values{"Q_DIRLRBG"} = $message->{Header}->{Fields}->[12]->{Decimal}
			if $is_in_csv_fields{'Q_DIRLRBG'};
				
		$csv_values{"Q_DLRBG"} = $message->{Header}->{Fields}->[13]->{Decimal}
			if $is_in_csv_fields{'Q_DLRBG'};
				
		$csv_values{"L_DOUBTOVER"} = $message->{Header}->{Fields}->[14]->{Decimal}
			if $is_in_csv_fields{'L_DOUBTOVER'};
				
		$csv_values{"L_DOUBTUNDER"} = $message->{Header}->{Fields}->[15]->{Decimal}
			if $is_in_csv_fields{'L_DOUBTUNDER'};
				
		$csv_values{"V_TRAIN"} = $message->{Header}->{Fields}->[16]->{Decimal}
			if $is_in_csv_fields{'V_TRAIN'};
				
		$csv_values{"DRIVER_ID"} = $message->{Header}->{Fields}->[17]->{Hex}
			if $is_in_csv_fields{'DRIVER_ID'};
				
		$csv_values{"NID_OPERATIONAL"} = $message->{Header}->{Fields}->[18]->{Hex}
			if $is_in_csv_fields{'NID_OPERATIONAL'};
				
		$csv_values{"M_LEVEL"} = $message->{Header}->{Fields}->[19]->{Decimal}
			if $is_in_csv_fields{'M_LEVEL'};
				
		$csv_values{"M_MODE"} = $message->{Header}->{Fields}->[20]->{Decimal}
			if $is_in_csv_fields{'M_MODE'};
			
		if ($message->{Header}->{Fields}->[0]->{Decimal} == 6) {
			# TELEGRAM FROM BALISE
			$csv_values{"BALISE_Q_UPDOWN"} = $message->{Balise}->{Header}->{Fields}->[0]->{Decimal}
				if $is_in_csv_fields{'BALISE_Q_UPDOWN'};
				
			$csv_values{"BALISE_M_VERSION"} = $message->{Balise}->{Header}->{Fields}->[1]->{Decimal}
				if $is_in_csv_fields{'BALISE_M_VERSION'};
				
			$csv_values{"BALISE_Q_MEDIA"} = $message->{Balise}->{Header}->{Fields}->[2]->{Decimal}
				if $is_in_csv_fields{'BALISE_Q_MEDIA'};
				
			$csv_values{"BALISE_N_PIG"} = $message->{Balise}->{Header}->{Fields}->[3]->{Decimal}
				if $is_in_csv_fields{'BALISE_N_PIG'};
				
			$csv_values{"BALISE_N_TOTAL"} = $message->{Balise}->{Header}->{Fields}->[4]->{Decimal}
				if $is_in_csv_fields{'BALISE_N_TOTAL'};
				
			$csv_values{"BALISE_M_DUP"} = $message->{Balise}->{Header}->{Fields}->[5]->{Decimal}
				if $is_in_csv_fields{'BALISE_M_DUP'};
				
			$csv_values{"BALISE_M_MCOUNT"} = $message->{Balise}->{Header}->{Fields}->[6]->{Decimal}
				if $is_in_csv_fields{'BALISE_M_MCOUNT'};
				
			$csv_values{"BALISE_NID_C"} = $message->{Balise}->{Header}->{Fields}->[7]->{Decimal}
				if $is_in_csv_fields{'BALISE_NID_C'};
				
			$csv_values{"RBC_NID_MESSAGE"} = $message->{Balise}->{Header}->{Fields}->[8]->{Decimal}
				if $is_in_csv_fields{'RBC_NID_MESSAGE'};
				
			$csv_values{"BALISE_NID_BG"} = $message->{Balise}->{Header}->{Fields}->[9]->{Decimal}
				if $is_in_csv_fields{'BALISE_NID_BG'};
		} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 9) {
			# MESSAGE FROM RBC
			$csv_values{"RBC_NID_MESSAGE"} = $message->{MessageFromRBC}->{Fields}->[0]->{Decimal}
				if $is_in_csv_fields{'RBC_NID_MESSAGE'};
				
			$csv_values{"RBC_L_MESSAGE"} = $message->{MessageFromRBC}->{Fields}->[1]->{Decimal}
				if $is_in_csv_fields{'RBC_L_MESSAGE'};
				
			$csv_values{"RBC_T_TRAIN"} = $message->{MessageFromRBC}->{Fields}->[2]->{Decimal}
				if $is_in_csv_fields{'RBC_T_TRAIN'};
				
			$csv_values{"RBC_M_ACK"} = $message->{MessageFromRBC}->{Fields}->[3]->{Decimal}
				if $is_in_csv_fields{'RBC_M_ACK'};
				
			$csv_values{"RBC_NID_LRBG"} = $message->{MessageFromRBC}->{Fields}->[4]->{Text}
				if $is_in_csv_fields{'RBC_NID_LRBG'};
		} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 10) {
			# MESSAGE TO RBC
			$csv_values{"RBC_NID_MESSAGE"} = $message->{MessageToRBC}->{Fields}->[0]->{Decimal}
				if $is_in_csv_fields{'RBC_NID_MESSAGE'};
				
			$csv_values{"RBC_L_MESSAGE"} = $message->{MessageToRBC}->{Fields}->[1]->{Decimal}
				if $is_in_csv_fields{'RBC_L_MESSAGE'};
				
			$csv_values{"RBC_T_TRAIN"} = $message->{MessageToRBC}->{Fields}->[2]->{Decimal}
				if $is_in_csv_fields{'RBC_T_TRAIN'};
				
			$csv_values{"RBC_NID_ENGINE"} = $message->{MessageToRBC}->{Fields}->[3]->{Decimal}
				if $is_in_csv_fields{'RBC_NID_ENGINE'};
		}
	} elsif ($message->{MessageType} eq "DRUMessage") {
		# parse DRU/ETCS record
	} else {
		print "Onbekend berichttype ".$message->{MessageType}." in record ".$DRU->{record_number}."\n";
	}
	
	# print record inhoud in CSV bestand
	if (%csv_values) {
		foreach (@csv_fields) {
			my $value = "";
			if (exists $csv_values{$_}) {
				$value = $csv_values{$_};
			}
#			print "Geen waarde voor $_\n" if not defined $value;
			print CSVFILE "$value;" if defined $value;
		}
		print CSVFILE "\n";
	}
}

print $fhxml "</DRULog>\n";
close $fhxml;
close DBGFILE;
close CSVFILE;
close $JRUfh;

print "\n";
