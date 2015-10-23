# Command line tools

The command line tools offer a number of interesting features that can be used to help setup and retrieve data from the
application.


## Overview


The command line tools are located in docs/web_services/examples, and they are mostly small scripts that automate the
usage of the the web services API.

### get_gff3.groovy

Example:

``` 
get_gff3.groovy -organism Amel_4.5 -username admin@webapollo.com \
    -password admin_password -url http://localhost:8080/apollo > my output.gff3
```
This command outputs the gff3 to standard output and can be redirected.


The -username and -password refer to the admin user, and they can also be specified via stdin instead of the command
line if they are omitted.


### add_users.groovy


Example:

``` 
add_users.groovy -username admin@webapollo.com -password admin_password \
    -newuser newuser@test.com -newpassword newuserpass \
    -destinationurl http://localhost:8080/apollo
```

The -username and -password refer to the admin user, and they can also be specified via stdin instead of the command
line if they are omitted.

A list of users specified in a csv file can also be used as input.

### add_organism.groovy


Example:

``` 
add_organism.groovy -name yeast -url http://localhost:8080/apollo/ \
    -directory /opt/apollo/yeast -username admin@webapollo.com -password admin_password
```


The -directory refers to the jbrowse data directory containing the output from prepare-refseqs.pl, flatfile-to-json.pl,
etc. The -blatdb is optional, -genus, and -species are optional.

The -username and -password refer to the admin user, and they can also be specified via stdin instead of the command
line if they are omitted.



