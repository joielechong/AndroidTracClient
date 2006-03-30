CC=gcc -O4 -Wall -I/usr/local/pgsql/include


giro: giro.tab.o lex.yy.o pg_giro.o
	gcc -o giro giro.tab.o lex.yy.o pg_giro.o -L/usr/local/pgsql/lib -lpq

giro.tab.c: giro.y Makefile
	bison -d -t giro.y

giro.tab.o: giro.tab.c pg_giro.h

lex.yy.o: lex.yy.c giro.tab.h

lex.yy.c: giro.l Makefile
	flex -v -B giro.l

giro.tab.h: giro.y

pg_giro.o: pg_giro.c pg_giro.h

clean: 
	rm -f *.o *.i *.output giro.tab.* lex.yy.*

install: giro
	install giro ~/bin

