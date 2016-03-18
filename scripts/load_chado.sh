#!/bin/bash

USER=$1
DB=$2
HOST=localhost

gunzip -c chado-schema-with-ontologies.sql.gz | psql -U ${USER} -h ${HOST} -d ${DB}
