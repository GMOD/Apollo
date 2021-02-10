#!/bin/bash

url=$1
username=$2
password=$3
status_name=$4

usage() {
    echo "Sample script for creating statuses via web services"
    echo "Usage:    ./createStatus.sh <complete_apollo_URL> <username> <password> <status_name>"
    echo "Example:  ./createStatus.sh http://localhost:8080/apollo demo demopass resolved"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  || ! -n "$status_name" ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/createStatus -d "{'username': '${username}', 'password': '${password}',value:"${status_name}"}"
curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/createStatus -d "{'username': '${username}', 'password': '${password}',value:"${status_name}"}"


