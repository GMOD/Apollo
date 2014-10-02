#!/bin/bash

function print_usage {
	cat << END
usage: $(basename $0)
	-i <input_database> || -d <annotation_directory>
	i: input JE database to update
	d: directory containing JE databases to update (will process every
	   database in the directory - overrides "-i" option
	w: location where WebApollo is installed
	H: database(s) to be processed are history databases
	h: this help
END
	exit 1
}

function update {
	args="-i $1"
	if [ $update_history ]; then
		args="$args -H"
	fi
    mvn -q exec:java -Dexec.mainClass="org.bbop.apollo.web.tools.UpdateSequenceAlterations" -Dexec.args="$args"
}

while getopts "i:d:w:Hh" opt; do
	case $opt in
		i) input_db=$OPTARG;;
		d) annotation_dir=$OPTARG;;
		H) update_history=1;;
		h) print_usage;;
	esac
done


echo "Remember to shut down the WebApollo instance before running this tool!"

if [ -z $annotation_dir ]; then
	update $input_db
else
	if [ ! $update_history ]; then
		for i in $(ls -d $annotation_dir/Annotation* | grep -v _history); do
			echo "Processing $i"
			update $i
		done
	else
		for i in $(ls -d $annotation_dir/Annotation* | grep _history); do
			echo "Processing $i"
			update $i
		done
	fi
fi
