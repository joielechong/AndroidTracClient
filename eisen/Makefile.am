
SHELL=/bin/sh

INCLUDES=-O2 -Wall -ggdb $(DEPS_CFLAGS)

SUFFIXES=".cpp .o"

bin_PROGRAMS=eisen

eisen_SOURCES=eisen.cpp eisparser.cpp eisparser.h
eisen_LDADD=$(DEPS_LIBS)

all-local: $(CONFIG_HEADER)
