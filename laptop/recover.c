#include <stdio.h>
#include <dir.h>
#include <string.h>
#include <stdlib.h>
#include <\tc\source\news\hashnews.h>

char *newsdir = "\\spool\\news";

struct
{
	char name[64];
	int nr;
} groups[100];

int ngrp = 0;

char *rip(char *s)
{
	char *c;

	if ((c=strchr(s,'\n')) != NULL)
		*c = 0;
	return s;
}

char *get_field(FILE *fid,char *f)
{
	static char buffer[1024];
	size_t lf;
	char *c,*b;

	if (fid == NULL || f == NULL || *f == 0)
		return "";
	lf = strlen(f);
	rewind(fid);
	while (fgets(buffer,1024,fid) != NULL)
	{
		rip(buffer);
		if (strlen(buffer) == 0)
			break;
		if (strnicmp(buffer,f,lf) == 0)
		{
			c = buffer + lf;
			while (*c == ' '|| *c == '\t')
				c++;
			b = c + strlen(c);
			while (fgets(b,1024-((int)(b-buffer)),fid) != NULL)
			{
				if (strchr(" \t",*b) == NULL)
				{
					*b = 0;
					break;
				}
				rip(b);
				b += strlen(b);
			}
			return c;
		}
	}
	return "";
}

int voeg_toe(char *ng,char *msg_id,long filenr)
{
	FILE *fid;
	int i;
	char filename[15];
	unsigned hv;
	struct msg_ptr mp;

	for(i=0;i<ngrp;i++)
	{
		if (stricmp(groups[i].name,ng) == 0)
			break;
	}
	if (i==ngrp)
	{
		strcpy(groups[i].name,ng);
		groups[i].nr = 0;
		ngrp++;
		sprintf(filename,"recv%4.4d.grp",i);
		fid = fopen(filename,"wt");
		fprintf(fid,"%s\n",ng);
	}
	else
	{
		sprintf(filename,"recv%4.4d.grp",i);
		fid=fopen(filename,"at");
	}
	fprintf(fid,"%d %s\n",++groups[i].nr,msg_id);
	hv = hash(msg_id,0);
	if (hv > hashsize)
		hv = hash(msg_id,1);
	if (hv > hashsize)
	{
		fclose(fid);
		return -1;
	}
	get_msg_ptr(hv,&mp);
	mp.status.article = 1;
	mp.filenr = filenr;
	put_msg_ptr(hv,&mp);
	fclose(fid);
	return 0;
}

void main(void)
{
	FILE *fid;
	struct ffblk ffb;
	int iret;
	char *ng,*msg_id,*d,*c;
	char new_file[20];

	for(iret=findfirst("*.nws",&ffb,0);iret==0;iret=findnext(&ffb))
	{
		if ((fid = fopen(ffb.ff_name,"rt")) != NULL)
		{
			ng = strdup(get_field(fid,"Newsgroups:"));
			msg_id = strdup(get_field(fid,"Message-ID:"));
			fclose(fid);
			if (strlen(ng) > 0 && strlen(msg_id) > 0)
			{
				for(c=ng;c!=NULL;c=d)
				{
					d = strchr(c,',');
					if (d != NULL)
						*d++ = 0;
					if (voeg_toe(c,msg_id,atol(ffb.ff_name)) >= 0)
					{
						sprintf(new_file,"..\\%s",ffb.ff_name);
						rename(ffb.ff_name,new_file);
						printf("Added %s\n",ffb.ff_name);
					}
					else
						printf("Skipped %s\n",ffb.ff_name);
				}
			}
			free(msg_id);
			free(ng);
		}
	}
}