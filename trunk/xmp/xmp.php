<?php


$xmp_parsed = ee_extract_exif_from_pscs_xmp ("xx.jpg",1);

function ee_extract_exif_from_pscs_xmp ($filename,$printout=0) {
    
    // very straightforward one-purpose utility function which
    // reads image data and gets some EXIF data (what I needed) out from its XMP tags (by Adobe Photoshop CS)
    // returns an array with values
    // code by Pekka Saarinen http://photography-on-the.net
    
    ob_start();
    readfile($filename);
    $source = ob_get_contents();
    ob_end_clean();
    
    $xmpdata_start = strpos($source,"<x:xmpmeta");
    $xmpdata_end = strpos($source,"</x:xmpmeta>");
    $xmplenght = $xmpdata_end-$xmpdata_start;
    $xmpdata = substr($source,$xmpdata_start,$xmplenght+12);

    echo $xmpdata."\n";

    $xmp_parsed = array();
    
    $regexps = array(
    array("name" => "IPTC4XMPCore Location", "regexp" => "/<Iptc4xmpCore:Location>.+<\/Iptc4xmpCore:Location>/"),
    array("name" => "DC creator", "regexp" => "/<dc:creator>\s*<rdf:Seq>\s*<rdf:li>.+<\/rdf:li>\s*<\/rdf:Seq>\s*<\/dc:creator>/"),
    array("name" => "TIFF camera model", "regexp" => "/<tiff:Model>.+<\/tiff:Model>/"),
    array("name" => "TIFF maker", "regexp" => "/<tiff:Make>.+<\/tiff:Make>/"),
    array("name" => "EXIF exposure time", "regexp" => "/<exif:ExposureTime>.+<\/exif:ExposureTime>/"),
    array("name" => "EXIF f number", "regexp" => "/<exif:FNumber>.+<\/exif:FNumber>/"),
    array("name" => "EXIF aperture value", "regexp" => "/<exif:ApertureValue>.+<\/exif:ApertureValue>/"),
    array("name" => "EXIF exposure program", "regexp" => "/<exif:ExposureProgram>.+<\/exif:ExposureProgram>/"),
    array("name" => "EXIF iso speed ratings", "regexp" => "/<exif:ISOSpeedRatings>\s*<rdf:Seq>\s*<rdf:li>.+<\/rdf:li>\s*<\/rdf:Seq>\s*<\/exif:ISOSpeedRatings>/"),
    array("name" => "EXIF datetime original", "regexp" => "/<exif:DateTimeOriginal>.+<\/exif:DateTimeOriginal>/"),
    array("name" => "EXIF exposure bias value", "regexp" => "/<exif:ExposureBiasValue>.+<\/exif:ExposureBiasValue>/"),
    array("name" => "EXIF metering mode", "regexp" => "/<exif:MeteringMode>.+<\/exif:MeteringMode>/"),
    array("name" => "EXIF focal lenght", "regexp" => "/<exif:FocalLength>.+<\/exif:FocalLength>/"),
    array("name" => "AUX lens", "regexp" => "/<aux:Lens>.+<\/aux:Lens>/")
    );
    
    foreach ($regexps as $key => $k) {
            $name         = $k["name"];
            $regexp     = $k["regexp"];
            unset($r);
            preg_match ($regexp, $xmpdata, $r);
            $xmp_item = "";
            $xmp_item = @$r[0];
            array_push($xmp_parsed,array("item" => $name, "value" => $xmp_item));
    }
    
    if ($printout == 1) {
        foreach ($xmp_parsed as $key => $k) {
                $item         = $k["item"];
                $value         = $k["value"];
                print "<br><b>" . $item . ":</b> " . $value. "\n";
        }
    }

    require 'xmpxml.php';
    parseit('<?xml version="1.0" encoding="UTF-8"?>'.$xmpdata);
    
return ($xmp_parsed);

}

?>
