#!/bin/bash

url=$1
username=$2
password=$3

username='demo'

curl -b demo_cookies.txt -c demo_cookies.txt -H "Content-Type:application/json" -d "{'username': 'demo@demo.com', 'password': 'demo'}" http://icebox.lbl.gov/Apollo2/Login?operation=login
curl -b demo_cookies.txt -c demo_cookies.txt -e "http://icebox.lbl.gov/Apollo2/" --data "{ 'organism':'Honeybee','operation': 'get_features', 'track': 'Group1.10'}" http://icebox.lbl.gov/Apollo2/AnnotationEditorService
