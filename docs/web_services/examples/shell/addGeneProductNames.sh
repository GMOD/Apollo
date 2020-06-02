#!/bin/bash

url=$1
username=$2
password=$3
names=$4
organisms=$5

usage() {
    echo "Sample script for adding names to to web services"
    echo "Usage:    ./addGeneProductNames.sh <complete_apollo_URL> <username> <password> <names> <organisms [optional ids]>"
    echo "Example:  ./addGeneProductNames.sh http://localhost:8080/apollo demo demo 'RCA-1,RCA-2,RCA-4' '3,5'"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$names" ]]; then
    usage
    exit
fi

#echo curl -i -H 'Content-type: application/json' -X POST ${url}/geneProductName/addGeneProductNames -d "{'username': '${username}', 'password': '${password}', 'names':[${names}]"
if [[ "$organisms" ]]; then
  curl -i -H 'Content-type: application/json' -X POST ${url}/geneProductName/addGeneProductNames -d "{'username': '${username}', 'password': '${password}', 'names':[$names], 'organisms':[$organisms]}"
else
  curl -i -H 'Content-type: application/json' -X POST ${url}/geneProductName/addGeneProductNames -d "{'username': '${username}', 'password': '${password}', 'names':[$names]}"
fi

