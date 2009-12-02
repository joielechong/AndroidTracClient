<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
ini_set('memory_limit', '50M');

require_once 'Zend/Loader/Autoloader.php';                                      
$autoloader = Zend_loader_Autoloader::getInstance();              // load Zend Gdata libraries

function openDB() {
  $cred = fopen("/home/mfvl/download/credentials.pg","r");
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
  
  $dbh = new PDO("pgsql:dbname=mfvl",$user,$pass);
  return $dbh;
}

function getName($getname,$id)
{
  $getname->bindParam(':id',$id,PDO::PARAM_INT);
  $getname->bindColumn('naam',$naam);
  $getname->execute();
  $rowsCount = $getname->fetch(PDO::FETCH_BOUND);
  $getname->closeCursor();
  return $naam;
}

$dbh = openDB();
$getname = $dbh->prepare('SELECT naam FROM contacts WHERE id=:id');


// set credentials for ClientLogin authentication
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

echo "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
echo "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n";
echo "<head><title>Listing contacts</title><style>\n";
echo "body {font-family: Verdana;}\n";
echo "div.name {color: red; text-decoration: none; font-weight: bolder;}\n";
echo "div.entry {display: inline; float: left; width: 400px; height: 150px; border: 2px solid; margin: 10px; padding: 5px;}\n";
echo "td {vertical-align: top;}\n";
echo "</style></head>\n";
echo "<body>\n";

try {
  // perform login and set protocol version to 3.0
  $client = Zend_Gdata_ClientLogin::getHttpClient($user, $pass, 'cp');
  $gdata = new Zend_Gdata($client);
  $gdata->setMajorProtocolVersion(3);
  
  // perform query and get result feed
  //$query = new Zend_Gdata_Query('http://www.google.com/m8/feeds/contacts/default/full?max-results=2048');
  $query = new Zend_Gdata_Query('http://www.google.com/m8/feeds/contacts/default/full');
  $feed = $gdata->getFeed($query);
  
  // display title and result count
  
  echo "<h2>".$feed->title."</h2>\n<div>\n";
  echo $feed->totalResults."contact(s) found.\n</div>\n";
  
  // parse feed and extract contact information
  // into simpler objects
  $results = array();
  foreach($feed as $entry){
    $xml = simplexml_load_string($entry->getXML());
    echo("<!--\n");
    print_r($entry);
    print_r($xml);
    echo("-->\n");
    $obj = new stdClass;
    $obj->name = (string) $entry->title;
    $obj->content = (string) $entry->content;
    $obj->orgName = (string) $xml->organization->orgName; 
    $obj->orgTitle = (string) $xml->organization->orgTitle; 
    
    foreach ($xml->email as $e) {
      $obj->emailAddress[] = (string) $e['address'];
    }
    
    foreach ($xml->phoneNumber as $p) {
      $obj->phoneNumber[] = (string) $p;
    }
    foreach ($xml->website as $w) {
      $obj->website[] = (string) $w['href'];
    }
    
    list($key,$val) = explode('=',$obj->content);                                     
    $obj->dbName = getName($getname,$val);

    $results[] = $obj;  
  }
} catch (Exception $e) {
  die('ERROR:' . $e->getMessage());  
  }

// display results
foreach ($results as $r) {
  echo "<div class=\"entry\">\n";
  echo "<div class=\"name\">";
  echo (!empty($r->name)) ? $r->name : 'Name not available'; 
  echo "</div>\n";
  echo "<div class=\"data\">\n";
  echo "<table>\n<tr><td>Organization</td><td>";
  echo $r->orgName;
  echo "</td></tr>\n";
  echo "<tr><td>Email</td><td>";
  echo @join(', ', $r->emailAddress);
  echo "</td></tr>\n";
  echo "<tr><td>Phone</td><td>";
  echo @join(', ', $r->phoneNumber);
  echo "</td></tr>\n";
  echo "<tr><td>Web</td><td>";
  echo @join(', ', $r->website);
  echo "</td></tr>\n";
  echo "<tr><td>Content</td><td>";
  echo $r->content;
  echo "</td></tr>\n";
  echo "<tr><td>DB Naam</td><td>";
  echo $r->dbName;
  echo "</td></tr>\n";
  echo "</table>\n</div>\n</div>\n</body>\n</html>\n";
}
?>