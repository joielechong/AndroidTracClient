/*
 * sockextr.cpp
 *
 * Copyright (C) 1995,1996,2004 M.F. van Loon <michiel@van-loon.xs4all.nl>,
 * Copyright (C) 2001-2003 Marcel .J.E. Mol <marcel@mesa.nl>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <errno.h>

#include "sockextr.h"

static char *localname = NULL;
static char *localtmp = NULL;

char *localhost(void)
{
  if (localname == NULL)
  {
    if(localtmp == NULL)
    {
      char tmp[2048];

      if(gethostname(tmp,sizeof(tmp)) == 0)
        localtmp = strdup(tmp);
      else
        localtmp = "localhost.error";
    }
    return localtmp;
  }
  else
    return localname;
}

void close_socket(struct tcp_socket *s)
{
  if( s != NULL)
  {
    close(s->socket);
    free(s);
  }
}

struct tcp_socket *make_connection(const char *host,const char *service)
{
  unsigned long ipa;
  int s;
  struct hostent *phe;
  struct sockaddr_in server;
  struct servent *port;
  unsigned short portnum;
  struct tcp_socket *tc;

  if (host == NULL || service == NULL)
    return NULL;

  if((ipa = inet_addr(host)) == INADDR_NONE)
  {
    phe = gethostbyname(host);
    if (phe != NULL)
      ipa = *(unsigned long *)phe->h_addr;
    else
    {
      fprintf(stderr,"gethostbyname cannot find host\n");
      return NULL;
    }
  }
  else
  {
    phe = gethostbyaddr((char *)&ipa,4,PF_INET);
    if (phe == NULL)
    {
      perror("gethostbyaddr cannot find address");
      return NULL;
    }
  }

  if((portnum = atoi(service)) == 0)
  {
    port = getservbyname(service,"tcp");
    if (port == NULL)
    {
      fprintf(stderr,"Service %s unknown\nCheck 'services' file\n",service);
      return NULL;
    }
    portnum = htons(port->s_port);
  }

  if((s = socket(AF_INET, SOCK_STREAM,0)) <= 0)
  {
    perror("socket");
    return NULL;
  }

  memset(&server,0,sizeof(server));
  server.sin_family = AF_INET;
  server.sin_port   = ntohs(portnum);
  server.sin_addr.s_addr   = ipa;

  if(connect(s,(struct sockaddr *)&server,sizeof(server)) == -1)
  {
    perror("connect");
    close(s);
    return NULL;
  }
  if((tc = (struct tcp_socket *)calloc(1,sizeof(struct tcp_socket))) != NULL)
  {
    tc->socket = s;
    tc->remote = strdup(phe->h_name);
    tc->portnum = portnum;
    tc->in = tc->out = 0;
    if (localname == NULL)
    {
      struct sockaddr_1
      { 
        u_short fam;
        u_short undef;
        unsigned long ipaddr;
        char zero[6];
      } ghn;
      socklen_t len = sizeof(ghn);

      memset(&ghn,0,sizeof(ghn));
      if (getsockname(s,(struct sockaddr *)&ghn,&len) == 0)
      {
/*        int i;

        for(i=0;i<len;i++)
          fprintf(stderr,"%2.2x ",(int)(((char *)&ghn)[i]));
        fprintf(stderr,"\n"); */
        phe = gethostbyaddr((char *)(&(ghn.ipaddr)),4,PF_INET);
/*        fprintf(stderr,"%s\n",phe->h_name);*/
        localname = strdup(phe->h_name);
      }
      else
        perror("Error after getsockname");
    }
  }
  else
    close(s);
  return tc;
}

int sock_printf(struct tcp_socket *sock,char *f,...)
{
  va_list argp;
  char msg[4096];
  int s;
  int retval = -1;  /* assume failure */

  if(sock == NULL)
    return retval;

  s = sock->socket;

  va_start(argp,f);
  vsprintf(msg,f,argp);
  va_end(argp);

  if(msg[strlen(msg)-1] != '\n')
    strcat(msg,"\n");

  if((send(s,msg,strlen(msg),0)) == -1)
    perror("sock_printf");
  else
    retval = strlen(msg);

  return retval;
}

static void fill_buffer(struct tcp_socket *sock)
{
  int count = TCP_BUFFER/2;
  int read;

  if (sock == NULL)
    return;

  if((TCP_BUFFER - sock->in) < count)
    count = TCP_BUFFER - sock->in;

  if((read = recv(sock->socket,sock->recvbuf+sock->in,count,0)) < 0)
  {
    perror("Fill buffer failed");
    return;
  }
  sock->in += read;
  if(sock->in == TCP_BUFFER)
    sock->in = 0;
}

static int get_byte_from_socket(struct tcp_socket *sock)
{
  int i;

  if (sock == NULL)
    return -1;

  if(sock->in == sock->out)
    fill_buffer(sock);
  if(sock->in == sock->out)
    return -1;
  i = sock->recvbuf[sock->out++];
  if(sock->out == TCP_BUFFER)
    sock->out = 0;
  return i;
}

char *sgets(char *buf,int n,struct tcp_socket *sock)
{
  char *b=buf;
  int i,c;

  if (buf == NULL || sock == NULL)
    return NULL;

  for(i=0;i<n-1;i++)
  {
    if((c = get_byte_from_socket(sock)) < 0)
    {
      if(b != buf)
        break;
      else
        return NULL;
    }

    *b++ = c;
    if(c == '\n')
      break;
  }
  *b++ = 0;
  return b;
}

#define SE_VERSION "1.04 ("__DATE__"-"__TIME__") (C) 1995,1996 M.F. van Loon"
char *sock_version(void)
{
  return SE_VERSION;
}
