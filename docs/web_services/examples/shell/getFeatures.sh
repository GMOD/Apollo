#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
track=$5

usage() {
    echo "Sample script to get all features from a sequence of an organism via web services"
    echo "Usage:    ./getFeatures.sh <complete_apollo_URL> <username> <password> <organism> <sequence_name>"
    echo "Example:  ./getFeatures.sh http://localhost:8080/apollo demo@demo.com demo Honeybee Group1.10"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$track" ]]; then
    usage
    exit
fi

echo curl -H 'Content-Type:application/json' --data "{ 'organism':'${organism}','operation': 'get_features', 'track': '${track}','username':'${username}','password':'${password}'}" ${url}/annotationEditor/getFeatures
curl -H 'Content-Type:application/json' --data "{ 'organism':'${organism}','operation': 'get_features', 'track': '${track}','username':'${username}','password':'${password}'}" ${url}/annotationEditor/getFeatures
