#!/bin/bash

url=$1
username=$2
password=$3
status_name=$4

usage() {
    echo "Sample script for deleting statuses via web services"
    echo "Usage:    ./deleteStatus.sh <complete_apollo_URL> <username> <password> <status_name>"
    echo "Example:  ./deleteStatus.sh http://localhost:8080/apollo demo demopass resolved"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  || ! -n "$status_name" ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/deleteStatus -d "{'username': '${username}', 'password': '${password}',value:"${status_name}"}"
curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/deleteStatus -d "{'username': '${username}', 'password': '${password}',value:"${status_name}"}"

