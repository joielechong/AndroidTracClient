<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
ini_set('memory_limit', '50M');

require_once 'Zend/Loader/Autoloader.php';                                      
$autoloader = Zend_loader_Autoloader::getInstance();              // load Zend Gdata libraries

class Contacts {

  private $dbh, $getname, $setname;
  private $currid,$changed;
  
  private $name;
  private $company;
  private $function;
  private $website;
  private $birthday;
  
  function __construct() {
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
  
    $this->dbh = new PDO("pgsql:dbname=mfvl",$user,$pass);
    $this->getname = $this->dbh->prepare('SELECT naam,company,geboortedatum,webpagina FROM contacts WHERE id=:id');
	$this->currid = -1;
  }
  
  function loadId($id) {
	if ($this->currid != $id) {
      $this->getname->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getname->bindColumn('naam',$this->naam);
      $this->getname->bindColumn('company',$this->company);
      $this->getname->bindColumn('geboortedatum',$this->birthday);
      $this->getname->bindColumn('webpagina',$this->website);
      $this->getname->execute();
      $rowsCount = $this->getname->fetch(PDO::FETCH_BOUND);
      $this->getname->closeCursor();
	  $this->changed = 0;
	  $this->currid = $id;
	}
  }

  function getId()
  {
    return $this->currid;
  }

  function getName()
  {
    return $this->naam;
  }

  function getCompany()
  {
    return $this->company;
  }
}
$cdb = new Contacts;


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
  echo("<!--\n");
  print_r($feed);
  echo("-->\n");
  
  // display title and result count
  
  echo "<h2>".$feed->title."</h2>\n<div>\n";
  echo $feed->totalResults." contact(s) found.\n</div>\n";
  
  // parse feed and extract contact information
  // into simpler objects
  $results = array();
  foreach($feed as $entry){
    $xml = simplexml_load_string($entry->getXML());
    $obj = new stdClass;
    $obj->name = (string) $entry->title;
    $obj->content = (string) $entry->content;
	$obj->updated = (string) $entry->updated;
    $obj->orgName = (string) $xml->organization->orgName; 
    $obj->orgTitle = (string) $xml->organization->orgTitle;
	$obj->fullName = (string) $xml->fullName;
	$obj->givenName = (string) $xml->givenName;
	$obj->additionalName = (string) $xml->additionalName;
	$obj->familyName = (string) $xml->familyName;
    
    foreach ($xml->email as $e) {
	  $relstr = (string) $e['rel'];
	  list($g,$rel) = explode("#",$relstr);
      $obj->emailAddress[] =  $rel.": ".(string) $e['address'];
    }
    
    foreach ($xml->phoneNumber as $p) {
	  $relstr = (string) $e['rel'];
	  list($g,$rel) = explode("#",$relstr);
      $obj->phoneNumber[] = $rel.": ".(string) $p;
    }
    foreach ($xml->website as $w) {
      $obj->website[] = (string) $w['href'];
    }
    
	$obj->dbName = '*** No Id ***';
	if (isset($obj->content)) {
	  
      list($key,$val) = explode('=',$obj->content);                                     
	  if ($key === "id") {
	    $cdb->loadId($val);
        $obj->dbName = $cdb->getName();
	  }
	}

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