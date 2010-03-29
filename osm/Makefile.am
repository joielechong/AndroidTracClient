
SHELL=/bin/sh

INCLUDES=-O2 -Wall -ggdb $(DEPS_CFLAGS) $(OSM_CFLAGS) $(ARG_CFLAGS)

SUFFIXES=".cpp .o"

bin_PROGRAMS=mkdb dijkstra osmtest
mkdb_SOURCES=mkdb.cpp  myparser.cpp osm_db.h myparser.h
mkdb_LDADD=$(DEPS_LIBS) -losm $(OSM_LIBS) $(ARG_LIBS) 
osmtest_SOURCES=osmtest.cpp  osm.h osm_db.h
osmtest_LDADD=-losm $(OSM_LIBS) $(ARG_LIBS)
dijkstra_SOURCES=dijkstra.cpp

lib_LTLIBRARIES = libosm.la libosmsql.la
libosm_la_LDFLAGS = -version-info 0:1:0
libosm_la_SOURCES = osm.cpp osm_db.cpp
include_HEADERS = osm_db.h osm.h 
libosm_la_LIBADD=$(OSM_LIBS)

libosmsql_la_SOURCES = sqlosm.c
libosmsql_la_LIBADD =-lm

all-local: $(CONFIG_HEADER)
