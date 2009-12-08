<?php
set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
ini_set('memory_limit', '50M');
set_error_handler(create_function('$a, $b, $c, $d', 'throw new ErrorException($b, 0, $a, $c, $d);'), E_ALL);

require_once 'Zend/Loader/Autoloader.php';                                      
$autoloader = Zend_loader_Autoloader::getInstance();              // load Zend Gdata libraries

require_once 'CDB.php';
require_once 'DB_Contacts.php';
require_once 'GMail_Contacts.php';

echo "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
echo "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n";
echo "<head><title>Listing contacts</title><style>\n";
echo "body {font-family: Verdana;}\n";
echo "div.name {color: blue; text-decoration: none; font-weight: bolder;}\n";
echo ".diff {color: red; text-decoration: none; font-weight: bolder;}\n";
echo "div.entry {display: inline; float: left; width: 450px; height: 200px; border: 2px solid; margin: 10px; padding: 5px;}\n";
echo "td {vertical-align: top;}\n";
echo "</style></head>\n";
echo "<body>\n";

try {
  $cdb = new CDB;
  $ids=$cdb->getIds();
  $db_results=array();
  foreach ($ids as $id) {
    //    echo "Loading $id\r";
    $e = new DB_Contacts($cdb->getId($id));
    $db_results[$id] = $e;
  } 
  echo "\n";  
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
  // perform login and set protocol version to 3.0
  $client = Zend_Gdata_ClientLogin::getHttpClient($user, $pass, 'cp');
  $gdata = new Zend_Gdata($client);
  $gdata->setMajorProtocolVersion(3);
  
  // perform query and get result feed
  $query = new Zend_Gdata_Query('http://www.google.com/m8/feeds/contacts/default/full?max-results=2048');
  //$query = new Zend_Gdata_Query('http://www.google.com/m8/feeds/contacts/default/full');
  $feed = $gdata->getFeed($query);
  //echo "<!--\n";var_dump($feed);echo " -->\n";
  
  // display title and result count
  
  echo "<h2>".$feed->title."</h2>\n<div>\n";
  echo $feed->totalResults." contact(s) found.\n</div>\n";
  
  // parse feed and extract contact information
  // into simpler objects
  $gm_results = array();
  foreach($feed as $entry){
    //	echo "Loading ".$entry->content."\r";
    $obj = new GMail_Contacts($entry);
    $gm_results[] = $obj;  
  }
  echo "\n";

echo "<h3>All contacts</h3>\n";

// display results
foreach ($gm_results as $r) {
	$id = $r->getId();
	if ($id === FALSE) {
	  throw new Exception('GMail contacts not loaded?');
	} else if ($id == -1) {
	  CDB->createContact($r);
	} else {
	}
  //  if ($r->getId() !== FALSE) {
  //    $cdb->loadId($r->getId());
  //    echo $cdb->compare($r);
  //  } else {
	  echo $r->printme();
  //  }
}
echo "</body>\n</html>\n";
} catch (Exception $e) {
  die('ERROR:' . $e->getMessage()."\n".$e->getTraceAsString()."\n");  
  }
?>
