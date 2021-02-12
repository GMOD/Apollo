#!/bin/bash

url=$1
username=$2
password=$3
old_name=$4
new_name=$5

usage() {
    echo "Sample script for updateing statuses via web services"
    echo "Usage:    ./updateSuggestedName.sh <complete_apollo_URL> <username> <password> <old_name>  <new_name>"
    echo "Example:  ./updateSuggestedName.sh http://localhost:8080/apollo demo demopass resolved Completed"
    echo ""
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password"  || ! -n "$old_name" || ! -n "$new_name" ]]; then
    usage
    exit
fi

echo curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/updateName -d "{'username': '${username}', 'password': '${password}',old_name:'${old_name}',new_name:'${new_name}'}"
curl -H 'Content-type: application/json' -X POST ${url}/suggestedName/updateName -d "{'username': '${username}', 'password': '${password}',old_name:'${old_name}',new_name:'${new_name}'}"



