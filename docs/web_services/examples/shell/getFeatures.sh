#!/bin/bash

url=$1
username=$2
password=$3

username='demo'

#curl -b ${username}_cookes.txt -c ${username}_cookies.txt -H "Content-Type:application/json" -d "{'username': '${username}', 'password': '${password}'}" "${url}/Login?operation=login" 2> /dev/null
# curl -b cookies.txt -c cookies.txt -e  "http://localhost:8080" -H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}" "http://localhost:8080/apollo/Login?operation=login"
#curl -b ${username}_cookies.txt -c ${username}_cookies.txt -e  "http://icebox.lbl.gov/WebApolloDemo/" -d "{ 'operation': 'get_features', 'tracks': ['Group1.10']}" http://icebox.lbl.gov/WebApolloDemo/

curl -b demo_cookies.txt -c demo_cookies.txt -H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}" http://icebox.lbl.gov/WebApolloDemo/Login?operation=login
curl -b demo_cookies.txt -c demo_cookies.txt -e "http://icebox.lbl.gov/WebApolloDemo/" --data "{ 'operation': 'get_features', 'track': 'Group1.10'}" http://icebox.lbl.gov/WebApolloDemo/AnnotationEditorService
