
SHELL=/bin/sh

INCLUDES=-O0 -Wall -ggdb $(DEPS_CFLAGS) $(OSM_CFLAGS)

SUFFIXES=".cpp .o"

bin_PROGRAMS=mkdb dijkstra osm
mkdb_SOURCES=mkdb.cpp osm_db.cpp myparser.cpp osm_db.h myparser.h
mkdb_LDADD=$(DEPS_LIBS) $(OSM_LIBS)
osm_SOURCES=osm.cpp osm.h osm_db.cpp osm_db.h
osm_LDADD=$(OSM_LIBS)
dijkstra_SOURCES=dijkstra.cpp

all-local: $(CONFIG_HEADER)
