#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
trackName=$5
uniqueName=$6
status=$7

usage() {
    echo "Sample script for listing statuses via web services"
    echo "Usage:    ./listStatus.sh <complete_apollo_URL> <username> <password> "
    echo "Example:  ./listStatus.sh http://localhost:8080/apollo demo demopass"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" ! -n "$status" ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/showStatus -d "{'username': '${username}', 'password': '${password}', 'features': [{'uniquename': '${uniqueName}', 'status': '${status}'}], 'track': '${trackName}', 'organism': '${organism}'}"
curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/showStatus -d "{'username': '${username}', 'password': '${password}', 'features': [{'uniquename': '${uniqueName}', 'status': '${status}'}], 'track': '${trackName}', 'organism': '${organism}'}"
