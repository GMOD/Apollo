#!/bin/bash

url=$1
username=$2
password=$3

usage() {
    echo "Sample script for finding all available organisms in an Apollo instance via web services"
    echo "Usage:    ./findAllOrganisms.sh <complete_apollo_URL> <username> <password>"
    echo "Example:  ./findAllOrganisms.sh http://localhost:8080/apollo demo demo"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" ]]; then
    usage
    exit
fi

echo curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/organism/findAllOrganisms"
curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/organism/findAllOrganisms"

