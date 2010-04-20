AM_CXXFLAGS=
AM_CFLAGS=
SHELL=/bin/sh

#INCLUDES=-O0 -Wall -ggdb $(DEPS_CFLAGS) $(OSM_CFLAGS) $(ARG_CFLAGS)
INCLUDES=-O2 -Wall -ggdb 

SUFFIXES=".cpp .o"

bin_PROGRAMS=mkdb \
	     dijkstra \
	     osmtest

mkdb_SOURCES=mkdb.cpp \
	     myparser.cpp \
	     osm_db.h \
             myparser.h \
             grootcirkel.h \
             myfunc.h \
	     osmapi.cpp \
	     osmapi.h

mkdb_LDADD=$(XML_LIBS) -losm $(OSM_LIBS) $(ARG_LIBS) $(SOCK_LIBS)
mkdb_CPPFLAGS=$(OSM_CFLAGS) $(ARG_CFLAGS) $(XML_CFLAGS) $(SOCK_CFLAGS)

osmtest_LDADD=-losm $(OSM_LIBS) $(ARG_LIBS)
osmtest_CPPDLAGS=$(OSM_CFLAGS) $(ARG_CFLAGS)
osmtest_SOURCES=osmtest.cpp \
		osm.h \
		osm_db.h

dijkstra_SOURCES=dijkstra.cpp

lib_LTLIBRARIES = libosm.la \
		  libosmsql.la

libosm_la_LDFLAGS = -version-info 0:1:0
libosm_la_LIBADD=$(OSM_LIBS)
libosm_la_CPPFLAGS=$(OSM_CFLAGS)
libosm_la_SOURCES = element.cpp node.cpp way.cpp relation.cpp map.cpp \
		    osm_db.cpp \
		    osm_db_except.cpp \
		    osm.h \
		    db_create.cpp \
		    db_postprocess.cpp \
		    db_getelements.cpp \
		    interpol.cpp

libosmsql_la_SOURCES = sqlosm.c \
                       grootcirkel.h \
                       myfunc.h

libosmsql_la_LIBADD = -lm
libosmsql_la_LDFLAGS = -version-info 0:1:0

include_HEADERS = osm_db.h \
		  osm.h

all-local: $(CONFIG_HEADER)
