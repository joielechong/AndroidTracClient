
SHELL=/bin/sh

INCLUDES=-O0 -Wall -ggdb $(DEPS_CFLAGS)
LIBS=$(DEPS_LIBS)

SUFFIXES=".cpp .o"

bin_PROGRAMS=mkdb dijkstra
mkdb_SOURCES=main.cpp osm_db.cpp myparser.cpp osm_db.h myparser.h
dijkstra_SOURCES=dijkstra.cpp

all-local: $(CONFIG_HEADER)
