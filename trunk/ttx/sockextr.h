#ifndef _SOCKET_EXTRA_H
#define _SOCKET_EXTRA_H

#include "config.h"
#include <stdarg.h>
#include <stdlib.h>
#include <sys/param.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#ifndef PARAMS
# if PROTOTYPES
#  define PARAMS(protos) protos
# else /* no PROTOTYPES */
#  define PARAMS(protos) ()
# endif /* no PROTOTYPES */
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

struct tcp_socket *make_connection PARAMS ((const char *host,const char *service));
void close_socket PARAMS ((struct tcp_socket *s));
int sock_printf PARAMS ((struct tcp_socket *sock,char *f,...));
char *sgets PARAMS ((char *buf,int n,struct tcp_socket *sock));
char *localhost PARAMS ((void));

char *sock_version PARAMS ((void));

#ifndef INADDR_NONE
#define INADDR_NONE             0xffffffff
#endif

#endif
