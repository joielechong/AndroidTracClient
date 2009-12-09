<?php
require_once 'Contacts.php';

class DB_Contacts  extends Contacts {
  private $entry;
  private $tabel = array('Mobiel' => 'mobile',
			 'Werk' => 'work',
			 'Prive' => 'home',
			 'Anders' => 'other'
			 );
    
  public function __construct($entry) {
	$this->entry = $entry;
  }
  private function generic_array($d,$f1,$f2,$o) {  
    $a=array();
	$obj = new stdClass;
    foreach($d as $e) {
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
  public function getName() {
    return utf8_decode($this->entry->contact['cn']);
  }
  public function getGivenName() {
    return utf8_decode($this->entry->contact['voornaam']);
  }
  public function getFamilyName() {
    return utf8_decode($this->entry->contact['achternaam']);
  }
  public function getBirthday() {
    return utf8_decode($this->entry->contact['geboortedatum']);
  }
  public function getOrgName() {
    return utf8_decode($this->entry->contact['company']);
  }
  public function getOrgTitle() {
    return utf8_decode($this->entry->contact['function']);
  }
  public function getMail($o=0) {
    return  $this->generic_array($this->entry->mail,'type','mailaddress',$o);
  }
  public function getPhoneNumber($o=0) {
    return  $this->generic_array($this->entry->phone,'tel_type','number',$o);
  }
  public function getWebsite($o=0) {
    return  $this->generic_array($this->entry->web,'type','webpagina',$o);
  }
  public function getAddress($o=0) {
    return  $this->generic_array($this->entry->naw,'adr_type','adres',$o);
  }
  public function getTime() {
    return strtotime($this->entry->contact['updatetime']);
 }
  public function getId() {
    return $this->entry->contact['id'];
 }
}
?>
