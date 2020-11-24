#!/bin/sh

url=$1
username=$2
password=$3
uniquename=$4

usage() {
    echo "Sample script for exporting features from organism as GFF3 via web services"
    echo "Usage:    ./getGff3.sh <complete_apollo_URL> <username> <password> <export_type>"
    echo "Example:  ./getGff3.sh http://localhost:8080/apollo demo demo peptide"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$uniquename" ]]; then
    usage
    exit
fi

echo curl "${url}/annotationEditor/getGff3" -H 'Content-Type: application/json' --data "{'features':[{'uniquename': '${uniquename}'}],  'username': '${username}', 'password': '${password}'}"
curl "${url}/annotationEditor/getGff3" -H 'Content-Type: application/json' --data "{'features':[{'uniquename': '${uniquename}'}], 'username': '${username}', 'password': '${password}'}"

