#!/bin/bash


url=$1
username=$2
password=$3
groupid=$4

usage() {
    echo "Sample script to get all features from a sequence of an organism via web services"
    echo "Usage:    ./loadGroups.sh <complete_apollo_URL> <username> <password> <group_id>"
    echo "Example:  ./loadGroups.sh http://localhost:8080/apollo demo demo 123"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" ]]; then
    usage
    exit
fi

echo "Load all groups"
curl  -X POST -H 'Content-Type: application/json' -d "{'username': '${username}', 'password': '${password}'}" "${url}/group/loadGroups"


echo "Load one group with a given userId"
curl  -X POST -H 'Content-Type: application/json' -d "{'username': '${username}', 'password': '${password}', 'groupId': ${groupid}}" "${url}/group/loadGroups"

  
