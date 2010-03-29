#include "osm_db.h"
#include <string>
#include <stdexcept>
#include <cstring>

namespace osm_db {  
  osm_db_error::osm_db_error(const char *format,...)
  {
    const int buffsz = static_cast<int>( std::max( (size_t) 2048, strlen(format) * 2 ) );
    std::vector<char> buffer( buffsz, '\0' );
    va_list vargs;
    va_start ( vargs, format );
    int size = vsnprintf(&buffer[0], buffsz, format, vargs);
    va_end( vargs );
    if (size > (buffsz-1))
      {
	// replace tail of msg with "..."
	size = buffsz-1;
	for( int i = buffsz-4; i < buffsz-1; ++i )
	  {
	    buffer[i] = '.';
	  }
      }
    buffer[size] = '\0';
    this->m_what = std::string( &buffer[0], &buffer[0]+size );
  }

  osm_db_error::~osm_db_error() throw() {}
  
  char const * osm_db_error::what() const throw()
  {
    return this->m_what.c_str();
  }  
}
