#!/bin/bash

url=$1
username=$2
password=$3
organism=$4
sequence=$5
feature=$6
feature_type=$7

usage() {
    echo "Sample script to get all sequences from a sequence of an organism via web services . . type is peptide, cds, cdna, genomic"
    echo "Usage:    ./getSequenceBysequence.sh <complete_apollo_URL> <username> <password> <organism> <sequence> <type>"
    echo "Example:  ./getSequenceBysequence.sh http://localhost:8080/apollo ndunn@me.com demo Honeybee Group11.18 GB45222-RA-00002 peptide"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$sequence" || ! -n "$feature"  || ! -n "$feature_type"  ]]; then
    usage
    exit
fi


 echo curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/$organism/$sequence/$feature.$feature_type"
 curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}'}" "${url}/sequence/$organism/$sequence/$feature.$feature_type"
#curl  --header "Content-type: application/json" --request POST --data "{'username': '${username}', 'password': '${password}', 'organism': '${organism}',{ 'uniquename':'${sequences}'}" "${url}/${organism}/${sequence}/getSequenceBysequence"
