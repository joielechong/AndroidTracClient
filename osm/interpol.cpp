#include "interpol.h"
#include <stdexcept>

InterpolatedAddresses::InterpolatedAddresses(osm::Way &w) {
  _method = w["addr:interpolation"] == "";
  if (_method == "")
    throw std::domain_error("Verkeerd type weg");
  
}
