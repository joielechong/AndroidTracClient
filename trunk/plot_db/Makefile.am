SHELL=/bin/sh

SUFFIXES=".cpp .o"

INCLUDES=-I/usr/local/pgsql/include/
CPPFLAGS=-I/usr/local/pgsql/include/
LDFLAGS=-L/usr/local/pgsql/lib/ 
LIBS=-lmfl -liconv -lgd -lpq

CFLAGS=-O2 -Wall

bin_PROGRAMS = Barchart Scatchart
Barchart_SOURCES = Barchart.cpp getresults.cpp
Scatchart_SOURCES = Scatchart.cpp getresults.cpp

all-local: $(CONFIG_HEADER)
