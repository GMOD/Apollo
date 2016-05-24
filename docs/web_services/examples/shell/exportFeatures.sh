#!/bin/sh 
curl --silent 'http://localhost:8080/apollo/IOService/write'  \
	-H 'Content-Type: application/json' \
	--data "{\"type\":\"GFF3\", \"seqType\":\"genomic\", \"exportAllSequences\":\"false\", \"exportGff3Fasta\":\"true\", \"output\":\"text\", \"format\":\"text\", \"sequences\":[], \"username\":\"demo@user.com\", \"password\":\"password\",\"organism\":\"Merlin\" }" ;
