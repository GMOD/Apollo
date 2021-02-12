#!/bin/bash

url=$1
username=$2
password=$3
suggested_name=$4

usage() {
    echo "Sample script for deleting statuses via web services"
    echo "Usage:    ./deleteSuggestedName.sh <complete_apollo_URL> <username> <password> <suggested_name>"
    echo "Example:  ./deleteSuggestedName.sh http://localhost:8080/apollo demo demopass resolved"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  || ! -n "$suggested_name" ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/deleteName -d "{'username': '${username}', 'password': '${password}',name:"${suggested_name}"}"
curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/deleteName -d "{'username': '${username}', 'password': '${password}',name:"${suggested_name}"}"

