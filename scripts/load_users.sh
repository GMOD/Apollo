#!/bin/sh
curl http://localhost:8080/apollo/user/loadUsers -i -H "Content-type: application/json" -X POST  -d '{"username": "demo@demo.com", "password": "demo"}'

