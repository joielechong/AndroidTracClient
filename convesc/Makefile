CFLAGS=-O2 -Wall -funsigned-char -mpentium 

LINK=gcc

ROOTDIR=/home/mfvl
BINDIR=$(ROOTDIR)/bin

all: convtbl skipheader

skipheader: skipheader.cpp
	$(LINK) -o skipheader skipheader.cpp 

convtbl: lex.yy.c
	$(LINK) $(CFLAGS) -o convtbl lex.yy.c -lfl

lex.yy.c: convtbl.l
	flex -i convtbl.l

install: all
	install -m 755 -s convtbl $(BINDIR)
	install -m 755 -s skipheader $(BINDIR)


