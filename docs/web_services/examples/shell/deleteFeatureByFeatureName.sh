#!/bin/bash

url=$1
username=$2
password=$3
name=$4

usage() {
    echo "Sample script for deleting a feature via web services"
    echo "Usage:    ./deleteFeature.sh <complete_apollo_URL> <username> <password> <unique_name_for_feature>"
    echo "Example:  ./deleteFeature.sh http://localhost:8080/apollo demo@demo.com demo hox3a-0001"
}


if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$name"  ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/deleteFeature -d "{'username':'${username}', 'password':'${password}', 'features':[{'name':'${name}'}], 'track':'${trackName}', 'organism':'${organism}'}"
curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/deleteFeature -d "{'username':'${username}', 'password':'${password}', 'features':[{'name':'${name}'}], 'track':'${trackName}', 'organism':'${organism}'}"
