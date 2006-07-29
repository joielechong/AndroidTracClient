#ifndef _SOCKET_EXTRA_H
#define _SOCKET_EXTRA_H

#include "config.h"
#ifdef HAVE_STDARG_H
#include <stdarg.h>
#endif
#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif
#ifdef HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif
#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#ifdef HAVE_NETINET_IN_H
#include <netinet/in.h>
#endif
#ifdef HAVE_SYS_SOCKET_H
#include <sys/socket.h>
#endif
#ifdef HAVE_ARPA_INET_H
#include <arpa/inet.h>
#endif
#ifdef HAVE_WINSOCK2_H
#include <winsock2.h>
#else
#ifdef HAVE_WINSOCK_H
#include <winsock.h>
#endif
#endif

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
