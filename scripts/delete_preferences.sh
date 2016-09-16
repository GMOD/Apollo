#!/bin/bash

DBARG=apollo

if [ $# -ge 1 ]
then
DBARG=$1
fi

echo "Deleting features from $DBARG"

psql $DBARG -c  "delete from preference";
psql $DBARG -c  "delete from grails_user_assemblage";
psql $DBARG -c  "delete from assemblage";

