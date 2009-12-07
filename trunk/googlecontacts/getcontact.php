<?php

set_include_path('/web/ZendFramework/library'.PATH_SEPARATOR.get_include_path());
ini_set('memory_limit', '50M');
set_error_handler(create_function('$a, $b, $c, $d', 'throw new ErrorException($b, 0, $a, $c, $d);'), E_ALL);

require_once 'Zend/Loader/Autoloader.php';                                      
$autoloader = Zend_loader_Autoloader::getInstance();              // load Zend Gdata libraries

class Contacts {
  public function printme() {
    echo "<div class=\"entry\">\n";
    echo "<div class=\"name\">";
    echo $this->getName() !== FALSE ? $this->getName() : 'Name not available'; 
    echo "</div>\n";
    echo "<div class=\"data\">\n";
    echo "<table>\n<tr><td>Organization</td><td>";
    echo $this->getOrgName();
    echo "</td></tr>\n";
    echo "<tr><td>Email</td><td>";
    echo @join(', ', $this->getMail());
    echo "</td></tr>\n";
    echo "<tr><td>Phone</td><td>";
    echo @join(', ', $this->getPhoneNumber());
    echo "</td></tr>\n";
    echo "<tr><td>Address</td><td>";
    echo @join(', ', $this->getAdress());
    echo "</td></tr>\n";
    echo "<tr><td>Web</td><td>";
    echo @join(', ', $this->getWebsite());
    echo "</td></tr>\n";
    echo "<tr><td>Content</td><td>";
    echo $this->getContent();
    echo "</td></tr>\n";
    echo "</table>\n</div>\n</div>\n\n";
  }
  public function getName() {
    return FALSE;
  }
  public function getOrgName() {
    return FALSE;
  }
  public function getMail() {
    return array();;
  }
  public function getPhoneNumber() {
    return array();
  }
  public function getAddress() {
    return array();
  }
  public function getWebsite() {
    return array();
  }
  public function getContent() {
    return FALSE;
  }
}

class GMail_Contacts extends Contacts {
  private $entry;
  private $xml;
  
  public function getName() {
    if (empty($this->entry->title)) {
	  return FALSE;
	}
    return (string) $this->entry->title;
  }
  public function getContent() {
    return (string) $this->entry->content;
  }
  public function getId() {
    if (isset($this->entry->content) && strstr($this->entry->content,"=") != FALSE) {
      list($key,$val) = explode('=',$this->entry->content);                                     
      if (isset($key) && ($key === "id")) {
	    return $val;
      }
    }
	return FALSE;
  }
  public function getUpdated() {
    return (string) $this->entry->updated;
  }
  public function getTime() {
    return strtotime($this->entry->updated);
  }
  public function getOrgName() {
    return (string) $this->xml->organization->orgName;
  }
  public function getOrgTitle() {
    return (string) $this->xml->organization->orgTitle;
  }
  public function getFullName() {
    return (string) $this->xml->fullName;
  }
  public function getGivenName() {
    return (string) $this->xml->givenName;
  }
  public function getAdditionalName() {
    return (string) $this->xml->additionalName;
  }
  public function getFamilyName() {
    return (string) $this->xml->familyName;
  }
  public function getMail() {
    $emailAddress = array();
    foreach ($this->xml->email as $e) {
      $relstr = (string) $e['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "??";
      }
      $emailAddress[] =  $rel.": ".(string) $e['address'];
    }  
    return $emailAddress;
  }
  public function getPhoneNumber() {
    $phoneNumber = array();
    foreach ($this->xml->phoneNumber as $p) {
      $relstr = (string) $p['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "mobile";
      }
      $phoneNumber[] = $rel.": ".(string) $p;
    }
	return $phoneNumber;
  }
  public function getAddress () {
    $Address = array();
    foreach ($this->xml->structuredPostalAddress as $a) {
      $relstr = (string) $a['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "other";
      }
      $Address[] = $rel.": ".(string) $a->formattedAddress;
    }
    return $Address;
  }
  public function getWebsite() {
    $website = array();
    foreach ($this->xml->website as $w) {
      $rel = (string) $w['rel'];
      $website[] = $rel .": ".(string) $w['href'];
    }
	return $website;
  } 
 
  public function __construct($entry) {
	$this->entry = $entry;
	$this->xml = simplexml_load_string($entry->getXML());
  }
}

class DB {
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
    $this->getphone = $this->dbh->prepare("SELECT * FROM (SELECT * FROM telephone UNION SELECT *,'f' FROM fax) as tele WHERE contact_id=:id");
    $this->getnaw = $this->dbh->prepare('SELECT * FROM naw WHERE contact_id=:id');
    $this->getweb = $this->dbh->prepare('SELECT * FROM website WHERE contact_id=:id');
  }
  
  function getIds() {
    $result = $this->dbh->query("SELECT id FROM contacts");
	return $result->fetchAll(PDO::FETCH_COLUMN,0);
  }
  
  function getId($id) {
      $entry = new stdClass;
      
      $this->getname->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getname->execute();	  
      $entry->contact = $this->getname->fetch(PDO::FETCH_ASSOC);
      $this->getname->closeCursor();
      
      $this->getmail->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getmail->execute();
      $entry->mail = $this->getmail->fetchAll(PDO::FETCH_ASSOC);
      $this->getname->closeCursor();
      
      $this->getphone->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getphone->execute();
      $entry->phone = $this->getphone->fetchAll(PDO::FETCH_ASSOC);
      $this->getphone->closeCursor();
      
      $this->getnaw->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getnaw->execute();
      $entry->naw = $this->getnaw->fetchAll(PDO::FETCH_ASSOC);
      $this->getnaw->closeCursor();
	  
      $this->getweb->bindParam(':id',$id,PDO::PARAM_INT);
      $this->getweb->execute();
      $entry->web = $this->getweb->fetchAll(PDO::FETCH_ASSOC);
      $this->getweb->closeCursor();
      
      $entry->time = strtotime($entry->contact['updatetime']);
	  return $entry;
    }
  
  
  private function print_diff($field,$t1,$t2) {
    $outstr = "";
    if (isset($t1) && strlen($t1) == 0) {
      $t1 = NULL;
    }
    if (isset($t2) && strlen($t2) == 0) {
      $t2 = NULL;
    }
    if (isset($t1) && isset($t2) && $t1 !== $t2) {
      $outstr .= "<tr class=\"diff\"><td>$field</td><td>$t1</td><td>$t2</td></tr>\n";
    }
    if (isset($t1) xor isset($t2)) {
      $outstr .= "<tr class=\"diff\"><td>$field</td><td>$t1</td><td>$t2</td></tr>\n";
    }
    return $outstr;
  }
  
  private $tabel = array('Mobiel' => 'mobile',
			 'Werk' => 'work',
			 'Prive' => 'home',
			 'Anders' => 'other'
			 );
  
  private function print_difflist($field,$g,$d,$f1,$f2) {
    $outstr = "";
    //echo "<!--\n";var_dump($g);var_dump($d);echo " -->\n";
    $ng=array();
    $nd=array();
    if (is_null($d) || count($d) == 0) {
      if (is_null($g) || count($g) == 0) {
      } else {
	$ng=$g;
      }
    } else {
      // decode email address
      $a=array();
      foreach($d as $e) {
	$t = $e[$f1];
	if (isset($this->tabel[$t])) {
	  $t = $this->tabel[$t];
	}
	if ($e['class'] === 'FAX') {
	  $t = $t."_fax";
	}
	//echo "<!-- ".$e[$f1]." $t -->\n";
	$a[] = $t.": ".$e[$f2];
      }
      if (is_null($g) || count($g) == 0) {
	$nd=$a;
      } else {
	foreach ($a as $e) {
	  if (array_search($e,$g) === FALSE) {
	    $nd[] = $e;
	  }
	}
	foreach ($g as $e) {
	  if (array_search($e,$a) === FALSE) {
	    $ng[] = $e;
	  }
	}
      }
    }
    //echo "<!--\n";var_dump($ng);var_dump($nd);echo " -->\n";
    if (count($ng) || count($nd)) {
      $outstr .= "<tr class=\"diff\"><td>$field</td><td>".join(", ",$ng)."</td><td>".join(", ",$nd)."</td></tr>\n";
    }
    return $outstr;
  }
  
  function compare($r) {
    $printit = 0;
    $entry=$this->entry;
    //echo "<!--\n";print_r($r);print_r($entry);echo " -->\n";
    echo "<!-- ".$r->name."-->\n";
    $outstr = "<div class=\"entry\">\n";
    $outstr .= "<div class=\"name\">";
    $outstr .= (!empty($r->name)) ? $r->name : 'Name not available'; 
    $outstr .= "</div>\n";
    if ($r->name !== utf8_decode($entry->contact['cn'])) {
      $outstr .= "<div class=\"diff\">\n".utf8_decode($entry->contact['cn'])."</div>\n";
      $printit = 1;
    }
    $outstr .= "<div class=\"data\">\n";
    $outstr .= "<table>\n";
    $l = strlen($outstr);
    $outstr .= $this->print_diff("Organization",$r->orgName,utf8_decode($entry->contact['company']));
    $outstr .= $this->print_diff("Function",$r->orgTitle,utf8_decode($entry->contact['function']));
    $outstr .= $this->print_difflist('Email',(isset($r->emailAddress)?$r->emailAddress:NULL),(isset($entry->mail)?$entry->mail:NULL),'type','mailaddress');
    $outstr .= $this->print_difflist('Phone',(isset($r->phoneNumber)?$r->phoneNumber:NULL),(isset($entry->phone)?$entry->phone:NULL),'tel_type','number');
    $outstr .= $this->print_difflist('Web',(isset($r->website)?$r->website:NULL),(isset($entry->web)?$entry->web:NULL),'type','webpagina');
    
    $printit = ($l != strlen($outstr));
    $outstr .= "<tr><td>Content</td><td>".$r->content."</td></tr>\n";
    $outstr .= "<tr class=\"diff\"><td>Updated</td><td>".$r->time."</td><td>".$entry->time."</td></tr>\n";
    
    $outstr .= "</table>\n</div>\n";
    $outstr .= "</div>\n\n";
    if ($printit) {
      echo $outstr;
    }
  }
}

class DB_Contacts  extends Contacts {
  private $entry;
  
  public function __construct($entry) {
	$this->entry = $entry;
  }
  
  public function getName() {
    return utf8_decode($this->contact['cn'])
  }
  public function getOrgName() {
    return utf8_decode($this->contact['company'])
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
  $cdb = new DB;
  $ids=$cdb->getIds();
  $db_results=array();
  $results=array();
  foreach ($ids as $id) {
    echo "Loading $id\r";
    $e = new DB_Contacts($cdb->getId($id));
    $db_results[$id] = $e;
	$results[] = $e;
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
	echo "Loading ".$entry->content."\r";
    $obj = new GMail_Contacts($entry);
    $gm_results[] = $obj;  
	$results[]=$e;
  }
  echo "\n";
} catch (Exception $e) {
  die('ERROR:' . $e->getMessage()."\n".$e->getTraceAsString()."\n");  
  }
  
// display results
foreach ($results as $r) {
//  if ($r->getId() !== FALSE) {
//    $cdb->loadId($r->getId());
//    echo $cdb->compare($r);
//  } else {
	  echo $r->printme();
//  }
}
echo "</body>\n</html>\n";
?>
