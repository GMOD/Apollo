#!/bin/bash

webapollo_dir=$WEBAPOLLO_DIR

function print_usage {
	cat << END
usage: $(basename $0)
	-i <input_database>
	-I <top_level_feature_id_to_remove>
	-w <webapollo_installation_dir>
	[-h]

	i: input JE database to update
	I: top level feature id to remove (can be called multiple times)
	w: location where WebApollo is installed
	h: this help
END
	exit 1
}

function update {
	args="-i $input_db"
	for i in "${feature_ids[@]}"
	do
		args+=" -I $i"
	done

	java -cp "$(dirname $0)/classes.jar:$webapollo_dir/WEB-INF/classes:$webapollo_dir/WEB-INF/lib/*" org.bbop.apollo.web.tools.RemoveTopLevelFeaturesById $args
}

while getopts "i:I:w:h" opt; do
	case $opt in
		i) input_db=$OPTARG;;
		I) feature_ids+=("$OPTARG");;
		w) webapollo_dir=$OPTARG;;
		h) print_usage;;
	esac
done

if [ -z "$webapollo_dir" ]; then
	echo "Missing -w <webapollo_installation_dir> or \$WEBAPOLLO_DIR environment variable"
	exit 1
elif [ ! -d "$webapollo_dir" ]; then
	echo "Cannot access webapollo installation directory: $webapollo_dir"
	exit 1
fi

if [ -z "$input_db" ]; then
	echo "Missing -i <input_database> argument"
	exit 1
fi

if [ ! "$feature_ids" ]; then
	echo "Missing -I <top_level_feature_id_to_remove> argument"
	exit 1
fi


echo "Remember to shut down the WebApollo instance before running this tool!"

update
