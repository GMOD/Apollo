#!/bin/bash

url=$1
username=$2
password=$3
old_status=$4
new_status=$5

usage() {
    echo "Sample script for updateing statuses via web services"
    echo "Usage:    ./updateStatus.sh <complete_apollo_URL> <username> <password> <old_status>  <new_status>"
    echo "Example:  ./updateStatus.sh http://localhost:8080/apollo demo demopass resolved Completed"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  || ! -n "$old_status" || ! -n "$new_status" ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/updateStatus -d "{'username': '${username}', 'password': '${password}',old_value:'${old_status}',new_value:'${new_status}'}"
curl -H 'Content-type: application/json' -X POST ${url}/availableStatus/updateStatus -d "{'username': '${username}', 'password': '${password}',old_value:'${old_status}',new_value:'${new_status}'}"



