<?php


print '<hr>';

for($i=0,$texts = count($_REQUEST['text']); $i<$texts;$i++)
        print 'Text #'.$i.': '.$_REQUEST['text'][$i].'<br>';


for($i=0,$files = count($_FILES['userfile']['name']); $i<$files;$i++) 
        {
         if (is_uploaded_file($_FILES['userfile']['tmp_name'][$i])) 
                {
                        print "<br><br>File ". $_FILES['userfile']['name'][$i] ." uploaded successfully.\n<hr>";
                        print "Displaying contents<br>";
                        print nl2br(file_get_contents($_FILES['userfile']['tmp_name'][$i]));
                } 
        }

        
?>
