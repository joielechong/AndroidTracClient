/*
 * htget.c
 *
 * Copyright (C) 1995,1996 M.F. van Loon <michiel@van-loon.xs4all.nl>,
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

#undef  DEBUG
#undef  LOCAL
#undef  DEBUG_SMALL

#ifdef DEBUG
# define DEBUG_SMALL
#endif

#include "config.h"
#include <stdio.h>
#include <sys/types.h>
#include <netdb.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include "sockextr.h"

#ifndef FALSE
# define FALSE (0)
# define TRUE (!FALSE)
#endif

#ifdef HAVE_STRNCASECMP
#define STRNICMP strncasecmp
#else
#ifdef HAVE_STRNICMP
#define STRNICMP strnicmp
#else
#error "Geen strnicmp of strncasecmp gedefinieerd"
#endif
#endif

#ifdef LOCAL
# define NODE       "localhost"
# define PORT       "80"
# define URL        "/"
#else
# define NODE       "localhost"
# define PORT       "80"
# define URL        "/"
#endif

#define DEFAULT_PORT       "80"
#define URLLEN     512
#define BUFLEN     10240
#define USER_AGENT "User-Agent: htget/" VERSION " cmd-page-retriever"

#define CHECK_URL_LEN(x) \
           if (strlen(x) > URLLEN) { \
               fprintf(stderr, "urlname to long\nrecompile with bigger" \
                               " URLLEN > %d\n", strlen(x)); \
               exit(1); \
           }

int  repeat;
int debug = 0;
int  error = FALSE;
char *hostname = (char*)NODE;
char url[URLLEN] = URL;
char *port = (char*)PORT;
FILE *outfd;
char buffer[BUFLEN];

#define err_sock_print() perror("Cannot write to net\n")

extern char* optarg;
extern int optind;

/*
 * Function declarations
 */
char *strip_cr(char *b);
int get_page();
int expand_uri(char *  buffer); 


int main(int argc, char **argv)
{
    int c;

    fprintf(stderr, "htget " VERSION " (C) 1997 M.J.E. Mol, marcel@mesa.nl\n");

    outfd = stdout;

    while ((c = getopt(argc, argv, "dh:o:p:u:")) != EOF)
        switch (c) {
            case 'd':
                      debug++;
                      break;
            case 'h':
                      hostname = optarg;
                      break;
            case 'o':
                      outfd = fopen(optarg, "w");
                      break;
            case 'p':
                      port = optarg;
                      break;
            case 'u':
                      CHECK_URL_LEN(optarg);
                      strcpy(url, optarg);
                      break;
        }


    if (optind < argc) {
        while (optind < argc) {
            if (expand_uri(argv[optind++]) != 0)
            if (debug) {
                fprintf(stderr, "Host: %s\n", hostname);
                fprintf(stderr, "Port: %s\n", port);
                fprintf(stderr, "URL : %s\n", url);

                fprintf(stderr, "Socket init\n");
            }
            get_page();
        }
        exit(0);
    }
    else
        exit (get_page());

} /* main */



int get_page()
{
    struct tcp_socket *s;
    char *c;
    size_t i;

    if (debug)
        fprintf(stderr, "Starting page\n");

    do {
        if ((s = make_connection(hostname, port)) == NULL) {
            fprintf(stderr, "Can't connect to: %s:%s\n", hostname, port);
            return 1;
        }
        if (sock_printf(s, (char*)"GET %s HTTP/1.0", url) < 0) {
            err_sock_print();
            return 1;
        }
        if (sock_printf(s, (char*)"%s", USER_AGENT) < 0) {
            err_sock_print();
            return 1;
        }
        if (sock_printf(s, (char*)"Host: %s:%s", hostname, port) < 0) {
            err_sock_print();
            return 1;
        }

        if (sock_printf(s, (char*)"") < 0) {
            err_sock_print();
            return 1;
        }

        if (debug) {
	  fprintf(stderr, (char*)"GET %s HTTP/1.0\n", url);
            fprintf(stderr, (char*)"%s\n", USER_AGENT);
            fprintf(stderr, (char*)"Host: %s:%s", hostname, port);
            fprintf(stderr, (char*)"\n");
        }

        repeat = FALSE;
        memset(buffer, 0, sizeof(buffer));

        while (sgets(buffer, sizeof(buffer)-1, s) != NULL) {
            strip_cr(buffer);

            if (debug >= 2)
	      fprintf(stderr,"Hdr:%4ld: %s\n", (long)strlen(buffer), buffer);
            else if (debug)
	      fprintf(stderr, "Hdr:Size = %4ld\n", (long)strlen(buffer));

            if (buffer[0] == 0)
                break;  /* end of header lines */

            if (STRNICMP(buffer, "Location:", 9) == 0) {
                if (expand_uri(buffer) != 0)
                    return 1;

                if (debug)
                    fprintf(stderr, "New location: host=%s, port=%s, url=%s\n",
                                    hostname, port, url);
                repeat = TRUE;
                close_socket(s);
                break;
            }
        }
    } while (repeat);

    if (debug)
        fprintf(stderr, "Reading page\n");


    /*
     * Now we get the real data!
     */
    while ((c = sgets(buffer, sizeof(buffer)-1, s)) != NULL) {
        if (debug >= 2)
            fprintf(stderr, "Read %d bytes", c - buffer -1);


        if ((i = fwrite(buffer, 1L, (size_t) (c - buffer -1), outfd)) == 0)
            perror("fwrite");

        if (debug >= 2)
            fprintf(stderr, "wrote %d bytes\n", i);
    }
    fflush(stdout);
    close_socket(s);

    return 0;

} /* get_page */



char *strip_cr(char *b)
{
    size_t len = strlen(b);

    while (--len >= 0 && strchr("\n\r \t", b[len]) != NULL)
        b[len] = 0;

    return b;

} /* strip_cr */


int expand_uri(char *  buffer)
{
    char *c, *d, *e;

    if ((c = strstr(buffer, "http://")) == NULL) {
        if ((c = strchr(buffer, '/')) == NULL) {
            fprintf(stderr, "Bad Location: %s\n", buffer);
            return 1;
        }
        CHECK_URL_LEN(c);
        strcpy(url, c);
    }
    else {
        c += 7;
        d = strchr(c, ':');
        e = strchr(c, '/');
        if (d != NULL) {
            if (e != NULL && d > e) /* if : after first / */
                d = NULL;
        }
        if (d != NULL) {
            *d++ = 0;
            port = d;
        }
        else
	  port = (char *)DEFAULT_PORT;
        if (e != NULL) {
            CHECK_URL_LEN(e);
            strcpy(url, e);
            *e = 0;
        }
        else
            url[0] = 0;
        hostname = c;
    }

    return 0;

} /* expand_uri */

