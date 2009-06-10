all: bsn2pg

bsn2pg: bsn2pg.cpp
	gcc -o bsn2pg -O2 -Wall -I/usr/local/pgsql/include -L/usr/local/pgsql/lib bsn2pg.cpp -lmfl -lpq -lstdc++

