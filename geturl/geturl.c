#undef  DEBUG
#undef  LOCAL
#undef  DEBUG_SMALL

#ifdef DEBUG
#define DEBUG_SMALL
#endif

#define VERSIE "2.4.3"

#include <stdio.h>
#include <sys/types.h>
#include <netdb.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include "sockextr.h"

#ifndef FALSE
#define FALSE (0)
#define TRUE (!FALSE)
#endif

#ifndef __EMX__
#define stricmp strcasecmp
#define strnicmp strncasecmp
#endif

#define USER_AGENT "User-Agent: Lynx"

int repeat;
int error = FALSE;
char hostname[256];
short port;
char url[512];

#define err_sock_print() perror("Cannot write to net\n")

static char buffer[40960];

char *strip_cr(char *b)
{
  size_t len = strlen(b);

  while (len > 0 && strchr("\n\r \t\b",b[len-1]) != NULL)
    b[--len] = 0; 
  return b;
}


int main(int argc,char *argv[])
{
  struct tcp_socket *s;
  int in_header,show_header=0;
  char *c,*d,*e;

  sock_init();
  strcpy(hostname,argv[1]);
  port = atol(argv[2]);
  sprintf(url,"%s",argv[3]);

  if (port < 0) {
    port = -port;
    show_header=1;
  }

#ifdef DEBUG_SMALL
  fprintf(stderr,"host=%s, port=%d, url = %s\n",hostname,port,url);
#endif
  do {
    if ((s = make_connection(hostname,"80")) == NULL) {
      fprintf(stderr,"Can't connect to: %s\n",hostname);
      return 1; 
    }
    if (sock_printf(s,"GET %s HTTP/1.0",url) < 0) {
      err_sock_print();
      return 1;
    }
    if (sock_printf(s,"%s",USER_AGENT) < 0) {
      err_sock_print();
      return 1;
    }
    if (sock_printf(s,"") < 0) {
      err_sock_print();
      return 1;
    }
#ifdef DEBUG_SMALL
    fprintf(stderr,"GET %s HTTP/1.0\n",url);
    fprintf(stderr,"%s\n",USER_AGENT);  
    fprintf(stderr,"\n");
#endif
    
    in_header = TRUE;
    repeat = FALSE;
    memset(buffer,0,sizeof(buffer));
    for (;;) {
      if (in_header) {
	if (sgets(buffer,sizeof(buffer)-1,s) == NULL)
	  break;
	strip_cr(buffer);
#if defined( DEBUG)
	fprintf(stderr,"%4ld: %s\n",strlen(buffer),buffer);
#elif defined(DEBUG_SMALL)
	fprintf(stderr,"Size = %4ld\n",strlen(buffer));
#endif
	if (show_header)
	  printf("%s\n",buffer);

	if (buffer[0] == 0)
	  in_header = FALSE;
	else {
	  if(strnicmp(buffer,"Location:",9) == 0) {
	    if((c = strstr(buffer,"http://")) == NULL) {
	      if ((c = strchr(buffer,'/')) == NULL) {
		fprintf(stderr,"Bad Location: %s\n",buffer);
		error = TRUE;
		repeat = FALSE;
		return 1;
	      }
	      strcpy(url,c);
	    }
	    else {
	      c += 7;
	      d = strchr(c,':');
	      e = strchr(c,'/');
	      if(d != NULL) {
		if(d>e && e != NULL)
		  d = NULL;
	      }
	      if(d != NULL) {
		*d++ = 0;
		port = atoi(d);
	      }
	      else
		port = 80;
	      if(e != NULL) {
		strcpy(url,e);
		*e = 0;
	      }
	      else
		*url = 0;
	      strcpy(hostname,c);
	    }
#ifdef DEBUG_SMALL
	    fprintf(stderr,"Na location: host = %s, url = %s\n",hostname,url);
#endif
	    repeat = TRUE;
            break;
	  }
	}
      }
      else {
//	  for(d=buffer;*d!=0;d++)
//	    if (*d == '\r') *d = '\n';
//	  printf("%s\r\n",buffer);
	int count=sizeof(buffer);
	int rd;
	
        rd=recv_buf(s,buffer,count);
#ifdef DEBUG_SMALL
        fprintf(stderr,"Na recv_buf(%d) rd = %d\n",count,rd);
#endif

	if (rd < 0) {
	  perror("Read from network failed");
	  break;
	}
	if (rd == 0)
	  break;
	write(fileno(stdout),buffer,rd);
      }
      fflush(stderr);
      fflush(stdout);
    }
    close_socket(s);
  } while (repeat);
  return 0;
}
