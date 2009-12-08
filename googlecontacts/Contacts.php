<?php
class Contacts {
  public function printme() {
    $outstr = "<div class=\"entry\">\n";
    $outstr .= "<div class=\"name\">";
    $outstr .= $this->getName() !== FALSE ? $this->getName() : 'Name not available'; 
    $outstr .= "</div>\n";
    $outstr .= "<div class=\"data\">\n";
    $outstr .= "<table>\n<tr><td>Organization</td><td>";
    $outstr .= $this->getOrgName();
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Email</td><td>";
    $outstr .= @join(', ', $this->getMail());
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Phone</td><td>";
    $outstr .= @join(', ', $this->getPhoneNumber());
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Address</td><td>";
    $outstr .= @join(', ', $this->getAddress());
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Web</td><td>";
    $outstr .= @join(', ', $this->getWebsite());
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Content</td><td>";
    $outstr .= $this->getContent();
    $outstr .= "</td></tr>\n";
    $outstr .= "<tr><td>Id</td><td>";
    $outstr .= (string) $this->getId();
    $outstr .= "</td></tr>\n";
    $outstr .= "</table>\n</div>\n</div>\n\n";
	return $outstr;
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
  public function getId() {
    return FALSE;
  }
  public function getTime() {
    return FALSE;
  }
  public function getBirthday()
    return FALSE;
  }
  public function setName() {
    return FALSE;
  }
}
?>
