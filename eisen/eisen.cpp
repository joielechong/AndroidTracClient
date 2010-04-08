#include <cstring>
#include <glibmm/ustring.h>
#include <iostream>
#include <stdexcept>
#include "eisparser.h"

using namespace std;

int main(int argc, char* argv[])
{
  cout <<"<HTML><HEAD><TITLE>Eisen uit DBFM</TITLE</HEAD><BODY>"<<endl;
  for (int i=1;i<argc;i++) {
    string filepath = argv[i];
    cout << "<H1>"+filepath+"</H1>"<<endl;
    try {
      // Parse the entire document in one go:
      eisparser::EisenParser parser;
      parser.parse_file(filepath);
    } catch(const xmlpp::exception& ex) {
      cout << "libxml++ exception: " << ex.what() << endl;
    } catch (const Glib::ustring &ex) {
      cout << "Exception in parser: " << ex <<endl;
    } catch (const std::exception *ex) {
      cout << "Exception in program: " << ex->what() <<endl;
    }
  }
  cout << "</BODY></HTML>"<<endl;
  return 0;
}
