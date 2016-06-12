#!/bin/bash

url=$1
username=$2
password=$3

username='demo'

curl  --header "Content-type: application/json" \
--request POST \
--data "{ 'username': 'ndunn@me.com', 'password': 'password'}" http://icebox.lbl.gov/Apollo-staging/organism/findAllOrganisms 

