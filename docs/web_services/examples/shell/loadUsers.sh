#!/bin/bash

url=$1
username=$2
password=$3
userid=$4

usage() {
    echo "Sample script to get all features from a sequence of an organism via web services"
    echo "Usage:    ./loadUsers.sh <complete_apollo_URL> <username> <password> <user_id>"
    echo "Example:  ./loadUsers.sh http://localhost:8080/apollo demo demo 12345"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" ]]; then
    usage
    exit
fi

if [ ! -n "$userid" ]; then
    echo "Load all users"
    curl  -i -X POST -H 'Content-Type: application/json' -d "{'omitEmptyOrganisms':true,'username': '${username}', 'password': '${password}'}" "${url}/user/loadUsers"
else
    echo "Load one users with a given userId: ${userid}"
    curl  -i -X POST -H 'Content-Type: application/json' -d "{'username': '${username}', 'password': '${password}','userId': ${userid}}" ${url}/user/loadUsers
fi
