#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
trackName=$5
uniqueName=$6
comment=$7

usage() {
    echo "Sample script for adding comment to a feature via web services"
    echo "Usage:    ./addComment.sh <complete_apollo_URL> <username> <password> <organism_common_name> <track> <unique_name_for_feature> <comment>"
    echo "Example:  ./addComment.sh http://localhost:8080/apollo demo demo Amel Group1.10 f5f9fb2d-5b40-48fb-bf02-b67a87cfb82a 'This annotation is complete'"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$trackName" || ! -n "$uniqueName" || ! -n "$comment" ]]; then
    usage
    exit
fi

echo curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/addComments -d "{'operation': 'add_comments', 'username': '${username}', 'password': '${password}', 'features':[{'uniquename': '${uniqueName}','comments':['${comment}']}], 'track':'${trackName}', 'organism': '${organism}'}"
curl -i -H 'Content-type: application/json' -X POST ${url}/annotationEditor/addComments -d "{'operation': 'add_comments', 'username': '${username}', 'password': '${password}', 'features':[{'uniquename': '${uniqueName}','comments':['${comment}']}], 'track':'${trackName}', 'organism': '${organism}'}"