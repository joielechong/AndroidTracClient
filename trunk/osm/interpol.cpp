#include "osm.h"
#include <stdexcept>

namespace osm {
  using namespace std;

  void Map::InterpolatedAddresses(osm::Way &w) {
    string method = w["addr:interpolation"];
    if (method == "")
      throw std::domain_error("Geen geldige intepolatiemethode opgegeven");
  }
}
