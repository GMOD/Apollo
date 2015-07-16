#!/bin/bash
 curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" -H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}" "http://localhost:8080/apollo/Login?operation=login"
 curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" --data '{ operation: "write", adapter: "GFF3", tracks: ["scf1117875582023"], options: "output=file&format=gzip" }' http://localhost:8080/apollo/IOService
 # look for url 
