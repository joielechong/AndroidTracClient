#! /bin/sh

mv $1.schema $1.schema.old
pg_dump -s -d $1 >$1.schema
