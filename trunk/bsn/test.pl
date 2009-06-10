#! /usr/bin/perl -w

use Pg;

$fonds=shift;

$conn=Pg::connectdb("dbname=koersdata") or die "Cannot connect to database: $!\n";

$result=$conn->exec("select datum,slot from koers where naam = '$fonds' and datum >'1-1-1998'");

while (@row = $result->fetchrow) {
    @d=split("-",$row[0]);
    $d1="$d[2]-$d[1]-$d[0]";
    $ICT{$d1}=$row[1];
}

$result=$conn->exec("select datum,slot from koers where naam = 'AMS EOE INDEX'  and datum >'1-1-1998'");

while (@row = $result->fetchrow) {
    @d=split("-",$row[0]);
    $d1="$d[2]-$d[1]-$d[0]";
    $AEX{$d1}=$row[1];
}

@datum=sort keys %ICT;

$ictnorm=$ICT{$datum[0]};
$aexnorm=$AEX{$datum[0]};

foreach (@datum) {
    printf "%s %8.2f %8.2f\n",$_,$AEX{$_}*100/$aexnorm,$ICT{$_}*100/$ictnorm;
}

printf "Genormeerd naar koersen op %s\n",$datum[0];
printf "%20s : 100 = %8.2f\n","AEX",$AEX{$datum[0]};
printf "%20s : 100 = %8.2f\n",$fonds,$ICT{$datum[0]};

