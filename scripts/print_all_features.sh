#!/bin/bash

DBARG=apollo

if [ $# -ge 1 ]
then
DBARG=$1
fi

echo "Printing features from $DBARG"

echo "Users"
psql $DBARG -c  "select count(*) from feature_grails_user";

echo "DbXrefs"
psql $DBARG -c  "select count(*) from feature_dbxref";

echo "Properties"
psql $DBARG -c  "select count(*) from feature_property";

echo "Relationships"
psql $DBARG -c  "select count(*) from feature_relationship";

echo "Locations"
psql $DBARG -c  "select count(*) from feature_location";

echo "Features"
psql $DBARG -c  "select count(*) from feature";

echo "Feature Events"
psql $DBARG -c  "select count(*) from feature_event";
echo "Preferences"
psql $DBARG -c  "select count(*) from preference";

