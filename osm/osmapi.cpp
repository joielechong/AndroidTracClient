#include "osmapi.h"
#include <SocketHandler.h>
#include <StdoutLog.h>
#include <stdbool.h>
#include <iostream>

namespace osmapi {
  using namespace std;
  
  osmapiSocket::osmapiSocket(ISocketHandler& h,const std::string apistr) : HTTPSocket(h)
									 , m_host("api.openstreetmap.org")
									 , m_port(80)
									 , m_apistr(apistr)
									 , m_first(false)
									 , m_sz(0)
									 , m_buf("") {
    Open(m_host, m_port);
  }
  
  osmapiSocket::osmapiSocket(ISocketHandler& h,const std::string apistr,const std::string host) : HTTPSocket(h)
									 , m_host(host)
									 , m_port(80)
									 , m_apistr(apistr)
									 , m_first(false)
									 , m_sz(0)
									 , m_buf("") {
    cerr << "Host = " << m_host << " apistr = " << m_apistr << endl;
    Open(m_host, m_port);
  }
  
  void osmapiSocket::OnConnect() {
    Send("GET /api/0.6/" + m_apistr + " HTTP/1.0\r\n");
    Send("Host: " + m_host + "\r\n");
    Send("\r\n");
  }
  
  void osmapiSocket::OnFirst() {
  }
  
  void osmapiSocket::OnDisconnect() { // or OnDelete()
  }
  
  void osmapiSocket::OnHeader(const std::string& key, const std::string& value) {
    //    fprintf(stderr, "%s: %s\n", key.c_str(), value.c_str());
  }
  
  void osmapiSocket::OnHeaderComplete() {
    //    fprintf(stderr, "\n");
  }
  
  void osmapiSocket::OnData(const char *buf, size_t sz) {
    //    printf(" %d bytes\n", sz);
    if (1||!m_first)
      {
	std::string str = buf;
	str.resize(sz);
	m_buf += str;
	m_first = true;
      }
    m_sz += sz;
  }
  
  void osmapiSocket::OnDelete() {
    //    fprintf(stderr, "Content length: %d\n", m_sz);
  }
}
#ifdef TESTING  
int main(int argc, char *argv[])
{
  std::string apistr = argc > 1 ? argv[1] : "way/75146";
  StdoutLog log;
  SocketHandler h(&log);
  osmapiSocket sock(h, apistr);
  h.Add(&sock);
  while (h.GetCount())
    {
      h.Select(1, 0);
    }
}
#endif
