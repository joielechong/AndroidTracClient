// uuenc.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

/* uuencode.c -- uuencode utility.
 * Copyright (C) 1994, 1995 Free Software Foundation, Inc.
 *
 * This product is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This product is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this product; see the file COPYING.  If not, write to
 * the Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

/* Copyright (c) 1983 Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *       This product includes software developed by the University of
 *       California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/* Reworked to GNU style by Ian Lance Taylor, ian@airs.com, August 93.  */
/* Hacked to work with BusyBox by Alfred M. Szmidt */


#include <stdio.h>
#include <errno.h>

#define TRUE (-1)
#define FALSE (0)


/* The two currently defined translation tables.  The first is the
   standard uuencoding, the second is base64 encoding.  */
const char uu_std[64] = {
  '`', '!', '"', '#', '$', '%', '&', '\'',
  '(', ')', '*', '+', ',', '-', '.', '/',
  '0', '1', '2', '3', '4', '5', '6', '7',
  '8', '9', ':', ';', '<', '=', '>', '?',
  '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
  'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
  'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
  'X', 'Y', 'Z', '[', '\\', ']', '^', '_'
};

/* ENC is the basic 1 character encoding function to make a char printing.  */
#define ENC(Char) (uu_std[(Char) & 077])

/* Copy from IN to OUT, encoding as you go along. */
static void encode()
{
  register int ch, n;
  char *p = NULL;
  char buf[80];

  while (1) {
    n = 0;
    do {
      register int m = fread (buf, 1, 45 - n, stdin);
      if (m == 0)
        break;
      n += m;
    }
    while (n < 45);

    if (n == 0)
      break;

    if (putchar (ENC (n)) == EOF)
      break;
    for (p = buf; n > 2; n -= 3, p += 3) {
      ch = *p >> 2;
      ch = ENC (ch);
      if (putchar (ch) == EOF)
        break;
      ch = ((*p << 4) & 060) | ((p[1] >> 4) & 017);
      ch = ENC (ch);
      if (putchar (ch) == EOF)
        break;
      ch = ((p[1] << 2) & 074) | ((p[2] >> 6) & 03);
      ch = ENC (ch);
      if (putchar (ch) == EOF)
        break;
      ch = p[2] & 077;
      ch = ENC (ch);
      if (putchar (ch) == EOF)
        break;
    }

    if (n != 0)
      break;

    if (putchar ('\n') == EOF)
      break;
  }

  while (n != 0) {
    char c1 = *p;
    char c2 = n == 1 ? 0 : p[1];

    ch = c1 >> 2;
    ch = ENC (ch);
    if (putchar (ch) == EOF)
      break;

    ch = ((c1 << 4) & 060) | ((c2 >> 4) & 017);
    ch = ENC (ch);
    if (putchar (ch) == EOF)
      break;

    if (n == 1)
      ch = ENC ('\0');
    else {
      ch = (c2 << 2) & 074;
      ch = ENC (ch);
    }
    if (putchar (ch) == EOF)
      break;
    ch = ENC ('\0');
    if (putchar (ch) == EOF)
      break;
    putchar ('\n');
    break;
  }

  if (ferror (stdin))
    fprintf(stderr,"Read error\n");

    putchar (ENC ('\0'));
    putchar ('\n');
}

static const char uuencode_usage[] =
    "uuencode INFILE REMOTEFILE\n"
#ifndef BB_FEATURE_TRIVIAL_HELP
    "\nUuencode a file.\n\n"
#endif
;

int main (int argc,
                   char **argv)
{
  int mode = 0644;

  printf("begin %o %s\n", mode, argv[1]);
  encode();
  printf("end\n");
  if (ferror (stdout)) {
    fprintf(stderr,"Write error\n");
    return( FALSE);
  }
  return( TRUE);
}
