<!DOCTYPE html 
  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  <head>
    <title>Listing contacts</title>
    <style>
    body {
      font-family: Verdana;      
    }
    div.name {
      color: red; 
      text-decoration: none;
      font-weight: bolder;  
    }
    div.entry {
      display: inline;
      float: left;
      width: 400px;
      height: 150px;
      border: 2px solid; 
      margin: 10px;
      padding: 5px;
    }
    td {
      vertical-align: top;
    }
    </style>    
  </head>
  <body>
     
    <?php
    // load Zend Gdata libraries
    require_once 'Zend/Loader.php';
    Zend_Loader::loadClass('Zend_Gdata');
    Zend_Loader::loadClass('Zend_Gdata_ClientLogin');
    Zend_Loader::loadClass('Zend_Http_Client');
    Zend_Loader::loadClass('Zend_Gdata_Query');
    Zend_Loader::loadClass('Zend_Gdata_Feed');
    
    // set credentials for ClientLogin authentication
    $user = "mfvloon@gmail.com";
    $pass = "mikel02";
    
    try {
      // perform login and set protocol version to 3.0
      $client = Zend_Gdata_ClientLogin::getHttpClient(
        $user, $pass, 'cp');
      $gdata = new Zend_Gdata($client);
      $gdata->setMajorProtocolVersion(3);
      
      // perform query and get result feed
      $query = new Zend_Gdata_Query(
        'http://www.google.com/m8/feeds/contacts/default/full');
      $feed = $gdata->getFeed($query);
      
      // display title and result count
      ?>
      
      <h2><?php echo $feed->title; ?></h2>
      <div>
      <?php echo $feed->totalResults; ?> contact(s) found.
      </div>
      
      <?php
      // parse feed and extract contact information
      // into simpler objects
      $results = array();
      foreach($feed as $entry){
        $xml = simplexml_load_string($entry->getXML());
        $obj = new stdClass;
        $obj->name = (string) $entry->title;
        $obj->orgName = (string) $xml->organization->orgName; 
        $obj->orgTitle = (string) $xml->organization->orgTitle; 
      
        foreach ($xml->email as $e) {
          $obj->emailAddress[] = (string) $e['address'];
        }
        
        foreach ($xml->phoneNumber as $p) {
          $obj->phoneNumber[] = (string) $p;
        }
        foreach ($xml->website as $w) {
          $obj->website[] = (string) $w['href'];
        }
        
        $results[] = $obj;  
      }
    } catch (Exception $e) {
      die('ERROR:' . $e->getMessage());  
    }
    ?>
    
    <?php
    // display results
    foreach ($results as $r) {
    ?>
    <div class="entry">
      <div class="name"><?php echo (!empty($r->name)) ? 
       $r->name : 'Name not available'; ?></div>
      <div class="data">
        <table>
          <tr>
            <td>Organization</td>
            <td><?php echo $r->orgName; ?></td>
          </tr>
          <tr>
            <td>Email</td>
            <td><?php echo @join(', ', $r->emailAddress); ?></td>
          </tr>
          <tr>
            <td>Phone</td>
            <td><?php echo @join(', ', $r->phoneNumber); ?></td>
          </tr>
          <tr>
            <td>Web</td>
            <td><?php echo @join(', ', $r->website); ?></td>
          </tr>
        </table>
      </div>
    </div>
    <?php
    }
    ?>

  </body>
</html>
