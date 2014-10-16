#!/bin/bash

while [ $# -gt 0 ]; do
	arg=$1
	shift
    args="$args $arg"
done

echo "Remember to shut down the WebApollo instance before running this tool!"

mvn -q exec:java -Dexec.mainClass="org.bbop.apollo.web.tools.UpdateTranscriptToMrna" -Dexec.args="-m ./config/mapping.xml $args"
