#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
track=$5

usage() {
    echo "Sample script to get all features from a sequence of an organism via web services"
    echo "Usage:    ./getFeatures.sh <complete_apollo_URL> <username> <password> <organism> <sequence_name>"
    echo "Example:  ./getFeatures.sh http://localhost:8080/apollo demo demo Amel Group1.10"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$track" ]]; then
    usage
    exit
fi

# First login to Apollo
echo curl -b login_cookies.txt -c login_cookies.txt -H "Content-Type:application/json" -d "{'username': '${username}', 'password': '${password}'}" ${url}/Login?operation=login
curl -b login_cookies.txt -c login_cookies.txt -H "Content-Type:application/json" -d "{'username': '${username}', 'password': '${password}'}" ${url}/Login?operation=login

# Then request for features
echo curl -b login_cookies.txt -c login_cookies.txt -e "${url}" --data "{ 'organism':'${organism}','operation': 'get_features', 'track': '${track}'}" ${url}/annotator/AnnotationEditorService
curl -b login_cookies.txt -c login_cookies.txt -e "${url}" --data "{ 'organism':'${organism}','operation': 'get_features', 'track': '${track}'}" ${url}/annotator/AnnotationEditorService
