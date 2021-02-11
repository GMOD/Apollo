#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
location=$5

usage() {
    echo "Sample script to get all locations from a sequence of an organism via web services"
    echo "Usage:    ./getSequenceByLocation.sh <complete_apollo_URL> <username> <password> <organism> <location>"
    echo "Example:  ./getSequenceByLocation.sh http://localhost:8080/apollo ndunn@me.com demo Honeybee Group11.18:5..100"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$location" ]]; then
    usage
    exit
fi

 echo curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/$organism/$location"
 curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/$organism/$location"
#curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}', 'organism': '${organism}',{ 'uniquename':'${locations}'}" "${url}/${organism}/${sequence}/getSequenceByLocation"
