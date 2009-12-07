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
  private function generic_array($d,$f1,$f2) {  
    $a=array();
    foreach($d as $e) {
	  $t = $e[$f1];
	  if(isset($this->tabel[$t])) {
	    $t = $this->tabel[$t];
	  }
	  if ($e['class'] === 'FAX') {
	    $t = $t."_fax";
	  }
	//echo "<!-- ".$e[$f1]." $t -->\n";
	  $a[] = $t.": ".$e[$f2];
    }
	return $a;
  }
  public function getName() {
    return utf8_decode($this->entry->contact['cn']);
  }
  public function getOrgName() {
    return utf8_decode($this->entry->contact['company']);
  }
  public function getMail() {
    return  $this->generic_array($this->entry->mail,'type','mailaddress');
  }
  public function getPhoneNumber() {
    return  $this->generic_array($this->entry->phone,'tel_type','number');
  }
  public function getWebsite() {
    return  $this->generic_array($this->entry->web,'type','webpagina');
  }
  public function getAddress() {
    return  $this->generic_array($this->entry->naw,'adr_type','adres');
  }
  public function getTime() {
    return strtotime($this->entry->contact['updatetime']);
 }
  public function getId() {
    return $this->entry->contact['id'];
 }
}
?>
