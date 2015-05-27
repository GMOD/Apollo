#!/bin/bash

url=$1
username=$2
password=$3

curl -c ${username}_cookies.txt -H "Content-Type:application/json" -d "{'username': '${username}', 'password': '${password}'}" "${url}/Login?operation=login" 2> /dev/null