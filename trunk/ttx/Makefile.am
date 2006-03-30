SHELL=/bin/sh

SUFFIXES=".cpp .o"

AM_CXXFLAGS=-O2 -Wall

bin_PROGRAMS = ttx htget
ttx_SOURCES = ttx.cpp sockextr.cpp sockextr.h 
htget_SOURCES = htget.cpp sockextr.cpp sockextr.h

all-local: $(CONFIG_HEADER)
