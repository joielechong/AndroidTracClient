#! /usr/bin/perl -w

use Geo::Distance;
use Data::Dumper;
use Archive::Zip qw( :ERROR_CODES );
#use Pg;
use DBI;

my $dbh = DBI->connect("dbi:Pg:dbname=radar");

#my $db=Pg::connectdb("dbname=radar");

#my $zip = Archive::Zip->new();

#die "whoops!" unless $zip->read( '/network/ICT/NB1404/C/TEMP/pois-Blitzer.zip' ) == AZ_OK;


#my @members = grep { /\.asc$/} $zip->memberNames();

#print Dumper(@members);

#foreach my $mem (@members) {
#    print $mem,"\n";

#    my $member = $zip->memberNamed($mem);

#    my $contents = $member->contents();
##    print $contents;
#    my @lines = grep {!/^\r$/} grep {!/^;/} split("\n",$contents);
#    foreach (@lines) {
#	/(.*),(.*),\"(.*)\".*$/;
#	my ($lat,$lon,$comment)=($1,$2,$3);
#	my $cmd = "INSERT INTO posten VALUES (\'$mem\', $lat, $lon, \'$comment\')";
#	$db->exec($cmd);
#    }
##    print join("\n",@lines),"\n";
#}

##my $directory = "/network/ICT/NB1404/C/Program Files/POI-Warner MioMap Edition/pois/export/"; 
#my $directory = "/tmp/export/"; 
#opendir(DIR, $directory) || die "Kan $directory niet openen: $!";
#my @ascs = grep { /\.asc$/ && -f "$directory/$_" } readdir(DIR);
#closedir DIR;
##print join(", ", @ascs),"\n";

#foreach my $file (@ascs) {
#    open FIL , "<$directory/$file" or die "Kan $file niet openen\n";
#    while (<FIL>) {
#	chomp;
#	next if /^;/;
#	/(.*),(.*),[ ]*\"(.*)\"/;
#	my ($lat,$lon,$comment)=($1,$2,$3);
#	my $cmd = "INSERT INTO posten VALUES (\'$directory/$file\', $lat, $lon, \'$comment\')";
#	$db->exec($cmd);
#    }
#    close FIL;
#}


my $posten = $dbh->selectall_arrayref("SELECT lat,lon,id,file,commentaar from posten order by id;");

my $maxrow=$#$posten;
my $sth2 = $dbh->prepare("SELECT lat,lon,id,file,commentaar FROM posten WHERE (abs(lat - ?) < 0.1) and (abs(lon - ?) < 0.1) and (id > ?) and (file <> ?)");

my $geo=new Geo::Distance;

for my $i (0..$maxrow) {
    my ($lat1,$lon1,$id1,$file1,$rem1)=@{$$posten[$i]};
    
    $sth2->execute($lat1,$lon1,$id1,$file1);
    
    while ( my @row = $sth2->fetchrow_array ) {
	my ($lat2,$lon2,$id2,$file2,$rem2) = @row;
	my $distance = $geo->distance('meter',$lat1,$lon1=>$lat2,$lon2);
	if ($distance <=200) {
	    print "$id1,$lat1,$lon1,$id2,$lat2,$lon2,$distance,\"$file1\",\"$rem1\",\"$file2\",\"$rem2\"\n"; 
	}
    }
}
