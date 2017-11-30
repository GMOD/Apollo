#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
features=$5

usage() {
    echo "Sample script to get all features from a sequence of an organism via web services"
    echo "Usage:    ./getSequences.sh <complete_apollo_URL> <username> <password> <organism> <feature>"
    echo "Example:  ./getSequences.sh http://localhost:8080/apollo ndunn@me.com demo Honeybee ['Group1.10']"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || -n "$features" ]]; then
    usage
    exit
fi

curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}', 'organism': '${organism}',{ 'uniquename':'${features}'}" "${url}/annotationEditorController/getSequences"
