#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <errno.h>
#ifdef __MT__
#define INCL_DOS
#include <os2.h>
#endif
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include "sockextr.h"

#define SOCK_VERSION "1.05 ("__DATE__"-"__TIME__") (C) 1995-1998 M.F. van Loon"

static char *localname = NULL;
static char *localtmp = NULL;

#ifdef __MT__
static HMTX tcp_share;
static int tcp_share_set = 0;
#endif

static void 
reserve_tcp (void)
{
#ifdef __MT__
  if (!tcp_share_set)
    {
      DosCreateMutexSem (NULL, &tcp_share, 0, TRUE);
      tcp_share_set = 1;
    }
  DosRequestMutexSem (tcp_share, SEM_INDEFINITE_WAIT);
#endif
}

static void 
release_tcp (void)
{
#ifdef __MT__
  DosReleaseMutexSem (tcp_share);
#endif
}

#ifndef HAVE_SOCK_INIT
int 
sock_init (void)
{
#ifdef __MT__
  if (!tcp_share_set)
    {
      DosCreateMutexSem (NULL, &tcp_share, 0, FALSE);
      tcp_share_set = 1;
    }
#endif
  return 0;
}
#endif

char *
localhost (void)
{
  if (localname == NULL)
    {
      if (localtmp == NULL)
	{
	  char tmp[2048];

	  reserve_tcp ();
	  if (gethostname (tmp, sizeof (tmp)) == 0)
	    localtmp = strdup (tmp);
	  else
	    localtmp = "localhost.error";
	  release_tcp ();
	}
      return localtmp;
    }
  else
    return localname;
}

void 
close_socket (struct tcp_socket *s)
{
  if (s != NULL)
    {
      soclose (s->socket);
      free (s);
    }
}

struct tcp_socket *
make_connection (const char *host, const char *service)
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

  if ((ipa = inet_addr (host)) == INADDR_NONE)
    {
      reserve_tcp ();
      phe = gethostbyname (host);
      if (phe != NULL)
	ipa = *(unsigned long *) phe->h_addr;
      else
	{
	  release_tcp ();
	  fprintf (stderr, "gethostbyname cannot find host\n");
	  return NULL;
	}
      release_tcp ();
    }
  else
    {
      reserve_tcp ();
      phe = gethostbyaddr ((char *) &ipa, 4, PF_INET);
      release_tcp ();
      if (phe == NULL)
	{
	  perror ("gethostbyadd cannot find address");
	  return NULL;
	}
    }

  if ((portnum = atoi (service)) == 0)
    {
      reserve_tcp ();
      port = getservbyname (service, "tcp");
      if (port == NULL)
	{
	  release_tcp ();
	  fprintf (stderr, "Service %s unknown\nCheck 'services' file\n", service);
	  return NULL;
	}
      portnum = htons (port->s_port);
      release_tcp ();
    }

  if ((s = socket (AF_INET, SOCK_STREAM, 0)) <= 0)
    {
      perror ("socket");
      return NULL;
    }

  memset (&server, 0, sizeof (server));
  server.sin_family = AF_INET;
  server.sin_port = ntohs (portnum);
  server.sin_addr.s_addr = ipa;

  if (connect (s, (struct sockaddr *) &server, sizeof (server)) == -1)
    {
      perror ("connect");
      soclose (s);
      return NULL;
    }
  if ((tc = (struct tcp_socket*)calloc (1, sizeof (struct tcp_socket))) != NULL)
    {
      tc->socket = s;
      tc->remote = strdup (phe->h_name);
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
	    }
	  ghn;
	  unsigned int len = sizeof (ghn);

	  memset (&ghn, 0, sizeof (ghn));
	  if (getsockname (s, (struct sockaddr *) &ghn, &len) == 0)
	    {
/*        int i;

   for(i=0;i<len;i++)
   fprintf(stderr,"%2.2x ",(int)(((char *)&ghn)[i]));
   fprintf(stderr,"\n"); */
	      reserve_tcp ();
	      phe = gethostbyaddr ((char *) (&(ghn.ipaddr)), 4, PF_INET);
/*        fprintf(stderr,"%s\n",phe->h_name); */
	      localname = strdup (phe->h_name);
	      release_tcp ();
	    }
	  else
	    perror ("Error after getsockname");
	}
    }
  else
    soclose (s);
  return tc;
}

int 
sock_printf (struct tcp_socket *sock, char *f,...)
{
  va_list argp;
  char msg[4096];
  int s;
  int retval = -1;		/* assume failure */

  if (sock == NULL)
    return retval;

  s = sock->socket;

  va_start (argp, f);
  vsprintf (msg, f, argp);
  va_end (argp);

  if (msg[strlen (msg) - 1] != '\n')
    strcat (msg, "\n");

  if ((send (s, msg, strlen (msg), 0)) == -1)
    perror ("sock_printf");
  else
    retval = strlen (msg);

  return retval;
}

static void 
fill_buffer (struct tcp_socket *sock)
{
  int count = TCP_BUFFER / 2;
  int read;

  if (sock == NULL)
    return;

  if ((TCP_BUFFER - sock->in) < count)
    count = TCP_BUFFER - sock->in;

  if ((read = recv (sock->socket, sock->recvbuf + sock->in, count, 0)) < 0)
    {
      perror ("Fill buffer failed");
      return;
    }
  sock->in += read;
  if (sock->in == TCP_BUFFER)
    sock->in = 0;
}

static int 
get_byte_from_socket (struct tcp_socket *sock)
{
  int i;

  if (sock == NULL)
    return -1;

  if (sock->in == sock->out)
    fill_buffer (sock);
  if (sock->in == sock->out)
    return -1;
  i = sock->recvbuf[sock->out++];
  if (sock->out == TCP_BUFFER)
    sock->out = 0;
  return i;
}

int 
recv_buf (struct tcp_socket *sock, unsigned char *buf, int n)
{
  int rd = 0, c;

  while (rd < n)
    {
      if ((c = get_byte_from_socket (sock)) < 0)
	return rd;
      *buf++ = c;
      rd++;
    }
  return rd;
}

unsigned char *
sgets (unsigned char *buf, int n, struct tcp_socket *sock)
{
  unsigned char *b = buf;
  int i, c;

  if (buf == NULL || sock == NULL)
    return NULL;

  for (i = 0; i < n - 1; i++)
    {
      if ((c = get_byte_from_socket (sock)) < 0)
	{
	  if (b != buf)
	    break;
	  else
	    return NULL;
	}

      *b++ = c;
      if (c == '\n')
	break;
    }
  *b++ = 0;
  return b;
}

char *
sock_version (void)
{
  return SOCK_VERSION;
}
