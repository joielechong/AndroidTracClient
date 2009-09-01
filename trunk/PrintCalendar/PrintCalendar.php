<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
require_once 'Zend/Loader.php';
Zend_Loader::registerAutoload();

function doQuery($db,$query) {
#  echo "<!-- ".$query."-->\n";
  try {
    $db->exec($query);
  } catch (PDOException $e) {
     print "Error!: " . $e->getMessage() . "<br/>";
     die();
  }
}

function doSelectQuery($db,$query) {
#  echo "<!-- ".$query."-->\n";
  try {
    $result = $db->query($query);
  } catch (PDOException $e) {
     print "Error!: " . $e->getMessage() . "<br/>";
     die();
  } 
  return $result->fetchAll(PDO::FETCH_ASSOC);
}

function openDb($db_name) {
  try {
    $db = new PDO("sqlite:".$db_name);
  } catch (PDOException $e) {
     print "Error!: " . $e->getMessage() . "<br/>";
     die();
  }
  return $db;
}

$db_name="testdb-pdo.sqlite";
$db_name=":memory:";
$db=openDb($db_name);
doQuery($db,'CREATE TABLE calendar (event TEXT,startdag TEXT, starttijd TEXT, einddag TEXT, eindtijd TEXT, waar TEXT,calendar INTEGER)');
doQuery($db,'DELETE FROM calendar');

$user = '** invalid **';
$pass = '** invalid **';

$cred = fopen("/home/mfvl/download/credentials.PC","r");
while (!feof($cred)) {
  $buffer = fgets($cred);
  if ($buffer[0] !== "#" && strstr($buffer,"=") != FALSE) {
    list($key,$val) = split('=',$buffer);
    if ($key === "username") {
      $user = $val;
    }
    if ($key === "password") {
      $pass = $val;
    }    
  }
}
fclose($cred);

$service = Zend_Gdata_Calendar::AUTH_SERVICE_NAME;
$client = Zend_Gdata_ClientLogin::getHttpClient($user,$pass,$service);
$gdataCal = new Zend_Gdata_Calendar($client);
echo "<!-- GetCalendarListFeed step 1 time = ".time()." -->\n";
$calFeed= $gdataCal->getCalendarListFeed();
echo "<!-- GetCalendarListFeed step 2 time = ".time()." -->\n";
$calcnt = 0;
foreach ($calFeed as $calendar) {
  $cn = $calendar->title->text;
  echo "<!-- GetCalendarListFeed name = ".$cn." -->\n";
  if ($cn != "IB-agenda" && $cn != "Agenda 2e Montessorischool") { 
    $calid[$calcnt] = $calendar->id;
    $calname[$calcnt] = $cn;
    $calcolor[$calcnt] = $calendar->color;
    $calcnt++;
  }
}
echo "<!-- GetCalendarListFeed step 3 time = ".time()." -->\n";

if (isset($_REQUEST['maand'])) {
  $beginMaand = $_REQUEST['maand'];
} else {
  $beginMaand = date('m');
}
if (isset($_REQUEST['jaar'])) {
  $beginJaar = $_REQUEST['jaar'];
} else {
  $beginJaar = date('Y');
}

echo "<html>\n<head><link rel=\"stylesheet\" type=\"text/css\" href=\"kalender.css\">\n";
echo "<title>$beginJaar $beginMaand</title>\n";
echo "<style type=\"text/css\">\n";
for ($i=0; $i<$calcnt; $i++) {
  echo "div.kal$i {color: ".$calcolor[$i]."; font-size: xx-small;}\n";
}
echo "</style>\n</head>\n<body>\n";

$beginDatum=sprintf("%04d-%02d-01",$beginJaar,$beginMaand);
$eindMaand = $beginMaand+1;
$eindJaar = $beginJaar;
if ($eindMaand > 12) {
  $eindMaand -= 12;
  $eindJaar++;
}
$eindDatum=sprintf("%04d-%02d-01",$eindJaar,$eindMaand);

for ($i=0;$i<$calcnt;$i++) {
  $id= $calid[$i];
  $f = split("/",$id);
#  echo "<!--" . $calname[$i]."-->\n";
#  echo "<!--" . $calid[$i]."-->\n";
#  echo "<!-- EventQuery step 1 time = ".time()." -->\n";
  $query = $gdataCal->newEventQuery();
#  echo "<!-- EventQuery step 2 time = ".time()." -->\n";
  $query->setUser($f[6]);
#  echo "<!--" . $f[6]."-->\n";
  $query->setVisibility('private');
  $query->setProjection('full');
  $query->setOrderby('starttime');
  $query->setSortOrder('ascending');
  $query->setStartMin($beginDatum);
  $query->setStartMax($eindDatum);
  $query->setSingleEvents(true);
  $query->setMaxResults(99999);
#  echo "<!-- EventQuery step 3 time = ".time()." -->\n";
  $eventFeed = $gdataCal->getCalendarEventFeed($query);
#  echo "<!-- EventQuery step 4 time = ".time()." -->\n";
  foreach ($eventFeed as $event) {
    $naam = $event->title->text;
    $start = $event->when[0]->startTime;
    $eind = $event->when[0]->endTime;
    echo "<!-- $start -- $eind -->\n";
    $waar = $event->where[0];
    if (strpos($start,'T') > 0) {
      list($startdag,$starttijd) = split('\T',$start);
    } else {
      $startdag = $start;
      $starttijd = "";
    }
    if (strpos($eind,'T') > 0) {
      list($einddag,$eindtijd) = split('\T',$eind);
    } else {
      $einddag = $eind;
      $eindtijd = "";
    }
    if (! isset($starttijd)) {
      $starttijd='';
    }
    if ( !isset($eindtijd)) {
      $eindtijd='';
    }
#  echo "<!-- EventQuery step 5 time = ".time()." -->\n";
    doQuery($db,"INSERT INTO calendar VALUES (\"".$naam."\",'".$startdag."','".$starttijd."','".$einddag."','".$eindtijd."','".$waar."','".$i."')");
#  echo "<!-- EventQuery step 6 time = ".time()." -->\n";
  }
}

$aantaldagen = cal_days_in_month(CAL_GREGORIAN, $beginMaand,$beginJaar);

#echo "<!-- ArrayQuery step 1 time = ".time()." -->\n";
$result = doSelectQuery($db,'SELECT * FROM calendar ORDER BY startdag,starttijd,einddag,eindtijd');
#echo "<!-- ArrayQuery step 2 time = ".time()." -->\n";
foreach ($result as $entry) {
  $event = $entry['event'];
  $startdag = $entry['startdag'];
  $starttijd = substr($entry['starttijd'],0,5);
  $einddag = $entry['einddag'];
  $eindtijd = $entry['eindtijd'];
  $waar = $entry['waar'];
  $kalender = $entry['calendar'];
  $string = "<div class=kal$kalender>$starttijd $event</div>\n";
  if (isset($inhoud[$startdag])) {
    $inhoud[$startdag] .= $string;
  } else {
    $inhoud[$startdag] = $string;
  }
  echo "<!-- start=$startdag, eind=$einddag -->\n";
  if ($startdag != $einddag) {
    $string = "<div class=kal$kalender>$event</div>\n";
    list($jaar1,$maand1,$dag1) = split("-",$startdag);
    list($jaar2,$maand2,$dag2) = split("-",$einddag);
    if ($jaar1 < $beginJaar || $maand1 < $beginMaand) {
      $dag1 = 0;
    }
    if ($jaar1 > $beginJaar || $maand1 > $beginMaand) {
      $dag1 = $aantaldagen + 1;
    }
    if ($jaar2 > $beginJaar || $maand2 > $beginMaand) {
      $dag2 = $aantaldagen + 1;
    }
    echo "<!-- dag1 = $dag1, dag2 = $dag2 string = $string -->\n";
    for($d=$dag1+1;$d<$dag2;$d++) {
      $dag = sprintf("%04d-%02d-%02d",$beginJaar,$beginMaand,$d);
      echo "<!-- d = $d, $dag -->\n";
      if (isset($inhoud[$dag])) {
        $inhoud[$dag] .= $string;
      } else {
        $inhoud[$dag] = $string;
      }
      echo "<!-- ". $inhoud[$dag]. "-->\n";
    }
  }
}
#echo "<!-- ArrayQuery step 3 time = ".time()." -->\n";

$db = NULL;

#echo "<!-- TableBuild step 1 time = ".time()." -->\n";

$width=975;
$wkcol=30;
$rest=($width-$wkcol)/7;
$dag = $beginDatum;
echo "<table class=kalender border=1 width=$width>\n";
echo "<caption class=kalender>".substr($dag,0,7)."</caption>\n";
echo "<colgroup width=$wkcol></colgroup>\n";
echo "<colgroup span=7 width=$rest></colgroup>\n";
echo "<tr><th class=kalender>Wk</th><th class=kalender>Ma</th><th class=kalender>Di</th><th class=kalender>Wo</th><th class=kalender>Do</th><th class=kalender>Vr</th><th class=kalender>Za</th><th class=kalender>Zo</th></tr>\n";
$weeknr  = strftime("%V",strtotime($dag));
$weekdag = strftime("%u",strtotime($dag));
list($jaar,$maand,$dag) = split("-",$dag);

if ($weekdag > 1) {
  echo "<tr class=kalender><td class=wknr>$weeknr</td>";
  for($i=1;$i<$weekdag;$i++) {
    echo "<td></td>";
  }
}

#echo "<!-- TableBuild step 2 time = ".time()." -->\n";
for($dagnr=1; $dagnr<= $aantaldagen; $dagnr++) {
  $dag = sprintf("%04d-%02d-%02d",$beginJaar,$beginMaand,$dagnr);
  $weeknr  = strftime("%V",strtotime($dag));
  $weekdag = strftime("%u",strtotime($dag));
  if ($weekdag == 1) {
    if ($dagnr <> 1 ) {
      echo "</tr>\n";
    }
    echo "<tr class=kalender><td class=wknr>$weeknr</td>";
  }
  echo "<td class=kalender><div class=dag>$dagnr</div>";
  if (isset($inhoud[$dag])) {
    echo $inhoud[$dag];
  }
  echo "</td>";
}
echo "</tr>\n";
echo "</table>\n";

#echo "<!-- TableBuild step 3 time = ".time()." -->\n";

$vorigMaand = $beginMaand - 1;
$vorigJaar = $beginJaar;
if ($vorigMaand < 1) {
  $vorigMaand += 12;
  $vorigJaar--;
}

echo "<table width=$width border=0><tr>";
echo "<td align=left><a href=/PrintCalendar/?maand=$vorigMaand&jaar=$vorigJaar>".sprintf("%04d-%02d",$vorigJaar,$vorigMaand)."</a></td>";
echo "<td align=right><a href=/PrintCalendar/?maand=$eindMaand&jaar=$eindJaar>".sprintf("%04d-%02d",$eindJaar,$eindMaand)."</a></td>";
echo "</tr>\n</table>\n";

echo "</body>\n</html>\n";
?>
