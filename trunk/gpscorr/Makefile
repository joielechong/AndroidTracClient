all:	f477f f477c intptf


f477f:	f477.f
	g77 -ggdb -o f477f f477.f -lgfortran

intptf:	intpt.f
	g77 -ggdb -o intptf intpt.f -lgfortran

f477.o:	f477.c++
	g++ -ggdb -O0 -c -o f477.o f477.c++

f477c:	f477.o corrcoef.o egm96.o
	g++ -ggdb  -o f477c f477.o corrcoef.o egm96.o


corrcoef.o:	corrcoef.c++
	g++ -ggdb -O0 -c -o corrcoef.o corrcoef.c++

corrcoef.c++:	CORRCOEF
	(echo "typedef struct {int d1,d2;double c1,c2;} t_corrcoef;";echo "t_corrcoef corrcoef[] = {";(sed -e "s/[^0-9+-\.ED]\+/,/g;" -e "s/D/E/g;" -e "s/^,//;" <CORRCOEF);echo "};")  >corrcoef.c++

egm96.o:	egm96.c++
	g++ -ggdb -O0 -c -o egm96.o egm96.c++

egm96.c++:	EGM96
	 (echo "typedef struct {int d1,d2;double c1,c2,c3,c4;} t_egm96;";echo "t_egm96 egm96[] = {";(sed -e "s/[^0-9+-\.ED]\+/,/g;" -e "s/^,//;" <EGM96);echo "};")  >egm96.c++
