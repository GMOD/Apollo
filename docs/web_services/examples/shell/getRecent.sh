#!/bin/sh

url=$1
username=$2
password=$3
days=$4
status=$5

usage() {
    echo "Sample script for exporting features from organism as GFF3 via web services"
    echo "Usage:    ./getRecent.sh <complete_apollo_URL> <username> <password> <days> <status>"
    echo "Example:  ./getRecent.sh http://localhost:8080/apollo demo demo Finished|InProgress"
    echo "Example:  ./getRecent.sh http://localhost:8080/apollo demo demo None"
    echo "Example:  ./getRecent.sh http://localhost:8080/apollo demo demo "
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$days" ]]; then
    usage
    exit
fi

echo curl "${url}/annotationEditor/getRecentAnnotations" -H 'Content-Type: application/json' --data "{'days':${days},  'username': '${username}', 'password': '${password}', 'status':'${status}'}"
curl "${url}/annotationEditor/getRecentAnnotations" -H 'Content-Type: application/json' --data "{'days':${days}, 'username': '${username}', 'password': '${password}', 'status':'${status}'}"

