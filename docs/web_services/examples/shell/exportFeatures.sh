#!/bin/sh

url=$1
username=$2
password=$3
organism=$4
export_type=$5

usage() {
    echo "Sample script for exporting features from organism as GFF3 via web services"
    echo "Usage:    ./exportFeatures.sh <complete_apollo_URL> <username> <password> <export_type>"
    echo "Example:  ./exportFeatures.sh http://localhost:8080/apollo demo demo peptide"
}

if [[ ! -n "$url" || ! -n "$username" || ! -n "$password" || ! -n "$organism" || ! -n "$export_type" ]]; then
    usage
    exit
fi

echo curl "${url}/IOService/write" -H 'Content-Type: application/json' --data "{'type': 'GFF3', 'seqType':'${export_type}', 'exportAllSequences': 'false', 'exportGff3Fasta': 'true', 'output': 'text', 'format': 'text', 'sequences':[], 'username': '${username}', 'password': '${password}', 'organism': '${organism}'}"
curl "${url}/IOService/write" -H 'Content-Type: application/json' --data "{'type': 'GFF3', 'seqType':'${export_type}', 'exportAllSequences': 'false', 'exportGff3Fasta': 'true', 'output': 'text', 'format': 'text', 'sequences':[], 'username': '${username}', 'password': '${password}', 'organism': '${organism}'}"

