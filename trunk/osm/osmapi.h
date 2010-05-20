#include "config.h"
#include <HTTPSocket.h>
#include <stdbool.h>
#include <string>

namespace osmapi {
  class osmapiSocket : public HTTPSocket
  {
  public:
    osmapiSocket(ISocketHandler& h,const std::string apistr);
    void OnConnect();
    void OnFirst();
    void OnDisconnect();
    void OnHeader(const std::string& key, const std::string& value);
    void OnHeaderComplete();
    void OnData(const char *buf, size_t sz);
    void OnDelete();
    inline std::string GetData() { return m_buf;}
    
  private:
    std::string m_host;
    int m_port;
    std::string m_apistr;
    bool m_first;
    size_t m_sz;
    std::string m_buf;
  };
}
