#!/bin/bash

echo "Load all users"
curl  -i -X POST -H 'Content-Type: application/json' -d "{'username': 'demo@demo.com', 'password': 'supersecret'}" http://localhost:8080/apollo/user/loadUsers


echo "Load one users with a given userId"

curl  -i -X POST -H 'Content-Type: application/json' -d "{'username': 'demo@demo.com', 'password': 'supersecret','userId':288335}" http://localhost:8080/apollo/user/loadUsers

  
