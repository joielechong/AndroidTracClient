CFLAGS=-O2 -Wall -funsigned-char -mpentium
LFLAGS=-lmfl

%.o:	%.c
	gcc -c $(CFLAGS) $*.c

all: rdbeurs

install: all
	install rdbeurs /usr/local/bin

rdbeurs: rdbeurs.tab.o lex.yy.o
	gcc -o rdbeurs rdbeurs.tab.o lex.yy.o -lfl

rdbeurs.tab.c: rdbeurs.y
	bison -dtv rdbeurs.y

lex.yy.c: rdbeurs.l
	flex rdbeurs.l

lex.yy.o: lex.yy.c rdbeurs.tab.h
