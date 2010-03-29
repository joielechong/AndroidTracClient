#include "osm.h"
#include <string.h>

class InterpolatedAddresses  {
 public:
  InterpolatedAddresses(osm::Way &w);

  void createAddresses();

 private:
  std::string _method;
  std::string _first;
  std::string _last;
};

