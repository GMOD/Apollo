#!/bin/bash

url=$1
username=$2
password=$3

usage() {
    echo "Sample script for listing statuses via web services"
    echo "Usage:    ./listSuggestedName.sh <complete_apollo_URL> <username> <password> "
    echo "Example:  ./listSuggestedName.sh http://localhost:8080/apollo demo demopass"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/showName -d "{'username': '${username}', 'password': '${password}'}"
curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/showName -d "{'username': '${username}', 'password': '${password}'}"
