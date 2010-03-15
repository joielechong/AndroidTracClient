CPPFLAGS=-I/usr/include/libxml++-2.6/ -I/usr/include/glibmm-2.4 -I /usr/lib/glibmm-2.4/include -I/usr/include/glib-2.0 -I/usr/lib/glib-2.0/include -I/usr/lib/libxml++-2.6/include -ggdb -O0

CFLAGS=-ggdb -O0
LDFLAGS=
LIBS=-lxml++-2.6 -lglibmm-2.4 -lglib-2.0 -lxml2 -lsqlite3

all:	mkdb dijkstra

clean:
	rm *.o test dijkstra

mkdb:	main.o myparser.o
	g++ ${LDFLAGS} -o mkdb main.o myparser.o ${LIBS}

dijkstra:	dijkstra.o
	g++ -o dijkstra dijkstra.o

depend:	
	g++ -MM -I/usr/include/libxml++-2.6/ -I/usr/include/glibmm-2.4 -I /usr/lib/glibmm-2.4/include -I/usr/include/glib-2.0 -I/usr/lib/glib-2.0/include -I/usr/lib/libxml++-2.6/include *.cpp >make.depend

include make.depend
