#ifndef _SOCKET_EXTRA_H
#define _SOCKET_EXTRA_H

#include <stdarg.h>
#include <stdlib.h>
#include <sys/param.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef TCP_BUFFER
#define TCP_BUFFER 10240
#endif

struct tcp_socket
{
  int socket;
  unsigned char recvbuf[TCP_BUFFER];
  int in,out;
  char *remote;
  unsigned short portnum;
};

struct tcp_socket *make_connection(const char *host,const char *service);
void close_socket(struct tcp_socket *s);
int sock_printf(struct tcp_socket *sock,char *f,...);
unsigned char *sgets(unsigned char *buf,int n,struct tcp_socket *sock);
char *localhost(void);
int recv_buf(struct tcp_socket *sock,unsigned char *buf,int n);

#ifndef HAVE_SOCK_INIT
int sock_init(void);
#endif

#ifndef HAVE_SOCLOSE
#define soclose(s) close(s)
#endif

#ifndef HAVE_INET_ADDR
unsigned long inet_addr (const char *);
#endif

#ifndef HAVE_INET_NTOA
char *inet_ntoa (unsigned long);
#endif

char *sock_version(void);

#ifdef __cplusplus
}
#endif

#ifndef INADDR_NONE
#define INADDR_NONE             0xffffffff
#endif

#endif
