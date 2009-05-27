<?php
$depth = array();

function startElement($parser, $name, $attrs)
{
  global $depth;
  for ($i = 0; $i < $depth[$parser]; $i++) {
    echo "  ";
  }
  echo "$name\n";
  $depth[$parser]++;
}

function endElement($parser, $name)
{
  global $depth;
  $depth[$parser]--;
}

function parseit($data) 
{
  echo "<pre>\n";
  $xml_parser = xml_parser_create();
  xml_set_element_handler($xml_parser, "startElement", "endElement");
  if (!xml_parse($xml_parser, $data, true)) {
    die(sprintf("XML error: %s at line %d",
		xml_error_string(xml_get_error_code($xml_parser)),
		xml_get_current_line_number($xml_parser)));
  }
  xml_parser_free($xml_parser);
  echo "</pre>\n";
}
?>
