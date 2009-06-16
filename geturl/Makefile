all:	geturl

geturl:	geturl.o sockextr.o
	gcc -o geturl geturl.o sockextr.o -lstdc++

geturl.o:	geturl.cpp
	gcc -c -o geturl.o -I../ttx geturl.cpp

sockextr.o: ../ttx/sockextr.cpp
	gcc -c -o sockextr.o -I../ttx ../ttx/sockextr.cpp
