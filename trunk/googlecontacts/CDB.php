<?php
class CDB {
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
    $this->getnaw = $this->dbh->prepare('SELECT * FROM naw1 WHERE contact_id=:id');
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
?>
