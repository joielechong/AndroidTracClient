<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());

$debug=0;
#ini_set('display_errors',1);
if ($debug > 0 ) {
  error_reporting(E_ALL|E_STRICT|E_NOTICE);
  echo "hier beginnen we met 10 seconde rust\n";
#  sleep(10);
}

require_once 'Zend/Loader/Autoloader.php';
$autoloader = Zend_loader_Autoloader::getInstance();

#require_once 'Zend/Loader.php';
#Zend_Loader::registerAutoload();
#Zend_Loader::loadClass('Zend_Gdata');
#Zend_Loader::loadClass('Zend_Gdata_App_Util');
#Zend_Loader::loadClass('Zend_Gdata_HttpClient');
#Zend_Loader::loadClass('Zend_Gdata_AuthSub');
#Zend_Loader::loadClass('Zend_Gdata_ClientLogin');
#Zend_Loader::loadClass('Zend_Gdata_Calendar');

if ($debug>1) {echo "Zend geladen\n";}

$today = strftime("%Y%m%dT%H%M%S", strtotime ("+0 days"));
$beginDatum = strftime("%Y-%m-%d", strtotime ("-2 weeks"));
$eindDatum  = strftime("%Y-%m-%d", strtotime ("+2 months"));

$caltrans["Helga van Loon"]             = "H";
$caltrans["michiel"]                    = "M";
$caltrans["Paul en Jeroen"]             = "K";
$caltrans["Verjaardagen"]               = "";
$caltrans["Agenda 2e Montessorischool"] = "2";
$caltrans["Michiel van Loon"]           = "M";
$caltrans["Werk ICT"]                   = "I";
$caltrans["Werk ProRail"]               = "P";
$caltrans["Helga Werk 2e"]              = "H";
$caltrans["Schoolagenda"]               = "2";

$user = '** invalid **';
$pass = '** invalid **';

$cred = fopen("/home/mfvl/download/credentials.PC","r");
while (!feof($cred)) {
  $buffer = fgets($cred);
  if ($buffer[0] !== "#" && strstr($buffer,"=") != FALSE) {
    list($key,$val) = explode('=',$buffer);
    if ($key === "username") {
      $user = chop($val);
    }
    if ($key === "password") {
      $pass = chop($val);
    }    
  }
}
fclose($cred);
if ($debug > 0) { echo "Credentials gelezen\n";}

$service = Zend_Gdata_Calendar::AUTH_SERVICE_NAME;
if ($debug > 0) { echo "service '$service'  gezet\n";}

try {
  $client = Zend_Gdata_ClientLogin::getHttpClient($user,$pass,$service);
} catch (Zend_Gdata_App_CaptchaRequiredException $cre) {
  echo 'URI of CAPTCHA image: ' . $cre->getCaptchaUrl(). "\n";
} catch (Zend_Gdata_App_AuthException $ae) {
  var_dump($ae);
  echo "Problem authenticating: " . $ae->exception() . "\n";
}
if ($debug > 0) { echo "client gezet\n";}

$gdataCal = new Zend_Gdata_Calendar($client);
if ($debug > 0) { echo "Calendar geinitialiseerd\n";}

header("Content-Type: text/x-vcalendar");

if ($debug>0) {echo "<!-- GetCalendarListFeed step 1 time = ".time()." -->\n"; }
$calFeed= $gdataCal->getCalendarListFeed();
if ($debug>0) {echo "<!-- GetCalendarListFeed step 2 time = ".time()." -->\n";}
$calcnt = 0;
foreach ($calFeed as $calendar) {
  if (isset($caltrans[$calendar->title->text])) {
    $calid[$calcnt] = $calendar->id;
    $calname[$calcnt] = $calendar->title->text;
    $calcolor[$calcnt] = $calendar->color;
    if ($debug>0) {echo "<!-- $calcnt, ".$calendar->id." ".$calendar->title->text."\n";}
    $calcnt++;
  }
}
if ($debug>0) {echo "<!-- GetCalendarListFeed step 3 time = ".time()." -->\n";}

echo "BEGIN:VCALENDAR\n";
echo "PRODID:-//Michiel van Loon//MyCalendar retriever//EN\n";
echo "VERSION:2.0\n";
echo "CALSCALE:GREGORIAN\n";
echo "METHOD:PUBLISH\n";

echo "BEGIN:VTIMEZONE\n";
echo "TZID:Europe/Amsterdam\n";
echo "BEGIN:DAYLIGHT\n";
echo "TZOFFSETFROM:+0100\n";
echo "TZOFFSETTO:+0200\n";
echo "TZNAME:CEST\n";
echo "DTSTART:19700329T020000\n";
echo "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\n";
echo "END:DAYLIGHT\n";
echo "BEGIN:STANDARD\n";
echo "TZOFFSETFROM:+0200\n";
echo "TZOFFSETTO:+0100\n";
echo "TZNAME:CET\n";
echo "DTSTART:19701025T030000\n";
echo "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\n";
echo "END:STANDARD\n";
echo "END:VTIMEZONE\n";


if ($debug>0) {echo $beginDatum."\n".$eindDatum."\n";}

for ($i=0;$i<$calcnt;$i++) {
  $id= $calid[$i];
  $f = explode("/",$id);
#  echo "<!--CALNAME:" . $calname[$i]."-->\n";
  $calnaam=$calname[$i];
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
#  $query->setSingleEvents(Null);
#  $query->setFutureEvents(true);
  $query->setMaxResults(99999);
#  echo "<!-- EventQuery step 3 time = ".time()." -->\n";
  $eventFeed = $gdataCal->getCalendarEventFeed($query);
#  echo "<!-- EventQuery step 4 time = ".time()." -->\n";
  foreach ($eventFeed as $event) {
    echo "BEGIN:VEVENT\n";
    echo "DTSTAMP:$today\n";
    echo "CLASS:PRIVATE\n";
#    print_r($event);

    $naam = $event->title->text;
    echo "SUMMARY:";
    if ($caltrans[$calnaam] != "") echo "[".$caltrans[$calnaam]."]";
    echo "$naam\n";

    if ($event->recurrence != '' ) {
      $tzstr = strpos($event->recurrence,'BEGIN:VTIMEZONE');
      echo substr($event->recurrence,0,$tzstr-1)."\n";
    } else {
      if (strpos($event->when[0]->startTime,'T') === false) {
	$start=str_replace('-','',$event->when[0]->startTime);
      } else {
	$start=gmstrftime("%Y%m%dT%H%M%SZ",strtotime($event->when[0]->startTime));
      }
      echo "DTSTART:$start\n";
      
      if (strpos($event->when[0]->endTime,'T') === false) {
	$eind=str_replace('-','',$event->when[0]->endTime);
      } else {
	$eind=gmstrftime("%Y%m%dT%H%M%SZ",strtotime($event->when[0]->endTime));
      }
      echo "DTEND:$eind\n";
    }

    $created=gmstrftime("%Y%m%dT%H%M%SZ",strtotime($event->published->text)); 
    echo "CREATED:$created\n";

    $updated=gmstrftime("%Y%m%dT%H%M%SZ",strtotime($event->updated->text)); 
    echo "LAST-MODIFIED:$updated\n";

    $waar = $event->where[0];
    if ($waar != "") echo "LOCATION:$waar\n";

    $uid = $event->id;
#    $uid="none given\n";
#    foreach ($event->extensionElements as $ext) {
#      if ($ext->rootElement == "uid") {
#	$uid = $ext->extensionAttributes["value"]->value;
#      }
#    }
#    $uid=$event->uid->value;
#    echo "$naam $start $eind $waar $evid\n";
    echo "UID:$uid\n";
    echo "END:VEVENT\n";
#    echo "<!-- EventQuery step 5 time = ".time()." -->\n";
#    doQuery($db,"INSERT INTO calendar VALUES (\"".$naam."\",'".$startdag."','".$starttijd."','".$einddag."','".$eindtijd."','".$waar."','".$i."')");
#  echo "<!-- EventQuery step 6 time = ".time()." -->\n";
  }
}

echo "END:VCALENDAR\n";
?>
