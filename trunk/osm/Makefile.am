
SHELL=/bin/sh

INCLUDES=-O0 -Wall -ggdb $(DEPS_CFLAGS)

SUFFIXES=".cpp .o"

bin_PROGRAMS=mkdb dijkstra
mkdb_SOURCES=main.cpp osm_db.cpp myparser.cpp osm_db.h myparser.h
mkdb_LDADD=$(DEPS_LIBS)

dijkstra_SOURCES=dijkstra.cpp

all-local: $(CONFIG_HEADER)
