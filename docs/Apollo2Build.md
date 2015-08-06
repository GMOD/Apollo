# Quick-start guide

Here we will introduce how to setup WebApollo on your server. You should not have to worry too much about installing pre-requisites, as the Grails pre-requisites and Apollo pre-requisites are downloaded and installed locally (not to your system) during the setup.

Also note: There are two modes of deploying WebApollo.

There is "development mode" where the application is launched in a temporary server (automatically) and there is "production mode", which will typically require an external separate database and tomcat server where you can deploy the generated `war` file.

This guide will cover both of those cases.

## Install prerequisites

You will minimally need to install Java (Oracle or OpenJDK's version, Java (7 or greater), ([Grails](https://grails.org/), [git](https://git-scm.com/), [ant](http://ant.apache.org/), and java web server - [tomcat, minimally 7.0.28](http://tomcat.apache.org/) or [resin](http://caucho.com/).  If you wish to use a database a non-embedded database (recommended for a production environment) you will have to install that separately.  
### Grails 
Installing grails is made easier by using [GVM](http://gvmtool.net/) which can automatically setup grails for you. We will use grails 2.4.5 for Web Apollo

1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.5

### Get the code

To setup WebApollo, you can download the code from github:

- git clone https://github.com/GMOD/Apollo.git Apollo
- cd Apollo

Using a stable release tag might be suggested as well

### Verify install requirements

Once you have the pre-requisites installed and the code checked out, you can actually quick-start the application by launching it in "development mode" with this command:

```
    apollo run-local
```

The server will be automatically launched at `http://localhost:8080/apollo` once is up and running. Note: the command also accepts a port number as an argument, e.g. `apollo run-local 8085` so you can use this if there are conflicts on port 8080.

## Setting up the application


After we have a server setup, we will want to now install add a new organism to the panel. If you are a new user, you will want to setup a jbrowse data directory. You can see the [data loading guide](Data_loading.md) for more details, but essentially, you will want to run

```
    bin/prepare-refseqs.pl --fasta yourgenome.fasta --out /some/directory
    bin/flatfile-to-json.pl --gff yourannotations.fasta --type mRNA --trackLabel Annotations --out /some/directory
```

Then, after this jbrowse data directory is created, you can login to the web interface to add it.



### Login to the web interface

After copying the WAR file, the webapp will be deployed, and you can access it at http://localhost:8080/apollo/ or similar (the path will match whatever you named the WAR file) and you will be prompted for login information

![Login first time](images/1.png)

Figure 1. "Register First Admin User" screen allows you to create a new admin user.


![Organism configuration](images/2.png)

Figure 2. Navigate to the "Organism tab" and select "Create new organism". Then enter the new information for your
organism. Importantly, the data directory refers to a directory that has been prepared with the JBrowse data loading
scripts from the command line. See the [data loading](Data_loading.md) section for details.

![Open annotator](images/3.png)

Figure 3. Open up the new organism from the drop down tab on the annotator panel.



