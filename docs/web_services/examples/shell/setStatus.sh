#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
trackName=$5
uniqueName=$6
status=$7

usage() {
    echo "Sample script for adding status to a feature via web services"
    echo "Usage:    ./setStatus.sh <complete_apollo_URL> <username> <password> <organism_common_name> <track> <unique_name_for_feature> <status>"
    echo "Example:  ./setStatus.sh http://localhost:8080/apollo demo demo Amel Group1.10 f5f9fb2d-5b40-48fb-bf02-b67a87cfb82a 'VerificationNeeded'"
    echo ""
    echo "Note: Be sure to create the status beforehand in Apollo from the Admin tab in Annotator Panel"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$trackName" || ! -n "$uniqueName" || ! -n "$status" ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/setStatus -d "{'username': '${username}', 'password': '${password}', 'features': [{'uniquename': '${uniqueName}', 'status': '${status}'}], 'track': '${trackName}', 'organism': '${organism}'}"
curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/setStatus -d "{'username': '${username}', 'password': '${password}', 'features': [{'uniquename': '${uniqueName}', 'status': '${status}'}], 'track': '${trackName}', 'organism': '${organism}'}"
