#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
trackName=$5
uniqueName=$6
attributePair=$7

usage() {
    echo "Sample script for adding attribute to a feature via web services"
    echo "Usage:    ./addAttribute.sh <complete_apollo_URL> <username> <password> <organism_common_name> <track> <unique_name_for_feature> <attribute=value>"
    echo "Example:  ./addAttribute.sh http://localhost:8080/apollo demo demo Amel Group1.10 f5f9fb2d-5b40-48fb-bf02-b67a87cfb82a isPseudo=False"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$trackName" || ! -n "$uniqueName" || ! -n "$attributePair" ]]; then
    usage
    exit
fi

attrArray=(${attributePair//=/ })
echo curl -i -H \"Content-type: application/json\" -X POST ${url}/annotationEditor/addAttribute -d \"{\"username\":\"${username}\", \"password\":\"${password}\", \"features\":[{\"non_reserved_properties\":[{\"tag\":\"${attrArray[0]}\",\"value\":\"${attrArray[1]}\"}], \"uniquename\":\"${uniqueName}\"}],\"track\":\"${trackName}\", \"organism\":\"${organism}\"}\"
curl -i -H "Content-type: application/json" -X POST ${url}/annotationEditor/addAttribute -d "{"username":"${username}", "password":"${password}", "features":[{"non_reserved_properties":[{"tag":"${attrArray[0]}","value":"${attrArray[1]}"}], "uniquename":"${uniqueName}"}],"track":"${trackName}", "organism":"${organism}"}"