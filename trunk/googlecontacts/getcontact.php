<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
ini_set('memory_limit', '50M');
set_error_handler(create_function('$a, $b, $c, $d', 'throw new ErrorException($b, 0, $a, $c, $d);'), E_ALL);

require_once 'Zend/Loader/Autoloader.php';                                      
$autoloader = Zend_loader_Autoloader::getInstance();              // load Zend Gdata libraries

class Contacts {
  
  private $dbh, $getname, $setname;
  private $currid,$changed;
  
  private $entry;
  
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
    $this->getname = $this->dbh->prepare('SELECT * FROM contacts WHERE id=:id');
    $this->getmail = $this->dbh->prepare('SELECT * FROM mail WHERE contact_id=:id');
    $this->getphone = $this->dbh->prepare('SELECT * FROM telephone WHERE contact_id=:id');
    $this->getfax = $this->dbh->prepare('SELECT * FROM fax WHERE contact_id=:id');
    $this->getnaw = $this->dbh->prepare('SELECT * FROM naw WHERE contact_id=:id');
    $this->getweb = $this->dbh->prepare('SELECT * FROM website WHERE contact_id=:id');
	$this->currid = -1;
  }
  
  function loadId($id) {
	if ($this->currid != $id) {
      $this->getname->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getname->execute();
	  $this->entry = new stdClass;
      $this->entry->contact = $this->getname->fetch(PDO::FETCH_ASSOC);
      $this->getname->closeCursor();
	  
      $this->getmail->bindParam(':id',$id,PDO::PARAM_INT);
	  $this->getmail->execute();
	  $this->entry->mail = $this->getmail->fetchAll(PDO::FETCH_ASSOC);
      $this->getname->closeCursor();
	  
      $this->getphone->bindParam(':id',$id,PDO::PARAM_INT);
	  $this->getphone->execute();
	  $this->entry->phone = $this->getphone->fetchAll(PDO::FETCH_ASSOC);
      $this->getphone->closeCursor();
	  
      $this->getfax->bindParam(':id',$id,PDO::PARAM_INT);
	  $this->getfax->execute();
	  $this->entry->fax = $this->getfax->fetchAll(PDO::FETCH_ASSOC);
      $this->getfax->closeCursor();
	  
      $this->getnaw->bindParam(':id',$id,PDO::PARAM_INT);
	  $this->getnaw->execute();
	  $this->entry->naw = $this->getnaw->fetchAll(PDO::FETCH_ASSOC);
      $this->getnaw->closeCursor();
	  
      $this->getweb->bindParam(':id',$id,PDO::PARAM_INT);
	  $this->getweb->execute();
	  $this->entry->web = $this->getweb->fetchAll(PDO::FETCH_ASSOC);
      $this->getweb->closeCursor();
	  
	  $this->entry->dbchanged = 0;
	  $this->entry->gglchanged = 0;
	  $this->entry->time = strtotime($this->entry->contact['updatetime']);
	  $this->currid = $id;
	}
  }
  
  private function print_diff($field,$t1,$t2) {
    if (isset($t1) && isset($t2) && $t1 !== $t2) {
      echo "<tr class=\"diff\"><td>$field</td><td>$t1</td><td>$t2</td></tr>\n";
	}
	if (isset($t1) xor isset($t2)) {
      echo "<tr class=\"diff\"><td>$field</td><td>".var_dump($t1)."</td><td>".var_dump($t2)."</td></tr>\n";
	}
  }
  
  private function print_difflist($field,$g,$d) {
  }
  
  function compare($r) {
  $entry=$this->entry;
  echo "<!--\n";print_r($r);print_r($entry);echo " -->\n";
  echo "<div class=\"entry\">\n";
  echo "<div class=\"name\">";
  echo (!empty($r->name)) ? $r->name : 'Name not available'; 
  echo "</div>\n";
  if ($r->name !== $entry->contact['naam']) {
    echo "<div class=\"diff\">\n".$entry->contact['naam']."</div>\n";
  }
  echo "<div class=\"data\">\n";
  echo "<table>\n";
  $this->print_diff("Organization",$r->orgName,$entry->contact['company']);
  $this->print_diff("Function",$r->orgTitle,$entry->contact['function']);
  echo "<tr class=\"diff\"><td>Updated</td><td>".$r->time."</td><td>".$entry->time."</td></tr>\n";
  $this->print_difflist('Email',(isset($r->emailAddress)?$r->emailAddress:NULL),(isset($entry->mail)?$entry->mail:NULL));
  $this->print_difflist('Phone',(isset($r->phoneNumber)?$r->phoneNumber:NULL),(isset($entry->phone)?$entry->phone:NULL));
  $this->print_difflist('Web',(isset($r->website)?$r->website:NULL),(isset($entry->web)?$entry->web:NULL));
  echo "</td></tr>\n";
  echo "<tr><td>Content</td><td>".$r->content."</td></tr>\n";
  
  echo "</table>\n</div>\n";
  echo "</div>\n\n";	
  }
  
}

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
	$obj->time = strtotime($entry->updated);
    $obj->orgName = (string) $xml->organization->orgName; 
    $obj->orgTitle = (string) $xml->organization->orgTitle;
	$obj->fullName = (string) $xml->fullName;
	$obj->givenName = (string) $xml->givenName;
	$obj->additionalName = (string) $xml->additionalName;
	$obj->familyName = (string) $xml->familyName;
    
    foreach ($xml->email as $e) {
	  $relstr = (string) $e['rel'];
	  if (strstr($relstr,"#") != FALSE) {
	    list($g,$rel) = explode("#",$relstr);
	  } else {
	    $rel = "??";
	  }
      $obj->emailAddress[] =  $rel.": ".(string) $e['address'];
    }
    
    foreach ($xml->phoneNumber as $p) {
	  $relstr = (string) $p['rel'];
	  if (strstr($relstr,"#") != FALSE) {
	    list($g,$rel) = explode("#",$relstr);
	  } else {
	    $rel = "mobile";
	  }
      $obj->phoneNumber[] = $rel.": ".(string) $p;
    }
    foreach ($xml->website as $w) {
	  $relstr = (string) $p['rel'];
	  if (strstr($relstr,"#") != FALSE) {
	    list($g,$rel) = explode("#",$relstr);
	  } else {
	    $rel = "work";
	  }
      $obj->website[] = $rel .": ".(string) $w['href'];
    }
    
	if (isset($obj->content) && strstr($obj->content,"=") != FALSE) {
      list($key,$val) = explode('=',$obj->content);                                     
	  if (isset($key) && ($key === "id")) {
	    $obj->id = $val;
	  }
	}
    $results[] = $obj;  
  }
} catch (Exception $e) {
  die('ERROR:' . $e->getMessage()."\n".$e->getTraceAsString()."\n");  
}

// display results
foreach ($results as $r) {
  if (isset($r->id)) {
    $cdb->loadId($r->id);
	echo $cdb->compare($r);
  } else {
  echo "<div class=\"entry\">\n";
  echo "<div class=\"name\">";
  echo (!empty($r->name)) ? $r->name : 'Name not available'; 
  echo "</div>\n";
  echo "<div class=\"data\">\n";
  echo "<table>\n<tr><td>Organization</td><td>";
  echo $r->orgName;
  echo "</td></tr>\n";
  echo "<tr><td>Email</td><td>";
  if (isset($r->emailAddress) && is_array($r->emailAddress)) {
    echo @join(', ', $r->emailAddress);
  }
  echo "</td></tr>\n";
  echo "<tr><td>Phone</td><td>";
  if (isset($r->phoneNumber) && is_array($r->phoneNumber)) {
    echo @join(', ', $r->phoneNumber);
  }
  echo "</td></tr>\n";
  echo "<tr><td>Web</td><td>";
  if (isset($r->website) && is_array($r->website)) {
    echo @join(', ', $r->website);
  }
  echo "</td></tr>\n";
  echo "<tr><td>Content</td><td>";
  echo $r->content;
  echo "</td></tr>\n";
  echo "</table>\n</div>\n</div>\n\n";
  }
}
echo "</body>\n</html>\n";
?>
