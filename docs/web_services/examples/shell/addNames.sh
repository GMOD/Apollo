#!/bin/bash

url=$1
username=$2
password=$3
names=$4

usage() {
    echo "Sample script for adding names to to web services"
    echo "Usage:    ./addNames.sh <complete_apollo_URL> <username> <password> <names>"
    echo "Example:  ./addNeams.sh http://localhost:8080/apollo demo demo 'RCA-1,RCA-2,RCA-4'"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$names" ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/suggestedNames/addNames -d "{'username': '${username}', 'password': '${password}', 'names':['RCA-1,RCA-2']}"
curl -i -H 'Content-type: application/json' -X POST ${url}/suggestedNames/addNames -d "{'username': '${username}', 'password': '${password}', 'names':['RCA-1,RCA-2']}"
