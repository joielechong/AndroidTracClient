#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <fstream>
#include <iostream>

#include "myparser.h"

#define BUFFERSIZE (1024)

int
main(int argc, char* argv[])
{
  std::string filepath;
  if(argc > 1 )
    filepath = argv[1]; //Allow the user to specify a different XML file to parse.
  else
    filepath = "example.xml";
  
  // Parse the entire document in one go:
  try
    {
      MySaxParser parser;
      //      parser.set_substitute_entities(true); //
	  if (filepath == "-") {
	    parser.parse_stream(cin);
	  } else {
		parser.parse_file(filepath);
	  }
    }
  catch(const xmlpp::exception& ex)
    {
      std::cout << "libxml++ exception: " << ex.what() << std::endl;
    }
  
  
  // Demonstrate incremental parsing, sometimes useful for network connections:
  {
    //std::cout << "Incremental SAX Parser:" << std:endl;
    
    std::ifstream is(filepath.c_str());
    char buffer[BUFFERSIZE];
    
    MySaxParser parser;
    do {
      is.read(buffer, (BUFFERSIZE-1));
      Glib::ustring input(buffer, is.gcount());
      
      parser.parse_chunk(input);
    }
    while(is);
    
    parser.finish_chunk_parsing();
  }
  
  
  return 0;
}
