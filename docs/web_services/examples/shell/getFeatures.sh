#!/bin/bash

url=$1
username=$2
password=$3

curl -b ${username}_cookes.txt -c ${username}_cookies.txt -H "Content-Type:application/json" -d "{'username': '${username}', 'password': '${password}'}" "${url}/Login?operation=login" 2> /dev/null
# curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" -H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}" "http://localhost:8080/apollo/Login?operation=login"
curl -b ${username}_cookies.txt -c ${username}_cookies.txt -e  "http://localhost:8080" --data '{ operation: "write", adapter: "GFF3", tracks: ["Annotations-scf1117875582023"], options: "output=file&format=gzip" }' http://localhost:8080/apollo/IOService
