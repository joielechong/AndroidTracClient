all:	f477f f477c intptf


f477f:	f477.f
	g77 -ggdb -o f477f f477.f -lgfortran

intptf:	intpt.f
	g77 -ggdb -o intptf intpt.f -lgfortran

f477c:	f477.c++
	g++ -ggdb -O0 -o f477c f477.c++