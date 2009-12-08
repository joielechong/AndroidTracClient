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
	if ($this->entry->title == 'Michiel van Loon') {
	  echo "<!--\n";var_dump($this);echo "-->\n";
	}
  }
}
?>
