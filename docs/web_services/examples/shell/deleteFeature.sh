#!/bin/bash

url=$1
username=$2
password=$3
uniqueName=$4

usage() {
    echo "Sample script for adding status to a feature via web services"
    echo "Usage:    ./deleteFeature.sh <complete_apollo_URL> <username> <password> <unique_name_for_feature>"
    echo "Example:  ./deleteFeature.sh http://localhost:8080/apollo demo@demo.com demo f5f9fb2d-5b40-48fb-bf02-b67a87cfb82a"
    echo ""
    echo "Note: Be sure to create the status beforehand in Apollo from the Admin tab in Annotator Panel"
}


if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$uniqueName"  ]]; then
    usage
    exit
fi

echo curl -i -H \"Content-type: application/json\" -X POST ${url}/annotationEditor/deleteFeature -d \"{\"username\":\"${username}\", \"password\":\"${password}\", \"features\":[{\"uniquename\":\"${uniqueName}\", \"status\":\'${status}\'}], \"track\":\"${trackName}\", \"organism\":\"${organism}\"}\"
curl -i -H "Content-type: application/json" -X POST ${url}/annotationEditor/deleteFeature -d "{"username":"${username}", "password":"${password}", "features":[{"uniquename":"${uniqueName}"}]}"
