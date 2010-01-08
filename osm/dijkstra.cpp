#include <stdio.h>
#include <string.h>

#define MAX_NODES 1024        /* maximum number of nodes */
#define INFINITY 1000000000      /* a number larger than every maximum path */
typedef enum {permanent, tentative} labstate;
int n,dist[MAX_NODES][MAX_NODES];     /*dist[I][j] is the distance from i to j */
void shortest_path(int s,int t,int path[ ])
{
  struct state {                          /* the path being worked on */
    int predecessor ;                     /*previous node */
    int length;                                /*length from source to this node*/
    labstate label;    /*label state*/
  } nstate[MAX_NODES];
  
  int I, j, k, min;
  struct state *p;
  
  for (p=&nstate[0];p < &nstate[n];p++) {       /*initialize state*/
    p->predecessor=-1;
    p->length=INFINITY;
    p->label=tentative;
  }
  nstate[t].length=0; 
  nstate[t].label=permanent ;
  k=t ;                                                          /*k is the initial working node */
  do {                                                            /* is  the better path from k? */
    for (I=0; I < n; I++) {                                       /*this graph has n nodes */
      if (dist[k][I] !=0 && nstate[I].label==tentative) {
	if (nstate[k].length+dist[k][I] < nstate[I].length) {
	  nstate[I].predecessor=k;
	  nstate[I].length=nstate[k].length + dist[k][I];
	}
      }
      printf("i=%d, k=%d: ",I,k);
      for(j=0;j<n;j++)
        printf("{%d %d %d},",nstate[j].predecessor,nstate[j].length,nstate[j].label);
      printf("\n");
    }
    /* Find the tentatively labeled node with the smallest label. */
    k=0;min=INFINITY;
    for (I=0;I < n;I++) {
      if(nstate[I].label==tentative && nstate[I].length < min) {
	min=nstate[I].length;
	k=I;
      }
    }
    nstate[k].label=permanent;
  } while (k!=s);
  /*Copy the path into output array*/
  I=0;k=0;
  do { 
    printf("%d %d %d\n",I,k,nstate[k].predecessor);
    path[I++]=k;
    k=nstate[k].predecessor;
  } while (k > 0);
}

int main (int argc, char **argv) {
  int i;
  int path[MAX_NODES];

  memset(dist,sizeof(dist),0);
  n=5;
  dist[0][1]=1;
  dist[1][0]=1;
  dist[0][2]=5;
  dist[2][0]=5;
  dist[1][3]=2;
  dist[3][1]=2;
  dist[1][4]=4;
  dist[4][1]=4;
  dist[2][3]=2;
  dist[3][2]=2;
  dist[2][4]=3;
  dist[4][2]=3;
  dist[3][4]=1;
  dist[4][3]=1;
  dist[4][5]=4;
  dist[5][4]=4;
  dist[2][5]=3;
  dist[5][2]=3;
  
  shortest_path(0,5,path);
  for (i=0;i<n;i++) {
    printf("%d: %d\n",i,path[i]);
  }
  return 0;
}
