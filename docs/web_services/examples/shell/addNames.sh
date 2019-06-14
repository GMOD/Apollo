#!/bin/bash

url=$1
username=$2
password=$3
names=$4

usage() {
    echo "Sample script for adding names to to web services"
    echo "Usage:    ./addNames.sh <complete_apollo_URL> <username> <password> <names>"
    echo "Example:  ./addNames.sh http://localhost:8080/apollo demo demo 'RCA-1,RCA-2,RCA-4'"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$names" ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/suggestedName/addNames -d "{'username': '${username}', 'password': '${password}', 'names':[${names}]"
curl -i -H 'Content-type: application/json' -X POST ${url}/suggestedName/addNames -d "{'username': '${username}', 'password': '${password}', 'names':[$names]}"
