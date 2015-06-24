#!/bin/sh


echo "Do one run just to cache the data"
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 5 -load 1 -showHeader
echo "Starting testing"
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 5 -load 1 -showHeader
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 5 -load 2
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 5 -load 4
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 5 -load 8
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 2 -iter 5 -load 1
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 2 -iter 5 -load 2
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 2 -iter 5 -load 4
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 2 -iter 5 -load 8
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 4 -iter 5 -load 1
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 4 -iter 5 -load 2
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 4 -iter 5 -load 4
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 4 -iter 5 -load 8
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 10 -load 1
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 10 -load 2
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 10 -load 4
./stress_test.groovy -organism Honey1 -username ndunn@me.com -password demo -destinationurl http://localhost:8080/apollo -concurrency 1 -iter 10 -load 8
