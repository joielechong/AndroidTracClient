SHELL=/bin/sh

SUFFIXES=".cpp .lpp .ypp"

AM_CXXFLAGS=-O4 -Wall
AM_CFLAGS=-O4 -Wall

AM_YFLAGS=-dtv
AM_LFLAGS=-v -s -B 

bin_PROGRAMS = koersen
koersen_SOURCES = koersen.cpp koerssyn.ypp koerslex.lpp koersen.h

all-local: $(CONFIG_HEADER)

koersen.cpp: koersen.h
