#!/bin/bash

webapollo_dir=$WEBAPOLLO_DIR

while [ $# -gt 0 ]; do
	arg=$1
	shift
	if [[ $arg == -w ]]; then
		value=$1
		webapollo_dir=$value
		shift
	else
		args="$args $arg"
	fi
done

if [ -z "$webapollo_dir" ]; then
	echo "Missing -w <webapollo_installation_dir> or \$WEBAPOLLO_DIR environment variable"
	exit 1
elif [ ! -d "$webapollo_dir" ]; then
	echo "Cannot access webapollo installation directory: $webapollo_dir"
	exit 1
fi

java -cp "$webapollo_dir/WEB-INF/classes:$webapollo_dir/WEB-INF/lib/*" org.bbop.apollo.web.tools.JEDatabaseMerger $args
