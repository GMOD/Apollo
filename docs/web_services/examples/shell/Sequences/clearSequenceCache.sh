#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
sequence=$5

usage() {
    echo "Sample script to clear all locations from a sequence of an organism via web services"
    echo "Usage:    ./clearSequenceCache.sh <complete_apollo_URL> <username> <password> <organism> <sequence> (optional)"
    echo "Example:  ./clearSequenceCache.sh http://localhost:8080/apollo ndunn@me.com demo Honeybee Group11.18"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" ]]; then
    usage
    exit
fi

if [[ "$sequence" ]]; then 
    sequence="/$sequence"
fi 

 echo curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/cache/clear/$organism$sequence"
 curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/cache/clear/$organism$sequence"
