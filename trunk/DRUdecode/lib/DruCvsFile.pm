{
	package DruCsvFile;
	require Exporter;

	@ISA = qw(Exporter);

	# csv_fields geeft aan welke velden in de CSV file moeten komen.
	# Op dit moment zijn alle velden hierin opgenomen.	
	my @csv_fields = ( 
		# main fields
		"NID_MESSAGE",				# JRU / DRU message
		"NID_MESSAGE_TEXT",			# JRU / DRU message
		"DATE",					# JRU / DRU message
		"TIME",					# JRU / DRU message
		"Q_SCALE",				# JRU message
		"NID_LRBG",				# JRU message
		"D_LRBG",				# JRU message
		"Q_DIRLRBG",				# JRU message
		"Q_DLRBG",				# JRU message
		"L_DOUBTOVER",				# JRU message
		"L_DOUBTUNDER",				# JRU message
		"V_TRAIN",				# JRU message
		"NID_OPERATIONAL",			# JRU message
		"M_LEVEL",				# JRU message
		"M_LEVEL_TEXT",				# JRU message
		"M_MODE",				# JRU message
		"M_MODE_TEXT",				# JRU message
		"PADDING_T",				# DRU message
	
		"JRU_STATE",				# (0) JRU state
		"JRU_STATE_TEXT",			# (0) JRU state
	
		"V_MAXTRAIN",				# (2) data entry / train data
		"NC_TRAIN",				# (2) data entry / train data
		"L_TRAIN",				# (2) data entry / train data
#		"N_SERVICE_SECTIONS",			# (2) data entry / train data
#		"N_EMERGENCY_SECTIONS",			# (2) data entry / train data
#		"V_EMERGENCYDECEL_CAP",			# (2) data entry / train data
#		"A_EMERGENCYDECEL_CAP",			# (2) data entry / train data
#		"T_CUT_OFF",				# (2) data entry / train data
#		"T_DELAY",				# (2) data entry / train data (alleen Alstom)
		"M_LOADINGGAUGE",			# (2) data entry / train data
		"M_AXLELOAD",				# (2) data entry / train data
#		"N_ITER",				# (2) data entry / train data (alleen Alstom)
#		"M_TRACTION",				# (2) data entry / train data
#		"M_AIRTIGHT",				# (2) data entry / train data
#		"M_AIRTIGHT_TEXT",			# (2) data entry / train data
#		"M_ADHESION",				# (2) data entry / train data
#		"DATA_NID_C",				# (2) data entry / train data
#		"DATA_NID_RBC",				# (2) data entry / train data
#		"DATA_NID_RADIO",			# (2) data entry / train data
	
		"M_BRAKESTATE",				# (3) brake state
		"M_BRAKESTATE_TEXT",			# (3) brake state

		"M_EVENTS",				# (5) events
		"M_EVENTS_TEXT",			# (5) events
	
		"BALISE_Q_UPDOWN",			# (6) telegram from balise
		"BALISE_M_VERSION",			# (6) telegram from balise
		"BALISE_Q_MEDIA",			# (6) telegram from balise
		"BALISE_N_PIG",				# (6) telegram from balise
		"BALISE_N_TOTAL",			# (6) telegram from balise
		"BALISE_M_DUP",				# (6) telegram from balise
		"BALISE_M_MCOUNT",			# (6) telegram from balise
#		"BALISE_NID_C",				# (6) telegram from balise
		"BALISE_NID_BG",			# (6) telegram from balise
		"BALISE_Q_LINK",			# (6) telegram from balise
		
		"RBC_NID_MESSAGE",			# (9) message from RBC / (10) message to RBC
#		"RBC_NID_MESSAGE_TEXT",			# (9) message from RBC / (10) message to RBC
		"RBC_L_MESSAGE",			# (9) message from RBC / (10) message to RBC
		"RBC_T_TRAIN",				# (9) message from RBC / (10) message to RBC
		"RBC_M_ACK",				# (9) message from RBC
		"RBC_NID_LRBG",				# (9) message from RBC
		"RBC_NID_ENGINE",			# (10) message to RBC
		
		"RBCMSG2_Q_SCALE",			# RBC message 2
#		"RBCMSG2_Q_SCALE_TEXT",			# RBC message 2
		"RBCMSG2_D_SR",				# RBC message 2
		"RBCMSG8_T_TRAIN",			# RBC message 8
 		"RBCMSG15_NID_EM",			# RBC message 15
    		"RBCMSG15_Q_SCALE",			# RBC message 15
#    		"RBCMSG15_Q_SCALE_TEXT",		# RBC message 15
		"RBCMSG15_Q_DIR",			# RBC message 15
		"RBCMSG15_D_EMERGENCYSTOP",		# RBC message 15
		"RBCMSG16_NID_EM",			# RBC message 16
		"RBCMSG18_NID_EM",			# RBC message 18
		"RBCMSG27_T_TRAIN",			# RBC message 27
		"RBCMSG32_M_VERSION",			# RBC message 32
		"RBCMSG33_Q_SCALE",			# RBC message 33
#		"RBCMSG33_Q_SCALE_TEXT",		# RBC message 33
		"RBCMSG33_D_REF",			# RBC message 33
		"RBCMSG34_Q_SCALE",			# RBC message 34
#		"RBCMSG34_Q_SCALE_TEXT",		# RBC message 34
		"RBCMSG34_Q_DIR",			# RBC message 34
#		"RBCMSG34_Q_DIR_TEXT",			# RBC message 34
		"RBCMSG34_D_TAFDISPLAY",		# RBC message 34
		"RBCMSG34_L_TAFDISPLAY",		# RBC message 34

		"RBCMSG132_Q_TRACKDEL",			# RBC message 132
		"RBCMSG137_T_TRAIN",			# RBC message 137
		"RBCMSG138_T_TRAIN",			# RBC message 138
		"RBCMSG146_T_TRAIN",			# RBC message 146
		"RBCMSG147_NID_EM",			# RBC message 147
		"RBCMSG147_Q_EMERGENCYSTOP",		# RBC message 147
		"RBCMSG157_Q_STATUS",			# RBC message 157
		"RBCMSG157_Q_STATUS_TEXT",		# RBC message 157
		
		"PACKET_NID_PACKETS",			# RBC PACKETS
		"PACKET_NID_PACKETS_TEXT",		# RBC PACKETS
		
		"RBCPCK4_M_ERROR",			# RBC packet 4 ERROR REPORTING
		"RBCPCK4_M_ERROR_TEXT",			# RBC packet 4

		"RBCPCK5_D_LINK",			# RBC packet 5 LINKING
#		"RBCPCK5_NID_C",			# RBC packet 5
		"RBCPCK5_NID_BG",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION",		# RBC packet 5
		"RBCPCK5_Q_LINKACC",			# RBC packet 5
		"RBCPCK5_N_ITER",			# RBC packet 5
		"RBCPCK5_D_LINK_1",			# RBC packet 5
#		"RBCPCK5_NID_C_1",			# RBC packet 5
		"RBCPCK5_NID_BG_1",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION_1",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION_1",		# RBC packet 5
		"RBCPCK5_Q_LINKACC_1",			# RBC packet 5
		"RBCPCK5_D_LINK_2",			# RBC packet 5
#		"RBCPCK5_NID_C_2",			# RBC packet 5
		"RBCPCK5_NID_BG_2",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION_2",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION_2",		# RBC packet 5
		"RBCPCK5_Q_LINKACC_2",			# RBC packet 5
		"RBCPCK5_D_LINK_3",			# RBC packet 5
#		"RBCPCK5_NID_C_3",			# RBC packet 5
		"RBCPCK5_NID_BG_3",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION_3",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION_3",		# RBC packet 5
		"RBCPCK5_Q_LINKACC_3",			# RBC packet 5
		"RBCPCK5_D_LINK_4",			# RBC packet 5
#		"RBCPCK5_NID_C_4",			# RBC packet 5
		"RBCPCK5_NID_BG_4",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION_4",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION_4",		# RBC packet 5
		"RBCPCK5_Q_LINKACC_4",			# RBC packet 5
		"RBCPCK5_D_LINK_5",			# RBC packet 5
#		"RBCPCK5_NID_C_5",			# RBC packet 5
		"RBCPCK5_NID_BG_5",			# RBC packet 5
		"RBCPCK5_Q_LINKORIENTATION_5",		# RBC packet 5
		"RBCPCK5_Q_LINKREACTION_5",		# RBC packet 5
		"RBCPCK5_Q_LINKACC_5",			# RBC packet 5

		"RBCPCK15_N_ITER",			# RBC packet 15 LEVEL 2/3 MOVEMENT AUTHORITY
		"RBCPCK15_L_SECTION_1",			# RBC packet 15
		"RBCPCK15_L_SECTION_2",			# RBC packet 15
		"RBCPCK15_L_SECTION_3",			# RBC packet 15
		"RBCPCK15_L_SECTION_4",			# RBC packet 15
		"RBCPCK15_L_SECTION_5",			# RBC packet 15
		"RBCPCK15_L_ENDSECTION",		# RBC packet 15
		"RBCPCK15_Q_DANGERPOINT",		# RBC packet 15
		"RBCPCK15_Q_OVERLAP",			# RBC packet 15

		"RBCPCK21_D_GRADIENT",			# RBC packet 21 GRADIENT PROFILE
		"RBCPCK21_Q_GDIR",			# RBC packet 21
		"RBCPCK21_G_A",				# RBC packet 21
		"RBCPCK21_N_ITER",			# RBC packet 21
		"RBCPCK21_D_GRADIENT_1",		# RBC packet 21
		"RBCPCK21_Q_GDIR_1",			# RBC packet 21
		"RBCPCK21_G_A_1",			# RBC packet 21
		"RBCPCK21_D_GRADIENT_2",		# RBC packet 21
		"RBCPCK21_Q_GDIR_2",			# RBC packet 21
		"RBCPCK21_G_A_2",			# RBC packet 21
		"RBCPCK21_D_GRADIENT_3",		# RBC packet 21
		"RBCPCK21_Q_GDIR_3",			# RBC packet 21
		"RBCPCK21_G_A_3",			# RBC packet 21
		"RBCPCK21_D_GRADIENT_4",		# RBC packet 21
		"RBCPCK21_Q_GDIR_4",			# RBC packet 21
		"RBCPCK21_G_A_4",			# RBC packet 21
		"RBCPCK21_D_GRADIENT_5",		# RBC packet 21
		"RBCPCK21_Q_GDIR_5",			# RBC packet 21
		"RBCPCK21_G_A_5",			# RBC packet 21

		"RBCPCK27_D_STATIC",			# RBC packet 27 INTERNATIONAL STATIC SPEED PROFILE
		"RBCPCK27_V_STATIC",			# RBC packet 27
		"RBCPCK27_Q_FRONT",			# RBC packet 27
		"RBCPCK27_N_ITER",			# RBC packet 27
		"RBCPCK27_D_STATIC_1",			# RBC packet 27
		"RBCPCK27_V_STATIC_1",			# RBC packet 27
		"RBCPCK27_Q_FRONT_1",			# RBC packet 27
		"RBCPCK27_D_STATIC_2",			# RBC packet 27
		"RBCPCK27_V_STATIC_2",			# RBC packet 27
		"RBCPCK27_Q_FRONT_2",			# RBC packet 27
		"RBCPCK27_D_STATIC_3",			# RBC packet 27
		"RBCPCK27_V_STATIC_3",			# RBC packet 27
		"RBCPCK27_Q_FRONT_3",			# RBC packet 27
		"RBCPCK27_D_STATIC_4",			# RBC packet 27
		"RBCPCK27_V_STATIC_4",			# RBC packet 27
		"RBCPCK27_Q_FRONT_4",			# RBC packet 27
		"RBCPCK27_D_STATIC_5",			# RBC packet 27
		"RBCPCK27_V_STATIC_5",			# RBC packet 27
		"RBCPCK27_Q_FRONT_5",			# RBC packet 27

		"RBCPCK41_D_LEVELTR",			# RBC packet 41 LEVEL TRANSITION ORDER
		"RBCPCK41_M_LEVELTR",			# RBC packet 41
		"RBCPCK41_NID_STM",			# RBC packet 41
		"RBCPCK41_L_ACKLEVELTR",		# RBC packet 41
		"RBCPCK41_N_ITER",			# RBC packet 41
		"RBCPCK41_M_LEVELTR_1",			# RBC packet 41
		"RBCPCK41_NID_STM_1",			# RBC packet 41
		"RBCPCK41_L_ACKLEVELTR_1",		# RBC packet 41
		"RBCPCK41_M_LEVELTR_2",			# RBC packet 41
		"RBCPCK41_NID_STM_2",			# RBC packet 41
		"RBCPCK41_L_ACKLEVELTR_2",		# RBC packet 41

		"RBCPCK45_NID_MN",			# RBC packet 45 Radio Network registration
		
		"RBCPCK65_NID_TSR",			# RBC packet 65 TEMPORARY SPEED RESTRICTION
		"RBCPCK65_D_TSR",			# RBC packet 65
		"RBCPCK65_L_TSR",			# RBC packet 65
		"RBCPCK65_Q_FRONT",			# RBC packet 65
		"RBCPCK65_V_TSR",			# RBC packet 65

		"RBCPCK66_NID_TSR",			# RBC packet 66 TEMPORARY SPEED RESTRICTION REVOCATION
    
		"RBCPCK80_D_MAMODE",			# RBC packet 80 MODE PROFILE
		"RBCPCK80_M_MAMODE",			# RBC packet 80
		"RBCPCK80_V_MAMODE",			# RBC packet 80
		"RBCPCK80_L_MAMODE",			# RBC packet 80
		"RBCPCK80_L_ACKMAMODE",			# RBC packet 80
		"RBCPCK80_N_ITER",			# RBC packet 80
		"RBCPCK80_D_MAMODE_1",			# RBC packet 80
		"RBCPCK80_M_MAMODE_1",			# RBC packet 80
		"RBCPCK80_V_MAMODE_1",			# RBC packet 80
		"RBCPCK80_L_MAMODE_1",			# RBC packet 80
		"RBCPCK80_L_ACKMAMODE_1",		# RBC packet 80
		"RBCPCK80_D_MAMODE_2",			# RBC packet 80
		"RBCPCK80_M_MAMODE_2",			# RBC packet 80
		"RBCPCK80_V_MAMODE_2",			# RBC packet 80
		"RBCPCK80_L_MAMODE_2",			# RBC packet 80
		"RBCPCK80_L_ACKMAMODE_2",		# RBC packet 80

		"RBCPCK131_D_RBCTR",			# RBC packet 131 RBC TRANSITION ORDER
#		"RBCPCK131_NID_C",			# RBC packet 131
		"RBCPCK131_NID_RBC",			# RBC packet 131
		"RBCPCK131_NID_RADIO",			# RBC packet 131
		"RBCPCK131_Q_SLEEPSESSION",		# RBC packet 131

		"DRIVERS_ACTION",			# (11) drivers action
		"DRIVERS_ACTION_TEXT",			# (11) drivers action
		
#		"BALISE_GROUP_ERROR_NID_C", 		# (12) balise group error
		"BALISE_GROUP_ERROR_NID_BG",		# (12) balise group error
		"BALISE_GROUP_ERROR", 			# (12) balise group error
		"BALISE_GROUP_ERROR_TEXT",		# (12) balise group error
	
		"RADIO_LINK_ERROR", 			# (13) radio link supervision error
		"RADIO_LINK_ERROR_TEXT",		# (13) radio link supervision error
#		"RADIO_LINK_ERROR_NID_C", 		# (13) radio link supervision error
		"RADIO_LINK_ERROR_NID_RBC",		# (13) radio link supervision error
	
		"PREDEFINED_TEXT_Q_TEXTCLASS", 		# (15) predefined text
		"PREDEFINED_TEXT_Q_TEXTCLASS_TEXT", 	# (15) predefined text
		"PREDEFINED_TEXT_Q_TEXTCONFIRM", 	# (15) predefined text
		"PREDEFINED_TEXT_Q_TEXTCONFIRM_TEXT", 	# (15) predefined text
		"PREDEFINED_TEXT_Q_TEXT", 		# (15) predefined text
		"PREDEFINED_TEXT_Q_TEXT_TEXT", 		# (15) predefined text
		"PREDEFINED_TEXT_L_TEXT", 		# (15) predefined text
		"PREDEFINED_TEXT_X_TEXT", 		# (15) predefined text
		"PREDEFINED_TEXT_X_TEXT_TEXT", 		# (15) predefined text
	
		"PLAIN_TEXT_Q_TEXTCLASS", 		# (16) plain text
		"PLAIN_TEXT_Q_TEXTCLASS_TEXT", 		# (16) plain text
		"PLAIN_TEXT_Q_TEXTCONFIRM", 		# (16) plain text
		"PLAIN_TEXT_Q_TEXTCONFIRM_TEXT", 	# (16) plain text
		"PLAIN_TEXT_L_TEXT", 			# (16) plain text
		"PLAIN_TEXT_X_TEXT", 			# (16) plain text
		"PLAIN_TEXT_X_TEXT_TEXT", 		# (16) plain text
	
		"M_INDICATION_6",			# (17) indication to the driver
		"M_INDICATION_5",			# (17) indication to the driver
#		"M_INDICATION_5_TEXT",			# (17) indication to the driver
		"M_INDICATION_4",			# (17) indication to the driver
#		"M_INDICATION_4_TEXT",			# (17) indication to the driver
		"M_INDICATION_3",			# (17) indication to the driver
#		"M_INDICATION_3_TEXT",			# (17) indication to the driver
		"M_INDICATION_2",			# (17) indication to the driver
#		"M_INDICATION_2_TEXT",			# (17) indication to the driver
		"M_INDICATION_1",			# (17) indication to the driver
#		"M_INDICATION_1_TEXT",			# (17) indication to the driver
		"V_PERMITTED",				# (17) indication to the driver
#		"V_PERMITTED_TEXT",			# (17) indication to the driver
		"V_TARGET",				# (17) indication to the driver
#		"V_TARGET_TEXT",			# (17) indication to the driver
		"L_TARGET",				# (17) indication to the driver
		"V_RELEASE",				# (17) indication to the driver
#		"V_RELEASE_TEXT",			# (17) indication to the driver
		"Q_WARNING",				# (17) indication to the driver
	
		"ETCS_ID",				# (20) ETCS ID
		
		"DRU_NID_PACKET",			# DRU packet
		"DRU_L_PACKET",				# DRU packet
		"DRU_NID_SOURCE",			# DRU packet
#		"DRU_NID_SOURCE_TEXT",			# DRU packet
		"DRU_M_DIAG",				# DRU packet
		"DRU_NID_CHANNEL",			# DRU packet
		"DRU_L_TEXT",				# DRU packet
		"DRU_X_TEXT",				# DRU packet
		"DRU_X_TEXT_TEXT",			# DRU packet
	
	);
	my %is_in_csv_fields = ();	# hash met velden in @csv_fields
	for (@csv_fields) { $is_in_csv_fields{$_} = 1 };

	BEGIN {
		$JruCvsFile::VERSION = "0.1";
		#
		# 0.1 Initial version
		#
  	}

	use ETCS;
	use strict;
	use Data::Dumper;

	sub new {
		my $FileName = shift;
		my $CvsFh;
		
		print "Open CVS file: " . $FileName . "\n";
		open $CvsFh, ">" . $FileName or die "Error: Kan output bestand " . $FileName . " niet openen.\n";
		
		# write header data
		print $CvsFh join(";",@csv_fields),"\n";
		
	        my $self = {};
	        $self->{FileHandle} = $CvsFh;
		bless $self;
		return $self;
	}
	
	sub combineDecimalAndText {
		my $item = shift;
		my $retval = "";
			
		$retval = $retval . "(" . $item->{Decimal} . ")-->" 
			if defined $item->{Decimal};
		$retval = $retval . "(" . $item->{Bits} . ")-->" 
			if not defined $item->{Decimal} and defined $item->{Bits};
		
		$retval = $retval . $item->{Text} if defined $item->{Text};
		$retval = $retval . "UNKNOWN" if not defined $item->{Text};
		
		return $retval;
	}

	sub combineDecimalOrBits {
		my $item = shift;
		my $retval = "";
			
		$retval = $item->{Decimal}
			if defined $item->{Decimal};
		$retval =~ s/\./,/g;
		$retval = $item->{Bits}
			if not defined $item->{Decimal} and defined $item->{Bits};
		
		return $retval;
	}
	
	sub process {
		my ($self, $message) = @_;
		my %csv_values = ();		# array om de waarden op te slaan.
		my $packets = ();
		my $packet_iter;

	        # copy message to csv_values
		$csv_values{"NID_MESSAGE"} = $message->{Header}->{Fields}->[0]->{Decimal}
			if $is_in_csv_fields{'NID_MESSAGE'};
				
		$csv_values{"NID_MESSAGE_TEXT"} = combineDecimalAndText($message->{Header}->{Fields}->[0])
			if ($is_in_csv_fields{'NID_MESSAGE_TEXT'}) && ($message->{MessageType} eq "JRUMessage");
				
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

		if ($message->{MessageType} eq "JRUMessage") {
			
			# PROCESS MESSAGE HEADER
			
			$csv_values{"Q_SCALE"} = $message->{Header}->{Fields}->[9]->{Decimal}
				if $is_in_csv_fields{'Q_SCALE'};
					
			$csv_values{"NID_LRBG"} = $message->{Header}->{Fields}->[10]->{Text}
				if $is_in_csv_fields{'NID_LRBG'};
					
			$csv_values{"D_LRBG"} = combineDecimalOrBits($message->{Header}->{Fields}->[11])
				if $is_in_csv_fields{'D_LRBG'};
					
			$csv_values{"Q_DIRLRBG"} = combineDecimalOrBits($message->{Header}->{Fields}->[12])
				if $is_in_csv_fields{'Q_DIRLRBG'};
					
			$csv_values{"Q_DLRBG"} = combineDecimalOrBits($message->{Header}->{Fields}->[13])
				if $is_in_csv_fields{'Q_DLRBG'};
					
			$csv_values{"L_DOUBTOVER"} = combineDecimalOrBits($message->{Header}->{Fields}->[14])
				if $is_in_csv_fields{'L_DOUBTOVER'};
					
			$csv_values{"L_DOUBTUNDER"} = combineDecimalOrBits($message->{Header}->{Fields}->[15])
				if $is_in_csv_fields{'L_DOUBTUNDER'};
					
			$csv_values{"V_TRAIN"} = combineDecimalOrBits($message->{Header}->{Fields}->[16])
				if $is_in_csv_fields{'V_TRAIN'};
					
			$csv_values{"DRIVER_ID"} = $message->{Header}->{Fields}->[17]->{Decimal}
				if $is_in_csv_fields{'DRIVER_ID'};
					
			$csv_values{"NID_OPERATIONAL"} = $message->{Header}->{Fields}->[18]->{Hex}
				if $is_in_csv_fields{'NID_OPERATIONAL'};
					
			$csv_values{"M_LEVEL"} = $message->{Header}->{Fields}->[19]->{Decimal}
				if $is_in_csv_fields{'M_LEVEL'};
					
			$csv_values{"M_LEVEL_TEXT"} = combineDecimalAndText($message->{Header}->{Fields}->[19])
				if $is_in_csv_fields{'M_LEVEL_TEXT'};
					
			$csv_values{"M_MODE"} = $message->{Header}->{Fields}->[20]->{Decimal}
				if $is_in_csv_fields{'M_MODE'};
				
			$csv_values{"M_MODE_TEXT"} = combineDecimalAndText($message->{Header}->{Fields}->[20])
				if $is_in_csv_fields{'M_MODE_TEXT'};
				
			# PROCESS SPECIFIC MESSAGES
			
			if ($message->{Header}->{Fields}->[0]->{Decimal} == 0) {
				$csv_values{"JRU_STATE"} = $message->{JRUStateMsg}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'JRU_STATE'};
				$csv_values{"JRU_STATE_TEXT"} = combineDecimalAndText($message->{JRUStateMsg}->{Fields}->[0])
					if $is_in_csv_fields{'JRU_STATE_TEXT'};
				
				# map packets	
				$packets = $message->{JRUStateMsg}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 1) {
				# doe niets
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 2) {
				$csv_values{"V_MAXTRAIN"} = combineDecimalOrBits($message->{DataEntryCompleted}->{Fields}->[0])
					if $is_in_csv_fields{'V_MAXTRAIN'};
				$csv_values{"NC_TRAIN"} = $message->{DataEntryCompleted}->{Fields}->[1]->{Bits}
					if $is_in_csv_fields{'NC_TRAIN'};
				$csv_values{"L_TRAIN"} = $message->{DataEntryCompleted}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'L_TRAIN'};
				my $nServiceSections = $message->{DataEntryCompleted}->{Fields}->[3]->{Decimal};
				$csv_values{"N_SERVICE_SECTIONS"} = $message->{DataEntryCompleted}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'N_SERVICE_SECTIONS'};
        my $nSections = 0;
				if ($nServiceSections>0) {
				  # copieer Service brake sections
          # increment nSections
				  $nSections = 2*$nServiceSections;
				}
				my $nEmergencySections = $message->{DataEntryCompleted}->{Fields}->[4+$nSections]->{Decimal};
				$csv_values{"N_EMERGENCY_SECTIONS"} = $message->{DataEntryCompleted}->{Fields}->[4+$nSections]->{Decimal}
					if $is_in_csv_fields{'N_EMERGENCY_SECTIONS'};
				if ($nEmergencySections>0) {
				  # copieer Emergency brake sections
  				$csv_values{"V_EMERGENCYDECEL_CAP"} = $message->{DataEntryCompleted}->{Fields}->[5+$nSections]->{Decimal}
  					if $is_in_csv_fields{'V_EMERGENCYDECEL_CAP'};
  				$csv_values{"A_EMERGENCYDECEL_CAP"} = $message->{DataEntryCompleted}->{Fields}->[6+$nSections]->{Decimal}
  					if $is_in_csv_fields{'A_EMERGENCYDECEL_CAP'};
          # increment nSections
				  $nSections = $nSections + 2*$nEmergencySections;
  			}
				$csv_values{"T_CUT_OFF"} = $message->{DataEntryCompleted}->{Fields}->[5+$nSections]->{Decimal}
					if $is_in_csv_fields{'T_CUT_OFF'};
				if ($message->{DataEntryCompleted}->{Fields}->[6+$nSections]->{Field} == 'T_SERVICE_DELAY') {
				  $nSections = $nSections + 1; # skip service delay
  			}
				$csv_values{"T_DELAY"} = $message->{DataEntryCompleted}->{Fields}->[6+$nSections]->{Decimal}
					if $is_in_csv_fields{'T_DELAY'};
				$csv_values{"M_LOADINGGAUGE"} = $message->{DataEntryCompleted}->{Fields}->[7+$nSections]->{Decimal}
					if $is_in_csv_fields{'M_LOADINGGAUGE'};
				$csv_values{"M_AXLELOAD"} = $message->{DataEntryCompleted}->{Fields}->[8+$nSections]->{Decimal}
					if $is_in_csv_fields{'M_AXLELOAD'};
				if ($message->{DataEntryCompleted}->{Fields}->[9+$nSections]->{Field} == 'N_ITER') {
  				$csv_values{"N_ITER"} = $message->{DataEntryCompleted}->{Fields}->[9+$nSections]->{Decimal}
  					if $is_in_csv_fields{'N_ITER'};
  				$nSections = $nSections + 1;
  			}
				$csv_values{"M_TRACTION"} = $message->{DataEntryCompleted}->{Fields}->[9+$nSections]->{Decimal}
					if $is_in_csv_fields{'M_TRACTION'};
				$csv_values{"M_AIRTIGHT"} = $message->{DataEntryCompleted}->{Fields}->[10+$nSections]->{Decimal}
					if $is_in_csv_fields{'M_AIRTIGHT'};
				$csv_values{"M_AIRTIGHT_TEXT"} = combineDecimalAndText($message->{DataEntryCompleted}->{Fields}->[10+$nSections])
					if $is_in_csv_fields{'M_AIRTIGHT_TEXT'};
				$csv_values{"M_ADHESION"} = $message->{DataEntryCompleted}->{Fields}->[11+$nSections]->{Decimal}
					if $is_in_csv_fields{'M_ADHESION'};
				if ($message->{DataEntryCompleted}->{Fields}->[12+$nSections]->{Field} == 'DATA_NID_C') {
  				$csv_values{"DATA_NID_C"} = $message->{DataEntryCompleted}->{Fields}->[12+$nSections]->{Decimal}
  					if $is_in_csv_fields{'DATA_NID_C'};
  					$nSections = $nSections + 1;
  			}
				$csv_values{"DATA_NID_RBC"} = $message->{DataEntryCompleted}->{Fields}->[12+$nSections]->{Decimal}
					if $is_in_csv_fields{'DATA_NID_RBC'};
				$csv_values{"DATA_NID_RADIO"} = $message->{DataEntryCompleted}->{Fields}->[13+$nSections]->{hex}
					if $is_in_csv_fields{'DATA_NID_RADIO'};
				
				# map packets	
				$packets = $message->{DataEntryCompleted}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 3) {
				$csv_values{"M_BRAKESTATE"} = $message->{EmergencyBrake}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'M_BRAKESTATE'};
				$csv_values{"M_BRAKESTATE_TEXT"} = combineDecimalAndText($message->{EmergencyBrake}->{Fields}->[0])
					if $is_in_csv_fields{'M_BRAKESTATE_TEXT'};
				
				# map packets	
				$packets = $message->{EmergencyBrake}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 4) {
				$csv_values{"M_BRAKESTATE"} = $message->{ServiceBrake}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'M_BRAKESTATE'};
				$csv_values{"M_BRAKESTATE_TEXT"} = combineDecimalAndText($message->{ServiceBrake}->{Fields}->[0])
					if $is_in_csv_fields{'M_BRAKESTATE_TEXT'};
				
				# map packets	
				$packets = $message->{EmergencyBrake}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 5) {
				$csv_values{"M_EVENTS"} = $message->{Events}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'M_EVENTS'};
				$csv_values{"M_EVENTS_TEXT"} = combineDecimalAndText($message->{Events}->{Fields}->[0])
					if $is_in_csv_fields{'M_EVENTS_TEXT'};
				
				# map packets	
				$packets = $message->{Events}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 6) {
				# TELEGRAM FROM BALISE
				$csv_values{"BALISE_Q_UPDOWN"} = $message->{BaliseTelegram}->{Header}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'BALISE_Q_UPDOWN'};
					
				$csv_values{"BALISE_M_VERSION"} = $message->{BaliseTelegram}->{Header}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'BALISE_M_VERSION'};
					
				$csv_values{"BALISE_Q_MEDIA"} = $message->{BaliseTelegram}->{Header}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'BALISE_Q_MEDIA'};
					
				$csv_values{"BALISE_N_PIG"} = $message->{BaliseTelegram}->{Header}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'BALISE_N_PIG'};
					
				$csv_values{"BALISE_N_TOTAL"} = $message->{BaliseTelegram}->{Header}->{Fields}->[4]->{Decimal}
					if $is_in_csv_fields{'BALISE_N_TOTAL'};
					
				$csv_values{"BALISE_M_DUP"} = $message->{BaliseTelegram}->{Header}->{Fields}->[5]->{Decimal}
					if $is_in_csv_fields{'BALISE_M_DUP'};
					
				$csv_values{"BALISE_M_MCOUNT"} = $message->{BaliseTelegram}->{Header}->{Fields}->[6]->{Decimal}
					if $is_in_csv_fields{'BALISE_M_MCOUNT'};
					
				$csv_values{"BALISE_NID_C"} = $message->{BaliseTelegram}->{Header}->{Fields}->[7]->{Decimal}
					if $is_in_csv_fields{'BALISE_NID_C'};
					
				$csv_values{"BALISE_NID_BG"} = $message->{BaliseTelegram}->{Header}->{Fields}->[8]->{Decimal}
					if $is_in_csv_fields{'BALISE_NID_BG'};

				$csv_values{"BALISE_Q_LINK"} = $message->{BaliseTelegram}->{Header}->{Fields}->[9]->{Decimal}
					if $is_in_csv_fields{'BALISE_Q_LINK'};
					
				# map packets	
				$packets = $message->{BaliseTelegram}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 9) {
				# MESSAGE FROM RBC
				$csv_values{"RBC_NID_MESSAGE"} = $message->{MessageFromRBC}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'RBC_NID_MESSAGE'};
					
				$csv_values{"RBC_NID_MESSAGE_TEXT"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[0])
					if $is_in_csv_fields{'RBC_NID_MESSAGE_TEXT'};
					
				$csv_values{"RBC_L_MESSAGE"} = $message->{MessageFromRBC}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'RBC_L_MESSAGE'};
					
				$csv_values{"RBC_T_TRAIN"} = $message->{MessageFromRBC}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'RBC_T_TRAIN'};
					
				$csv_values{"RBC_M_ACK"} = $message->{MessageFromRBC}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'RBC_M_ACK'};
					
				$csv_values{"RBC_NID_LRBG"} = $message->{MessageFromRBC}->{Fields}->[4]->{Text}
					if $is_in_csv_fields{'RBC_NID_LRBG'};
					
				# RBC Messages
				if ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 2) {
					$csv_values{"RBCMSG2_Q_SCALE"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG2_Q_SCALE'};
						
					$csv_values{"RBCMSG2_Q_SCALE_TEXT"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[5])
						if $is_in_csv_fields{'RBCMSG2_Q_SCALE_TEXT'};

					$csv_values{"RBCMSG2_D_SR"} = $message->{MessageFromRBC}->{Fields}->[6]->{Decimal}
						if $is_in_csv_fields{'RBCMSG2_D_SR'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 8) {
					$csv_values{"RBCMSG8_T_TRAIN"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG8_T_TRAIN'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 15) {
					$csv_values{"RBCMSG15_NID_EM"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG15_NID_EM'};

					$csv_values{"RBCMSG15_Q_SCALE"} = $message->{MessageFromRBC}->{Fields}->[6]->{Decimal}
						if $is_in_csv_fields{'RBCMSG15_Q_SCALE'};
						
					$csv_values{"RBCMSG15_Q_DIR"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[6])
						if $is_in_csv_fields{'RBCMSG15_Q_DIR'};

					$csv_values{"RBCMSG15_D_EMERGENCYSTOP"} = $message->{MessageFromRBC}->{Fields}->[7]->{Decimal}
						if $is_in_csv_fields{'RBCMSG15_D_EMERGENCYSTOP'};

					$csv_values{"RBCMSG15_D_EMERGENCYSTOP"} = $message->{MessageFromRBC}->{Fields}->[8]->{Decimal}
						if $is_in_csv_fields{'RBCMSG15_D_EMERGENCYSTOP'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 16) {
					$csv_values{"RBCMSG16_NID_EM"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG16_NID_EM'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 18) {
					$csv_values{"RBCMSG18_NID_EM"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG18_NID_EM'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 27) {
					$csv_values{"RBCMSG27_T_TRAIN"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG27_T_TRAIN'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 32) {
					$csv_values{"RBCMSG32_M_VERSION"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG32_M_VERSION'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 33) {
					$csv_values{"RBCMSG33_Q_SCALE"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG33_Q_SCALE'};
						
					$csv_values{"RBCMSG33_Q_SCALE_TEXT"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[5])
						if $is_in_csv_fields{'RBCMSG33_Q_SCALE_TEXT'};

					$csv_values{"RBCMSG33_D_REF"} = $message->{MessageFromRBC}->{Fields}->[6]->{Decimal}
						if $is_in_csv_fields{'RBCMSG33_D_REF'};
				} elsif ($message->{MessageFromRBC}->{Fields}->[0]->{Decimal} == 34) {
					$csv_values{"RBCMSG34_Q_SCALE"} = $message->{MessageFromRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG34_Q_SCALE'};
						
					$csv_values{"RBCMSG34_Q_SCALE_TEXT"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[5])
						if $is_in_csv_fields{'RBCMSG34_Q_SCALE_TEXT'};

					$csv_values{"RBCMSG34_Q_DIR"} = $message->{MessageFromRBC}->{Fields}->[6]->{Decimal}
						if $is_in_csv_fields{'RBCMSG34_Q_DIR'};
						
					$csv_values{"RBCMSG34_Q_DIR_TEXT"} = combineDecimalAndText($message->{MessageFromRBC}->{Fields}->[6])
						if $is_in_csv_fields{'RBCMSG34_Q_DIR_TEXT'};

					$csv_values{"RBCMSG34_D_TAFDISPLAY"} = $message->{MessageFromRBC}->{Fields}->[7]->{Decimal}
						if $is_in_csv_fields{'RBCMSG34_D_TAFDISPLAY'};
						
					$csv_values{"RBCMSG34_L_TAFDISPLAY"} = $message->{MessageFromRBC}->{Fields}->[8]->{Decimal}
						if $is_in_csv_fields{'RBCMSG34_L_TAFDISPLAY'};
				}
				
				# map packets	
				$packets = $message->{MessageFromRBC}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 10) {
				# MESSAGE TO RBC
				$csv_values{"RBC_NID_MESSAGE"} = $message->{MessageToRBC}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'RBC_NID_MESSAGE'};
					
				$csv_values{"RBC_NID_MESSAGE_TEXT"} = combineDecimalAndText($message->{MessageToRBC}->{Fields}->[0])
					if $is_in_csv_fields{'RBC_NID_MESSAGE_TEXT'};
					
				$csv_values{"RBC_L_MESSAGE"} = $message->{MessageToRBC}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'RBC_L_MESSAGE'};
					
				$csv_values{"RBC_T_TRAIN"} = $message->{MessageToRBC}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'RBC_T_TRAIN'};
					
				$csv_values{"RBC_NID_ENGINE"} = $message->{MessageToRBC}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'RBC_NID_ENGINE'};
				
				# RBC messages
				if ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 132) {
					$csv_values{"RBCMSG132_Q_TRACKDEL"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG132_Q_TRACKDEL'};
				} elsif ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 137) {
					$csv_values{"RBCMSG137_T_TRAIN"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG137_T_TRAIN'};
				} elsif ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 138) {
					$csv_values{"RBCMSG138_T_TRAIN"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG138_T_TRAIN'};
				} elsif ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 146) {
					$csv_values{"RBCMSG146_T_TRAIN"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG146_T_TRAIN'};
				} elsif ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 147) {
					$csv_values{"RBCMSG147_NID_EM"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG147_NID_EM'};

					$csv_values{"RBCMSG147_Q_EMERGENCYSTOP"} = $message->{MessageToRBC}->{Fields}->[5]->{Decimal}
						if $is_in_csv_fields{'RBCMSG147_Q_EMERGENCYSTOP'};
				} elsif ($message->{MessageToRBC}->{Fields}->[0]->{Decimal} == 157) {
					$csv_values{"RBCMSG157_Q_STATUS"} = $message->{MessageToRBC}->{Fields}->[4]->{Decimal}
						if $is_in_csv_fields{'RBCMSG157_Q_STATUS'};
						
					$csv_values{"RBCMSG157_Q_STATUS_TEXT"} = combineDecimalAndText($message->{MessageToRBC}->{Fields}->[4])
						if $is_in_csv_fields{'RBCMSG157_Q_STATUS_TEXT'};
				}

				# map packets	
				$packets = $message->{MessageToRBC}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 11) {
				$csv_values{"DRIVERS_ACTION"} = $message->{DriversActions}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'DRIVERS_ACTION'};
				$csv_values{"DRIVERS_ACTION_TEXT"} = combineDecimalAndText($message->{DriversActions}->{Fields}->[0])
					if $is_in_csv_fields{'DRIVERS_ACTION_TEXT'};
				
				# map packets	
				$packets = $message->{DriversActions}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 12) {
				$csv_values{"BALISE_GROUP_ERROR_NID_C"} = $message->{BaliseGroupError}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'BALISE_GROUP_ERROR_NID_C'};
				$csv_values{"BALISE_GROUP_ERROR_NID_BG"} = $message->{BaliseGroupError}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'BALISE_GROUP_ERROR_NID_BG'};
				$csv_values{"BALISE_GROUP_ERROR"} = $message->{BaliseGroupError}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'BALISE_GROUP_ERROR'};
				$csv_values{"BALISE_GROUP_ERROR_TEXT"} = combineDecimalAndText($message->{BaliseGroupError}->{Fields}->[2])
					if $is_in_csv_fields{'BALISE_GROUP_ERROR_TEXT'};
				
				# map packets	
				$packets = $message->{BaliseGroupError}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 13) {
				$csv_values{"RADIO_LINK_ERROR"} = $message->{RadioLinkSupervisionError}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'RADIO_LINK_ERROR'};
				$csv_values{"RADIO_LINK_ERROR_TEXT"} = combineDecimalAndText($message->{RadioLinkSupervisionError}->{Fields}->[0])
					if $is_in_csv_fields{'RADIO_LINK_ERROR_TEXT'};
				$csv_values{"RADIO_LINK_ERROR_NID_C"} = $message->{RadioLinkSupervisionError}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'RADIO_LINK_ERROR_NID_C'};
				$csv_values{"RADIO_LINK_ERROR_NID_RBC"} = $message->{RadioLinkSupervisionError}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'RADIO_LINK_ERROR_NID_RBC'};
				
				# map packets	
				$packets = $message->{RadioLinkSupervisionError}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 15) {
				$csv_values{"PREDEFINED_TEXT_Q_TEXTCLASS"} = $message->{PredefinedTextMessage}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXTCLASS'};
				$csv_values{"PREDEFINED_TEXT_Q_TEXTCLASS_TEXT"} = combineDecimalAndText($message->{PredefinedTextMessage}->{Fields}->[0])
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXTCLASS_TEXT'};
				$csv_values{"PREDEFINED_TEXT_Q_TEXTCONFIRM"} = $message->{PredefinedTextMessage}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXTCONFIRM'};
				$csv_values{"PREDEFINED_TEXT_Q_TEXTCONFIRM_TEXT"} = combineDecimalAndText($message->{PredefinedTextMessage}->{Fields}->[1])
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXTCONFIRM_TEXT'};
				$csv_values{"PREDEFINED_TEXT_Q_TEXT"} = $message->{PredefinedTextMessage}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXT'};
				$csv_values{"PREDEFINED_TEXT_Q_TEXT_TEXT"} = combineDecimalAndText($message->{PredefinedTextMessage}->{Fields}->[2])
					if $is_in_csv_fields{'PREDEFINED_TEXT_Q_TEXT_TEXT'};
				$csv_values{"PREDEFINED_TEXT_L_TEXT"} = $message->{PredefinedTextMessage}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'PREDEFINED_TEXT_L_TEXT'};
				$csv_values{"PREDEFINED_TEXT_X_TEXT"} = $message->{PredefinedTextMessage}->{Fields}->[4]->{Bits}
					if $is_in_csv_fields{'PREDEFINED_TEXT_X_TEXT'};
				$csv_values{"PREDEFINED_TEXT_X_TEXT"} = combineDecimalAndText($message->{PredefinedTextMessage}->{Fields}->[4])
					if $is_in_csv_fields{'PREDEFINED_TEXT_X_TEXT'};
				
				# map packets	
				$packets = $message->{PredefinedTextMessage}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 16) {
				$csv_values{"PLAIN_TEXT_Q_TEXTCLASS"} = $message->{PlainTextMessage}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'PLAIN_TEXT_Q_TEXTCLASS'};
				$csv_values{"PLAIN_TEXT_Q_TEXTCLASS_TEXT"} = combineDecimalAndText($message->{PlainTextMessage}->{Fields}->[0])
					if $is_in_csv_fields{'PLAIN_TEXT_Q_TEXTCLASS_TEXT'};
				$csv_values{"PLAIN_TEXT_Q_TEXTCONFIRM"} = $message->{PlainTextMessage}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'PLAIN_TEXT_Q_TEXTCONFIRM'};
				$csv_values{"PLAIN_TEXT_Q_TEXTCONFIRM_TEXT"} = combineDecimalAndText($message->{PlainTextMessage}->{Fields}->[1])
					if $is_in_csv_fields{'PLAIN_TEXT_Q_TEXTCONFIRM_TEXT'};
				$csv_values{"PLAIN_TEXT_L_TEXT"} = $message->{PlainTextMessage}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'PLAIN_TEXT_L_TEXT'};
				$csv_values{"PLAIN_TEXT_X_TEXT"} = $message->{PlainTextMessage}->{Fields}->[3]->{Bits}
					if $is_in_csv_fields{'PLAIN_TEXT_X_TEXT'};
				$csv_values{"PLAIN_TEXT_X_TEXT_TEXT"} = combineDecimalAndText($message->{PlainTextMessage}->{Fields}->[3])
					if $is_in_csv_fields{'PLAIN_TEXT_X_TEXT_TEXT'};
				
				# map packets	
				$packets = $message->{PlainTextMessage}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 17) {
				$csv_values{"M_INDICATION_6"} = $message->{IndicationsToDriver}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_6'};
				$csv_values{"M_INDICATION_5"} = $message->{IndicationsToDriver}->{Fields}->[1]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_5'};
				$csv_values{"M_INDICATION_5_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[1])
					if $is_in_csv_fields{'M_INDICATION_5_TEXT'};
				$csv_values{"M_INDICATION_4"} = $message->{IndicationsToDriver}->{Fields}->[2]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_4'};
				$csv_values{"M_INDICATION_4_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[2])
					if $is_in_csv_fields{'M_INDICATION_4_TEXT'};
				$csv_values{"M_INDICATION_3"} = $message->{IndicationsToDriver}->{Fields}->[3]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_3'};
				$csv_values{"M_INDICATION_3_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[3])
					if $is_in_csv_fields{'M_INDICATION_3_TEXT'};
				$csv_values{"M_INDICATION_2"} = $message->{IndicationsToDriver}->{Fields}->[4]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_2'};
				$csv_values{"M_INDICATION_2_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[4])
					if $is_in_csv_fields{'M_INDICATION_2_TEXT'};
				$csv_values{"M_INDICATION_1"} = $message->{IndicationsToDriver}->{Fields}->[5]->{Decimal}
					if $is_in_csv_fields{'M_INDICATION_1'};
				$csv_values{"M_INDICATION_1_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[5])
					if $is_in_csv_fields{'M_INDICATION_1_TEXT'};
				$csv_values{"V_PERMITTED"} = combineDecimalOrBits($message->{IndicationsToDriver}->{Fields}->[6])
					if $is_in_csv_fields{'V_PERMITTED'};
				$csv_values{"V_PERMITTED_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[6])
					if $is_in_csv_fields{'V_PERMITTED_TEXT'};
				$csv_values{"V_TARGET"} = combineDecimalOrBits($message->{IndicationsToDriver}->{Fields}->[7])
					if $is_in_csv_fields{'V_TARGET'};
				$csv_values{"V_TARGET_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[7])
					if $is_in_csv_fields{'V_TARGET_TEXT'};
				$csv_values{"L_TARGET"} = $message->{IndicationsToDriver}->{Fields}->[8]->{Decimal}
					if $is_in_csv_fields{'L_TARGET'};
				$csv_values{"V_RELEASE"} = combineDecimalOrBits($message->{IndicationsToDriver}->{Fields}->[9])
					if $is_in_csv_fields{'V_RELEASE'};
				$csv_values{"V_RELEASE_TEXT"} = combineDecimalAndText($message->{IndicationsToDriver}->{Fields}->[9])
					if $is_in_csv_fields{'V_RELEASE_TEXT'};
				$csv_values{"Q_WARNING"} = $message->{IndicationsToDriver}->{Fields}->[10]->{Decimal}
					if $is_in_csv_fields{'Q_WARNING'};
				
				# map packets	
				$packets = $message->{IndicationsToDriver}->{Packets};
					
			} elsif ($message->{Header}->{Fields}->[0]->{Decimal} == 20) {
				$csv_values{"ETCS_ID"} = $message->{ETCSID}->{Fields}->[0]->{Decimal}
					if $is_in_csv_fields{'ETCS_ID'};
				
				# map packets	
				$packets = $message->{ETCSID}->{Packets};
					
			} else {
				print "Deze NID_MESSAGE (" . $message->{Header}->{Fields}->[0]->{Decimal} . ") wordt nog niet ondersteund.\n";
			}
			
			# PROCESS PACKETS
			
			if (defined($packets)) {
				$packet_iter = 0;
				$csv_values{"PACKET_NID_PACKETS"} = "" if ($is_in_csv_fields{'PACKET_NID_PACKETS'});
				$csv_values{"PACKET_NID_PACKETS_TEXT"} = "" if ($is_in_csv_fields{'PACKET_NID_PACKETS_TEXT'});
				
				do {
					# print nid_packets
					#print $packets->[$packet_iter]->{Fields}->[0]->{Decimal} . " ";
					if ($is_in_csv_fields{'PACKET_NID_PACKETS'}) {
						$csv_values{"PACKET_NID_PACKETS"} = $csv_values{"PACKET_NID_PACKETS"} . " " if $packet_iter>0;
						$csv_values{"PACKET_NID_PACKETS"} = $csv_values{"PACKET_NID_PACKETS"} . sprintf("%d",$packets->[$packet_iter]->{Fields}->[0]->{Decimal});
					}
					if ($is_in_csv_fields{'PACKET_NID_PACKETS_TEXT'}) {
						$csv_values{"PACKET_NID_PACKETS_TEXT"} = $csv_values{"PACKET_NID_PACKETS_TEXT"} . " " if $packet_iter>0;
						$csv_values{"PACKET_NID_PACKETS_TEXT"} = $csv_values{"PACKET_NID_PACKETS_TEXT"} . combineDecimalAndText($packets->[$packet_iter]->{Fields}->[0]);
					}
					
					my $fieldIter = 0;
					my $fieldName = "";
					if ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 4) {
						# ERROR
						$csv_values{"RBCPCK4_M_ERROR"} = $packets->[$packet_iter]->{Fields}->[2]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK4_M_ERROR'});
						
						$csv_values{"RBCPCK4_M_ERROR_TEXT"} = combineDecimalAndText($packets->[$packet_iter]->{Fields}->[2])
							if ($is_in_csv_fields{'RBCPCK4_M_ERROR_TEXT'});
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 5) {
						# LINKING
						
						$fieldIter = 4;    
						$csv_values{"RBCPCK5_D_LINK"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_D_LINK'});

						$fieldIter++;
						if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
							$fieldIter++;
							$csv_values{"RBCPCK5_NID_C"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{'RBCPCK5_NID_C'});
						}
						$fieldIter++;
						$csv_values{"RBCPCK5_NID_BG"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_NID_BG'});

						$fieldIter++;
						$csv_values{"RBCPCK5_Q_LINKORIENTATION"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_Q_LINKORIENTATION'});

						$fieldIter++;
						$csv_values{"RBCPCK5_Q_LINKREACTION"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_Q_LINKREACTION'});

						$fieldIter++;
						$csv_values{"RBCPCK5_Q_LINKACC"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_Q_LINKACC'});

						$fieldIter++;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK5_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK5_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$fieldName = "RBCPCK5_D_LINK_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
								$fieldIter++;
								$fieldName = "RBCPCK5_NID_C_" . $counter;
								$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
									if ($is_in_csv_fields{$fieldName});
							}
							$fieldIter++;
							$fieldName = "RBCPCK5_NID_BG_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK5_Q_LINKORIENTATION_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK5_Q_LINKREACTION_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK5_Q_LINKACC_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
						}						
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 15) {
						# LEVEL 2/3 MOVEMENT AUTHORITY
						
						$fieldIter = 6;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK15_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK15_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$fieldName = "RBCPCK15_L_SECTION_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
							$fieldIter++; # skip Q_SECTIONTIMER
							if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
								$fieldIter++; # skip T_SECTIONTIMER
								$fieldIter++; # skip D_SECTIONTIMERSTOPLOC
							}
						}
						$fieldIter++;
						$csv_values{"RBCPCK15_L_ENDSECTION"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK15_L_ENDSECTION'});
						
						$fieldIter++; # skip Q_SECTIONTIMER
						if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
							$fieldIter++; # skip T_SECTIONTIMER
							$fieldIter++; # skip D_SECTIONTIMERSTOPLOC
						}
						
						$fieldIter++; # skip Q_ENDTIMER
						if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
							$fieldIter++; # skip T_ENDTIMER
							$fieldIter++; # skip D_ENDTIMERSTARTLOC
						}
						
						$fieldIter++;
						$csv_values{"RBCPCK15_Q_DANGERPOINT"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK15_Q_DANGERPOINT'});
						
						if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
							$fieldIter++; # skip D_DP
							$fieldIter++; # skip V_RELEASEDP
						}
						
						$fieldIter++;
						$csv_values{"RBCPCK15_Q_OVERLAP"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK15_Q_OVERLAP'});

						if ($packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} == 1) {
							$fieldIter++; # skip D_STARTOL
							$fieldIter++; # skip T_OL
							$fieldIter++; # skip D_OL
							$fieldIter++; # skip V_RELEASEOL
						}
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 21) {
						# GRADIENT PROFILE

						$fieldIter = 4;    
						$csv_values{"RBCPCK21_D_GRADIENT"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK21_D_GRADIENT'});

						$fieldIter++;
						$csv_values{"RBCPCK21_Q_GDIR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK21_Q_GDIR'});

						$fieldIter++;
						$csv_values{"RBCPCK21_G_A"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK21_G_A'});

						$fieldIter++;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK21_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK21_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$fieldName = "RBCPCK21_D_GRADIENT_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK21_Q_GDIR_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK21_G_A_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
						}
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 27) {
						# INTERNATIONAL STATIC SPEED PROFILE
						$fieldIter = 4;    
						$csv_values{"RBCPCK27_D_STATIC"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK27_D_STATIC'});

						$fieldIter++;
						$csv_values{"RBCPCK27_V_STATIC"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK27_V_STATIC'});

						$fieldIter++;
						$csv_values{"RBCPCK27_Q_FRONT"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK27_Q_FRONT'});

						$fieldIter++;
						my $n_Iter_2 = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
							
						# loop
						for (my $counter_2=1;$counter_2<=$n_Iter_2;$counter_2++) {
							$fieldIter++; # skip NC_DIFF
							$fieldIter++; # skip V_DIFF
						}
						
						$fieldIter++;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK27_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK27_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$fieldName = "RBCPCK27_D_STATIC_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK27_V_STATIC_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
	
							$fieldIter++;
							$fieldName = "RBCPCK27_Q_FRONT_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
								
							$fieldIter++;
							my $n_Iter_2 = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
								
							# loop
							for (my $counter_2=1;$counter_2<=$n_Iter_2;$counter_2++) {
								$fieldIter++; # skip NC_DIFF
								$fieldIter++; # skip V_DIFF
							}
						}
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 41) {
    						# LEVEL TRANSITION ORDER
    						
						$fieldIter = 4;    
						$csv_values{"RBCPCK41_D_LEVELTR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK41_D_LEVELTR'});

						$fieldIter++;
						my $mLevelTr = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK41_M_LEVELTR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK41_M_LEVELTR'});
							
						if ($mLevelTr == 1) {
							$fieldIter++;
							$csv_values{"RBCPCK41_NID_STM"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{'RBCPCK41_NID_STM'});
						}
						
						$fieldIter++;
						$csv_values{"RBCPCK41_L_ACKLEVELTR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK41_L_ACKLEVELTR'});

						$fieldIter++;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK41_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK41_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$mLevelTr = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
							$fieldName = "RBCPCK41_M_LEVELTR_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
								
							if ($mLevelTr == 1) {
								$fieldIter++;
								$fieldName = "RBCPCK41_NID_STM_" . $counter;
								$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
									if ($is_in_csv_fields{$fieldName});
							}
							
							$fieldIter++;
							$fieldName = "RBCPCK41_L_ACKLEVELTR_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
						}
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 45) {
    						# Radio Network registration
						$fieldIter = 3;    
						$csv_values{"RBCPCK45_NID_MN"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK45_NID_MN'});
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 65) {
						# TEMPORARY SPEED RESTRICTION
						$fieldIter = 4;    
						$csv_values{"RBCPCK65_NID_TSR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK65_NID_TSR'});

						$fieldIter++;
						$csv_values{"RBCPCK65_D_TSR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK65_D_TSR'});

						$fieldIter++;
						$csv_values{"RBCPCK65_L_TSR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK65_L_TSR'});

						$fieldIter++;
						$csv_values{"RBCPCK65_Q_FRONT"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK65_Q_FRONT'});

						$fieldIter++;
						$csv_values{"RBCPCK65_V_TSR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK65_V_TSR'});
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 66) {
						# TEMPORARY SPEED RESTRICTION REVOCATION
						$fieldIter = 3;    
						$csv_values{"RBCPCK66_NID_TSR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK66_NID_TSR'});
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 80) {
						# MODE PROFILE
						$fieldIter = 4;    
						$csv_values{"RBCPCK80_D_MAMODE"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_D_MAMODE'});

						$fieldIter++;
						$csv_values{"RBCPCK80_M_MAMODE"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_M_MAMODE'});

						$fieldIter++;
						$csv_values{"RBCPCK80_V_MAMODE"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_V_MAMODE'});

						$fieldIter++;
						$csv_values{"RBCPCK80_L_MAMODE"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_L_MAMODE'});

						$fieldIter++;
						$csv_values{"RBCPCK80_L_ACKMAMODE"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_L_ACKMAMODE'});

						$fieldIter++;
						my $n_Iter = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal};
						$csv_values{"RBCPCK80_N_ITER"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK80_N_ITER'});
							
						# loop
						for (my $counter=1;$counter<=$n_Iter;$counter++) {
							$fieldIter++;
							$fieldName = "RBCPCK80_D_MAMODE_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});

							$fieldIter++;
							$fieldName = "RBCPCK80_M_MAMODE_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});

							$fieldIter++;
							$fieldName = "RBCPCK80_V_MAMODE_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});

							$fieldIter++;
							$fieldName = "RBCPCK80_L_MAMODE_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});

							$fieldIter++;
							$fieldName = "RBCPCK80_L_ACKMAMODE_" . $counter;
							$csv_values{$fieldName} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
								if ($is_in_csv_fields{$fieldName});
						}
					} elsif ($packets->[$packet_iter]->{Fields}->[0]->{Decimal} == 131) {
						# RBC TRANSITION ORDER
						$fieldIter = 4;    
						$csv_values{"RBCPCK131_D_RBCTR"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK131_D_RBCTR'});

						$fieldIter++;
						$csv_values{"RBCPCK131_NID_C"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK131_NID_C'});

						$fieldIter++;
						$csv_values{"RBCPCK131_NID_RBC"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK131_NID_RBC'});

						$fieldIter++;
						$csv_values{"RBCPCK131_NID_RADIO"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK131_NID_RADIO'});

						$fieldIter++;
						$csv_values{"RBCPCK131_Q_SLEEPSESSION"} = $packets->[$packet_iter]->{Fields}->[$fieldIter]->{Decimal} 
							if ($is_in_csv_fields{'RBCPCK131_Q_SLEEPSESSION'});
					}
										
					# next packet
					$packet_iter = $packet_iter + 1;
				} while defined $packets->[$packet_iter];
				#print "\n";
			}
			
			
		} elsif ($message->{MessageType} eq "DRUMessage") {
			# parse DRU/ETCS record
			$csv_values{"PADDING_T"} = $message->{Header}->{Fields}->[9]->{Decimal}
				if $is_in_csv_fields{'PADDING_T'};

			$csv_values{"DRU_NID_PACKET"} = $message->{Fields}->[0]->{Decimal}
				if $is_in_csv_fields{'DRU_NID_PACKET'};
				
			$csv_values{"DRU_L_PACKET"} = $message->{Fields}->[1]->{Decimal}
				if $is_in_csv_fields{'DRU_L_PACKET'};
				
			$csv_values{"DRU_NID_SOURCE"} = $message->{Fields}->[2]->{Decimal}
				if $is_in_csv_fields{'DRU_NID_SOURCE'};
				
			$csv_values{"DRU_NID_SOURCE_TEXT"} = combineDecimalAndText($message->{Fields}->[6])
				if $is_in_csv_fields{'DRU_NID_SOURCE_TEXT'};

			$csv_values{"DRU_M_DIAG"} = $message->{Fields}->[3]->{Decimal}
				if $is_in_csv_fields{'DRU_M_DIAG'};
				
			$csv_values{"DRU_NID_CHANNEL"} = $message->{Fields}->[4]->{Decimal}
				if $is_in_csv_fields{'DRU_NID_CHANNEL'};
				
			$csv_values{"DRU_L_TEXT"} = $message->{Fields}->[5]->{Decimal}
				if $is_in_csv_fields{'DRU_L_TEXT'};
				
			$csv_values{"DRU_X_TEXT"} = $message->{Fields}->[6]->{Text}
				if $is_in_csv_fields{'DRU_X_TEXT_TEXT'};
		} else {
			print "Error: Onbekend berichttype " . $message->{MessageType} . "\n";
			print Dumper($message);
		}
		
		# print record inhoud in CSV bestand
		if (%csv_values) {
			# print "Print data in CSV file\n";
			my $CvsFh = $self->{FileHandle};
			foreach (@csv_fields) {
				print $CvsFh $csv_values{$_} if (defined $csv_values{$_});
				print $CvsFh ";";
			}
			print $CvsFh "\n";
		}
		else {
			# print "Geen data\n";
		}
	}
	
	sub close {
		my ($self) = @_;
		my $CvsFh = $self->{FileHandle};
		close $CvsFh;
	}
	
}

1;
