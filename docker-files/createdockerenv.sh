#!/usr/bin/env bash
docker build - < ../Dockerfile.buildenv -t "gmod/apollo_env"
docker push gmod/apollo_env:latest
