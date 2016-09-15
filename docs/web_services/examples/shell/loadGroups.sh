#!/bin/bash

echo "Load all groups"
curl  -X POST -H 'Content-Type: application/json' -d "{'username': 'demo@demo.com', 'password': 'supersecret'}" http://localhost:8080/apollo/group/loadGroups


echo "Load one group with a given userId"

curl  -X POST -H 'Content-Type: application/json' -d "{'username': 'demo@demo.com', 'password': 'supersecret','userId':9863}" http://localhost:8080/apollo/group/loadGroups

  
