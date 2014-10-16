#!/bin/bash

while [ $# -gt 0 ]; do
	arg=$1
	shift
    args="$args $arg"
done

mvn -q exec:java -Dexec.mainClass="org.bbop.apollo.web.tools.JEDatabaseMerger" -Dexec.args="$args"

