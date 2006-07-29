/*
 *
 * ttx.c  --- Main program file for ttx
 *
 */

/*
 * ttx.c
 * Copyright (C) 1995,1996,2004,2005 M.F. van Loon <mfvl@xs4all.nl>,
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
#include "config.h" 

#ifdef HAVE_STDIO_H
#include <stdio.h>
#endif
#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#ifdef HAVE_NETDB_H
#include <netdb.h>
#endif
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif
#ifdef HAVE_STRING_H
#include <string.h>
#endif
#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif
#ifdef HAVE_GETOPT_H
#include <getopt.h>
#endif
#include "sockextr.h"

#ifndef FALSE
# define FALSE (0)
# define TRUE (!FALSE)
#endif

#ifdef WIN32
WORD wVersionRequested;
WSADATA wsaData;
int err;
#endif

#ifdef HAVE_STRNCASECMP
# define STRNICMP strncasecmp
#else
# ifdef HAVE_STRNICMP
#  define STRNICMP strnicmp
# else
#  error "No strnicmp or strncasecmp available"
# endif
#endif

#ifdef HAVE_STRCHR
# define STRCHR strchr
#else
# ifdef HAVE_INDEX
#  define STRCHR index
# else
#  error "No strchr or index avilable"
# endif
#endif

#ifdef COLOR_ENABLE
# define CE "c"
#else
# define CE ""
#endif

#ifdef DEBUG
# define DE "d"
#else
# define DE ""
#endif

char short_options[] = "L" DE CE "hrv";

struct option long_options[] = {
#ifdef COLOR_ENABLE
  {"nocol",0,0,'c'},
#endif
#ifdef DEBUG
  {"debug",0,0,'d'},
#endif
  {"help",0,0,'h'},
  {"less",0,0,'L'},
  {"raw",0,0,'r'},
  {"version",0,0,'v'},
  {NULL,0,0,0}
};

#define TTX_NODE       "teletekst.nos.nl"
#define TTX_PORT      "80"
#define TTX_URL        "/tekst/"

#define ID         "TTX V" VERSION " (C) 1995,1996,2004,2005 M.F. van Loon, 2001-2003 M.J.E. Mol"
#define USER_AGENT "Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 5.0; .NET CLR 1.0.3705)"
#define err_sock_print() perror("Cannot write to net\n")

int    repeat;
int    error = FALSE;
char   hostname[256];
char * port;
char   url[512];
static char buffer[40960];
struct tcp_socket *s;
int    in_header, pagcnt, subpage, in_pre;
int    do_raw   = 0;
#ifdef DEBUG
int    debug    = 0;
#endif
int    do_less  = 0;
char   *c, *d, *e;
char * ttx_node, * ttx_url;
char * ttx_port;
char * progname;
FILE * fout;

#ifdef COLOR_ENABLE

int    do_color = 1;

char *color_table[] = { "black",
			"red", 
			"lime", 
			"yellow", 
			"blue", 
			"fuchsia", 
			"aqua", 
			"white", 
};
# define MAXCOL 8
#endif 

char *strip_cr(char *b)
{
  size_t len = strlen(b);
  
  while (len > 0 && STRCHR("\n\r \t",b[len-1]) != NULL)
    b[--len] = 0; 
  
  return b;
  
} /* strip_cr */

int getpage(const char * pagenum,const int subpage) 
{
  char requeststring[2048];

  strcpy(hostname, ttx_node);
  port = ttx_port;
  sprintf(url, "%s%s-%02d.html", ttx_url, pagenum, subpage);
#ifdef DEBUG
  if (debug>0) 
    fprintf(stderr, "Pagina %s - %d\n", pagenum, subpage);
#endif
  
  do {
    if ((s = make_connection(hostname, port)) == NULL) {
      fprintf(stderr,"Can't connect to: %s, %s\n", hostname, port);
      return 1; 
    }
    sprintf(requeststring,"GET %s HTTP/1.1\r\n",url);
    strcat(requeststring,"Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/msword, application/vnd.ms-powerpoint, application/x-shockwave-flash, */*\r\n");
    strcat(requeststring,"Accept-Language: nl\r\n");
    strcat(requeststring,"Accept-Encoding: gzip, deflate\r\n");
    strcat(requeststring,"User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows 98; Win 9x 4.90)\r\n");
    strcat(requeststring,"Host: ");
    strcat(requeststring,hostname);
    strcat(requeststring,"\r\n");
    strcat(requeststring,"Connection: Keep-Alive\r\n\r\n");
    if (sock_printf(s, requeststring) < 0) {
      err_sock_print();
      return 1;
    }
#ifdef DEBUG
    if (debug>0) 
      fprintf(stderr,"%s",requeststring);
#endif
    
    in_header = TRUE;
    in_pre = FALSE;
    repeat = FALSE;
    memset(buffer, 0, sizeof(buffer));
    while (sgets(buffer, sizeof(buffer) - 1, s) != NULL) {
      strip_cr(buffer);
#ifdef DEBUG
      if (debug >1)
	fprintf(stderr, "%4ld: %s\n", strlen(buffer), buffer);
      else if (debug > 0)
 	fprintf(stderr, "Size = %4ld\n", strlen(buffer));
#endif

      if (in_header) {
	if (buffer[0] == 0)
	  in_header = FALSE;
	else if (STRNICMP(buffer, "HTTP/1.1 4", 10) == 0) {
	  if (STRNICMP(buffer+10,"04",2) !=0) 
	    fprintf(stderr,"%s\n",buffer);
	  else if (subpage==1) 
	    fprintf(stderr,"Pagina %s niet gevonden\n",pagenum); 
	  return 1;
	} else {
	  if (STRNICMP(buffer, "Location:", 9) == 0) {
	    if ((c = strstr(buffer, "http://")) == NULL) {
	      if ((c = STRCHR(buffer, '/')) == NULL) {
		fprintf(stderr, "Bad Location: %s\n", buffer);
		error = TRUE;
 		repeat = FALSE;
		return 1;
	      }
	      strcpy(url, c);
 	    }
 	    else {
 	      c += 7;
 	      d = STRCHR(c, ':');
 	      e = STRCHR(c, '/');
 	      if(d != NULL) {
 		if(d > e && e != NULL)
 		  d = NULL;
 	      }
 	      if(d != NULL) {
 		*d++ = 0;
 		port = d;
 	      }
 	      else
 		port = "80";
 	      if(e != NULL) {
		strcpy(url, e);
 		*e = 0;
 	      }
 	      else
 		*url = 0;
 	      strcpy(hostname, c);
 	    }
#ifdef DEBUG
 	    if (debug>0) 
 	      fprintf(stderr, "Na location: host = %s, port = %s, url = %s\n", hostname, port, url);
#endif
 	    repeat = TRUE;
 	  }
 	}
      }
      else if (do_raw) {
	fprintf(fout,"%s\n", buffer);
      }
      else {
	char * b = buffer;
	if ((c = strstr(b, "<pre>")) != NULL) {
 	  in_pre = TRUE;
 	  b = c + 5;
 	}
 	c = b;
 	if (strstr(b, "</pre>") != NULL) {
 	  in_pre = FALSE;
 	}
 	if (in_pre) {
 	  while ((d = STRCHR(c, '<')) != NULL) {
 	    if ((c = STRCHR(d, '>')) == NULL) {
 	      fprintf(stderr, "error: no matching > found\n");
 	      break;
 	    }
 	    else {
#ifdef COLOR_ENABLE
 	      /*
 	       * Check for COLOR 
 	       */
 	      if (do_color) {
 		if (!STRNICMP(d+1, "FONT COLOR=", 11)) {
 		  char * f;
 		  long int col;
 		  int col2 = '0', foundcol = 0;
 		  for (col2 = 0; col2 < MAXCOL; col2++) {
 		    if (!STRNICMP(d+12, color_table[col2], strlen(color_table[col2]))) {
 		      foundcol = 1;
		      break;
 		    }
		  }
 		  if (!foundcol) {
 		    col = strtol(d+12, &f, 16); 
 		    if (c != f)
 		      fprintf(stderr, "warning: font tag unknown\n");
 		    /* printf("col %06x ", col); */
 		    if (col & 0xff0000) col2 += 1;
 		    if (col & 0x00ff00) col2 += 2;
 		    if (col & 0x0000ff) col2 += 4;
 		  }
 		  /* printf("col %02d%c ", col2, foundcol ? 'a' : 'n'); */
 		  col2 += 0x30;
		  
 		  *d++ = '\e';
 		  *d++ = '[';
 		  *d++ = '3';
 		  *d++ = col2;
 		  *d++ = ';';
 		  *d++ = '1';
 		  *d++ = 'm';
 		}
 		else if (!STRNICMP
			 (d+1, "/FONT>", 6)) {
		  /* printf("col default "); */
		  *d++ = '\e';
		  *d++ = '[';
		  *d++ = '0';
		  *d++ = 'm';
		}
	      }
#endif      
	      memmove(d, ++c, strlen(c)+1);
	      c = d;
	    }
	  }
	  c = b;
	  while ((d = STRCHR(c, '&')) != NULL) {
	    if ((c = STRCHR(d, ';')) == NULL) {
	      fprintf(stderr, "error: no matching ; found\n");
	      break;
	    }
	    else {
	      *c++ = '\0';
	      if (strcmp(d+1, "quot") == 0)
		*d++ = '"';
	      else if (strcmp(d+1, "lt") == 0)
		*d++ = '<';
	      else if (strcmp(d+1, "gt") == 0)
		*d++ = '>';
	      else if (strcmp(d+1, "amp") == 0)
		*d++ = '&';
	      else 
		*d++ = '?';
	      memmove(d, c, strlen(c)+1);
	      c = d;
	    }
	  }
	  fprintf(fout,"%s\n", b);
	}
      }
    }
  } while (repeat);
#ifdef COLOR_ENABLE  
  if (do_color) {
    printf("\e[0m");
  }
#endif  
  return 0;
  
} /* getpage */

void version(void) {
  printf("%s\n", ID);
}

void usage(void)
{
  version();
  printf("\n%s [options] [<pagenumbers>]\n\n", progname);
  printf("Available options:\n");
#ifdef COLOR_ENABLE  
  printf("    -c  --nocol     do not use ansi color codes\n");
#endif
#ifdef DEBUG
  printf("    -d  --debug     increase debug level\n");
#endif
  printf("    -h  --help      this help screen\n");
  printf("    -L  --less      use 'less' for output to screen\n");
  printf("    -r  --raw       do not process, but dump raw page data\n");
  printf("    -v  --version   print version informatio\n\n");
  
  return;
  
} /* usage */

int main(int argc, char **argv)
{
  int rc = 0;
  int c;
  int option_index=0;
  progname = argv[0];


  while (1) {
    c = getopt_long(argc,argv,short_options,long_options,&option_index);


    if (c==-1)
      break;
    
    switch (c) {
    case 'v':
      version();
      exit(0);
      break;
      
    case 'h':
    case '?':
      usage();
      exit(0);
      break;

#ifdef COLOR_ENABLE
    case 'c':
      do_color=0;
      break;
#endif

    case 'L':
      do_less=1;
      break;

    case 'r':
      do_raw=1;
      break;

#ifdef DEBUG      
    case 'd':
      debug++;
      break;
#endif
    }
  }

#ifdef WIN32
wVersionRequested = MAKEWORD( 1, 1 );

err = WSAStartup( wVersionRequested, &wsaData );
if ( err != 0 ) {
    /* Tell the user that we couldn't find a useable */
    /* winsock.dll.                                  */
  fprintf(stderr,"Foute versie van Winsock\n");
    return 4;
}
#endif
  
  //  fprintf(stderr, "%s\n", ID);
  
  if (!(ttx_node = getenv("TTX_NODE")))
    ttx_node = TTX_NODE;
  if (!(ttx_port = getenv("TTX_PORT")))
    ttx_port = TTX_PORT;
  if (!(ttx_url = getenv("TTX_URL")))
    ttx_url = TTX_URL;

  if (do_less) {
    fout = popen("less","w");
#ifdef COLOR_ENABLE
    do_color=0;
#endif
  } else {
    fout = stdout;
  }
  
  if(optind == argc) {
    for (subpage = 1; getpage("100", subpage); subpage++);
  }
  else {
    for (pagcnt = optind; pagcnt < argc; pagcnt++) {
      rc = 0;
      for (subpage = 1; rc == 0; subpage++) {
	rc  = getpage(argv[pagcnt], subpage);
#ifdef DEBUG
	if (debug>0)
	  fprintf(stderr,"getting subpage %d rc %d\n", subpage, rc);
#endif
      }
    }
  }
  
  close_socket(s);

  if (do_less)
    pclose(fout);
  return(0);
} /* main */

