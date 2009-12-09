<?php
require_once 'Contacts.php';

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
	return -1;
  }
  public function getUpdated() {
    return (string) $this->entry->updated;
  }
  public function getTime() {
    return strtotime($this->entry->updated);
  }
  public function getBirthday() {
    echo "<!--\n";var_dump($this->xml);echo "-->\n";
    return (string) $this->xml->birthday['when'];
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
  public function getMail($o=0) {
    $emailAddress = array();
    foreach ($this->xml->email as $e) {
      $obj = new stdClass;
      $relstr = (string) $e['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "??";
      }
      $obj->rel = $rel;
      $obj->mailaddress = $e['address'];
      $obj->asString =  $rel.": ".(string) $e['address'];
      $emailAddress[] = $o==0 ? $obj->asString : $obj;
    }  
    return $emailAddress;
  }
  public function getPhoneNumber($o=0) {
    $phoneNumber = array();
    foreach ($this->xml->phoneNumber as $p) {
      $obj = new stdClass;
      $relstr = (string) $p['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "mobile";
      }
      $obj->rel = $rel;
      $obj->number= $p;
      $obj->asString = $rel.": ".(string) $p;
      $phoneNumber[] = $o==0 ? $obj->asString : $obj;
    }
    return $phoneNumber;
  }
  public function getAddress ($o=0) {
    $Address = array();
    foreach ($this->xml->structuredPostalAddress as $a) {
      $obj = new stdClass;
      $relstr = (string) $a['rel'];
      if (strstr($relstr,"#") != FALSE) {
	list($g,$rel) = explode("#",$relstr);
      } else {
	$rel = "other";
      }
      $obj->rel = $rel;
      $obj->adres = (string) $a->formattedAddress;
      $obj->asString = $rel.": ".(string) $a->formattedAddress;
      $Address[] = $o==0 ? $obj->asString : $obj;
    }
    return $Address;
  }
  public function getWebsite($o=0) {
    $website = array();
    foreach ($this->xml->website as $w) {
      $rel = (string) $w['rel'];
      $obj = new stdClass;
      $obj->rel = $rel;
      $obj->webpagina = (string) $w['href'];
      $obj->asString = $rel .": ".(string) $w['href'];
      $website[] = $o==0 ? $obj->asString : $obj;
    }
    return $website;
  } 
  
  public function __construct($entry) {
    $this->entry = $entry;
    $this->xml = simplexml_load_string($entry->getXML());
    //	if ($this->entry->title == 'Michiel van Loon') {
    //	  echo "<!--\n";var_dump($this);echo "-->\n";
    //	}
  }
}
?>
