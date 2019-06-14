#!/bin/bash
#docker run -it -v /opt/apollo/:/data -v /Users/nathandunn/PGDATA/:/var/lib/postgresql -p 8888:8080 quay.io/gmod/docker-apollo:neat-expermint-v3
#docker run -it -v /opt/apollo/:/data -p 8888:8080 quay.io/gmod/docker-apollo:neat-expermint-v3
#docker run -it -v /opt/apollo/:/data -p 8888:8080 quay.io/gmod/docker-apollo:latest
#docker run -it -v /opt/apollo/:/data -p 8888:8080 quay.io/gmod/docker-apollo:latest
#docker run -it -v /opt/apollo/:/data -p 8888:8080 d50c1cd918e3
docker run -it --env-file=env.list -v /opt/apollo/:/data -p 8888:8080 quay.io/gmod/docker-apollo:latest
