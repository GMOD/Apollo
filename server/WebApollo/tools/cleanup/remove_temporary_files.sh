#!/bin/bash

function print_usage {
	cat << END
usage: $(basename $0) -d <base_directory>
	-m <file_age_in_minutes>
        [-h]

       d: base directory for temporary files
       m: number of minutes where any file that is older will be deleted
       h: this help
END
	exit 1;
}

while getopts "d:m:h" opt;
do
	case $opt in
		h) print_usage;;
		d) dir=$OPTARG;;
		m) min=$OPTARG;;
	esac
done

for i in $(find $dir -mmin +$min -type f);
do
	rm $i && rmdir $(dirname $i)
done

find $dir -mindepth 1 -maxdepth 1 -empty -type d -exec rmdir {} \;
