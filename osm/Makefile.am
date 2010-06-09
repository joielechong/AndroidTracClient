SHELL=/bin/sh

AM_CXXFLAGS=
AM_CFLAGS=
AM_YFLAGS=-dtv
AM_LFLAGS=-v -s -B

ACLOCAL_AMFLAGS=-I m4 -I gnum4
SUBDIRS=gnulib
EXTRA_DIST=gnum4/gnulib-cache.m4

#INCLUDES=-O0 -Wall -ggdb $(DEPS_CFLAGS) $(SQL_CFLAGS) $(ARG_CFLAGS)
INCLUDES=-O0 -Wall -ggdb

SUFFIXES=".cpp .ypp .lpp .o"

bin_PROGRAMS=mkdb \
	     osmtest

mkdb_SOURCES=mkdb.cpp \
	     myparser.cpp \
	     osm_db.h \
             myparser.h \
             grootcirkel.h \
	     osmapi.cpp \
             myfuncs.h \
	     osmapi.h \
             gzstream.cpp \
             gzstream.h
osmdatadir=$(datadir)/osm
dist_osmdata_DATA=schema.sqlite.txt

mkdb_LDADD=$(XML_LIBS) $(Z_LIBS) -losm $(SQL_LIBS) $(ARG_LIBS) $(SOCK_LIBS) $(ACOS_LIBM) $(ASIN_LIBM) $(COS_LIBM) $(SIN_LIBM) $(SQRT_LIBM) $(POW_LIBM) -ll
mkdb_CPPFLAGS=$(Z_CFLAGS) $(SQL_CFLAGS) $(ARG_CFLAGS) $(XML_CFLAGS) $(SOCK_CFLAGS) -DDATADIR=\"$(datadir)/osm\"

osmtest_LDADD=-losm $(SQL_LIBS) $(ARG_LIBS) $(XML_LIBS)
osmtest_CPPFLAGS=$(SQL_CFLAGS) $(ARG_CFLAGS) $(XML_CFLAGS)
osmtest_SOURCES=osmtest.cpp \
		routesyn.ypp \
		routelex.lpp \
		osm.h \
		osm_db.h

lib_LTLIBRARIES = libosm.la \
		  libosmsql.la

libosm_la_LDFLAGS = -version-info 0:1:0 $(ASIN_LIBM) $(COS_LIBM) $(POW_LIB) $(POW_LIBM) $(SIN_LIBM) $(SQRT_LIBM)
libosm_la_LIBADD=$(SQL_LIBS) $(SOCK_LIBS) $(XML_LIBS)
libosm_la_CPPFLAGS=$(SQL_CFLAGS) $(SOCK_CFLAGS) $(XML_CFLAGS)
libosm_la_SOURCES = element.cpp node.cpp way.cpp relation.cpp \
		    map.cpp \
		    astar.cpp \
		    osm_db.cpp \
		    osm_db_except.cpp \
		    osm.h \
		    db_create.cpp \
		    db_getelements.cpp \
		    interpol.cpp \
                    osm_db.h

libosmsql_la_LDFLAGS= $(ASIN_LIBM) $(COS_LIBM) $(POW_LIB) $(POW_LIBM) $(SIN_LIBM) $(SQRT_LIBM)
libosmsql_la_SOURCES = sqlosm.c \
                       grootcirkel.h \
                       myfuncs.h

libosmsql_la_LIBADD = -lm $(ASIN_LIBM) $(COS_LIBM) $(POW_LIB) $(POW_LIBM) $(SIN_LIBM) $(SQRT_LIBM)

libosmsql_la_LDFLAGS += -version-info 0:1:0

include_HEADERS = osm_db.h \
		  osm.h

all-local: $(CONFIG_HEADER)
