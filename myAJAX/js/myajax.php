<?php
ob_start ("ob_gzhandler");
header("Content-type: text/javascript; charset: UTF-8");
header("Content-Encoding: gzip");
//header("Cache-Control: must-revalidate");
//header("Expires: " .gmdate("D, d M Y H:i:s",time() + 259200) . " GMT");

readfile('myajax.js');
ob_flush();
?>