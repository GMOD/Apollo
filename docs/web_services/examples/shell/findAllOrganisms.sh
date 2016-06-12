#!/bin/bash

url=$1
username=$2
password=$3

username='demo'

curl  --header "Content-type: application/json" \
--request POST \
--data "{ 'username': 'ndunn@me.com', 'password': 'demo'}" http://icebox.lbl.gov/Apollo-staging/organism/findAllOrganisms 
#--data "{ 'username': 'ndunn@me.com', 'password': 'demo'}" http://localhost:8080/apollo/organism/findAllOrganisms 

