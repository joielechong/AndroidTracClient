<?php
require_once 'Contacts.php';

class DB_Contacts  extends Contacts {
  private $entry;
  private $tabel = array('Mobiel' => 'mobile',
			 'Werk' => 'work',
			 'Prive' => 'home',
			 'Anders' => 'other'
			 );
    
  private $tabelterug = array('mobile' => 'Mobiel',
			  'work' => 'Werk',
			  'home' => 'Prive',
			  'other' => 'Anders'
			 );
    
  public function __construct($entry) {
	$this->entry = $entry;
  }
	public function getentry() {
	  return $this->entry;
	}
	
  private function get_generic_array($d,$f1,$f2,$o) {  
    $a=array();
    foreach($d as $e) {
	  $obj = new stdClass;
	  $t = $e[$f1];
	  if(isset($this->tabel[$t])) {
	    $t = $this->tabel[$t];
	  }
	  if ($e['class'] === 'FAX') {
	    $t = $t."_fax";
	  }
	//echo "<!-- ".$e[$f1]." $t -->\n";
	  $obj->rel = $t;
	  $obj->$f2=$e[$f2];
	  $obj->id = $e['id'];
	  $obj->class = $e['class'];
	  $obj->asString = $t.": ".$e[$f2];
	  $a[] = $o==0 ? $obj->asString : $obj;
    }
	return $a;
  }
  private function set_generic_array($f1,$f2,$s) {
    $a=array();
	foreach ($s as $e) {
	  $m=array();
	  $fax = strpos($e->rel,'_fax');
	  if ($fax !== FALSE) {
	    $rel = substr($e->rel,0,$fax);
		$m['class'] = 'FAX';
	  } else {
	    $rel = $e->rel;
			$m['class'] = 'TEL';
	  }
	  if (array_key_exists($rel,$this->tabelterug)) {
	    $rel=$this->tabelterug[$rel];
	  }
	  $m[$f1]=$rel;
	  $m[$f2]= (string)$e->$f2;
	  $m['dirty']=1;
	  $a[] = $m;
	}
	return $a;
  }
  public function getName() {
    return utf8_decode($this->entry->contact['cn']);
  }
  public function setName($s) {
    $this->entry->contact['cn'] = (is_null($s)||$s=='') ? NULL : utf8_encode($s);
  }
  public function getGivenName() {
    return utf8_decode($this->entry->contact['voornaam']);
  }
  public function setGivenName($s) {
    echo "setGivenName ";var_dump($s);
    $this->entry->contact['voornaam'] = (is_null($s)||$s=='') ? NULL : utf8_encode($s);
  }
  public function getFamilyName() {
    return utf8_decode(is_null($this->entry->contact['tussenvoegsel']) ? $this->entry->contact['achternaam'] : $this->entry->contact['tussenvoegsel'].' '.$this->entry->contact['achternaam']);
  }
  public function setFamilyName($s) {
    $tvs = array('van der','den',"in 't",'von','vanden','van den',"van 't",'van de','van','te','de','ter');
	$tv = NULL;
	if ($s === NULL || $s == '') {
	  $this->entry->contact['tussenvoegsel'] = NULL;
	  $this->entry->contact['achternaam'] = NULL;
	} else {
	$a=$s;
	foreach($tvs as $t) {
	  $t1=$t." ";
	  if (substr($a,0,strlen($t1)) == $t1) {
	    $tv = $t;
		$a = substr($a,strlen($t1));
		break;
	  }
		}
    $this->entry->contact['tussenvoegsel'] = $tv ===NULL ? NULL : utf8_encode($tv);
    $this->entry->contact['achternaam'] = utf8_encode($a);
		}
  }
  public function getBirthday() {
    return utf8_decode($this->entry->contact['geboortedatum']);
  }
  public function setBirthday($s) {
    $this->entry->contact['geboortedatum'] = (is_null($s)||$s=='') ? NULL : utf8_encode($s);
  }
  public function getOrgName() {
    return utf8_decode($this->entry->contact['company']);
  }
  public function setOrgName($s) {
    $this->entry->contact['company'] = (is_null($s)||$s=='') ? NULL : utf8_encode($s);
  }
  public function getOrgTitle() {
    return utf8_decode($this->entry->contact['function']);
  }
  public function setOrgTitle($s) {
    $this->entry->contact['function'] = (is_null($s)||$s=='') ? NULL : utf8_encode($s);
  }
  public function getMail($o=0) {
    return  $this->get_generic_array($this->entry->mail,'type','mailaddress',$o);
  }
  public function setMail($s) {
    $this->entry->mail = $this->set_generic_array('type','mailaddress',$s);
  }
  public function getPhoneNumber($o=0) {
    return  $this->get_generic_array($this->entry->phone,'tel_type','number',$o);
  }
  public function setPhoneNumber($s) {
    $this->entry->phone = $this->set_generic_array('tel_type','number',$s);
  }
  public function getWebsite($o=0) {
    return  $this->get_generic_array($this->entry->web,'type','webpagina',$o);
  }
  public function setWebsite($s) {
    $this->entry->web = $this->set_generic_array('type','webpagina',$s);
  }
  public function getAddress($o=0) {
    return  $this->get_generic_array($this->entry->naw,'adr_type','adres',$o);
  }
  public function setAddress($s) {
    $this->entry->naw = $this->set_generic_array('adr_type','adres',$s);
  }
  public function getTime() {
    return strtotime($this->entry->contact['updatetime']);
 }
  public function getId() {
    return $this->entry->contact['id'];
 }
}
?>