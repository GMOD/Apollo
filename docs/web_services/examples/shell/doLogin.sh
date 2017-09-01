#!/bin/bash

url=$1
username=$2
password=$3

usage() {
    echo "Sample script to login to Apollo via web services"
    echo "Usage:    ./doLogin.sh <complete_apollo_URL> <username> <password>"
    echo "Example:  ./doLogin.sh http://localhost:8080/apollo demo demo"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" ]]; then
    usage
    exit
fi

echo curl -c ${username}_cookies.txt -H 'Content-Type:application/json' -d "{'username': '${username}', 'password': '${password}'}" "${url}/Login?operation=login" 2> /dev/null
curl -c ${username}_cookies.txt -H 'Content-Type:application/json' -d "{'username': '${username}', 'password': '${password}'}" "${url}/Login?operation=login" 2> /dev/null