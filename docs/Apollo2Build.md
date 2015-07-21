# Quick-start guide

## Install grails

Installing grails is made easier by using [GVM](http://gvmtool.net/) which can automatically setup grails for you. We will use grails 2.4.4 for Web Apollo

1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.4

## Get the code

To setup WebApollo, you can download the code from github:

- git clone https://github.com/GMOD/Apollo.git Apollo
- cd Apollo

Using a stable release tag might be suggested as well

## Basic configuration

Web Apollo 2.0 simplifies and expands options for the database setup. This section will cover the basic options for this.

The basic idea is to setup a new apollo-config.groovy file from some existing samples to initialize your database settings.

## Database options


#### Configure for H2:
- To use H2 for your database, simply copy sample-h2-apollo-config.groovy to apollo-config.groovy
- Note: H2 is a 'zero-configuration' or 'embedded' database, so it doesn't require any external database to be setup

#### Configure for PostgreSQL:
- Create a new database with postgres (i.e. via the command line `createdb apollo-production`)
- Copy sample-postgres-apollo-config.groovy to apollo-config.groovy

#### Configure for MySQL:
- Create a new MySQL database (i.e. run `create database apollo-production` in the mysql console)
- Copy sample-mysql-apollo-config.groovy to apollo-config.groovy

### Notes on database the database setup

- The schema is automatically initialized when you startup the app, but manually creating the database is necessary before hand. See below for description on the "production" vs "development" environments

## 

After you have setup your apollo-config.groovy file, we will build a WAR file to deploy.

```
    ./apollo deploy
```

This command will download and package jbrowse and other prerequisites into a WAR file in the "target/" subfolder. After it completes, you can then copy the WAR file (i.e. `target/apollo-X.Y.Z.war`) to your webapps folder. If you name the file apollo.war in your webapps folder, then you can access your app at "http://localhost:8080/apollo".


Note: you can also run the following command to launch a temporary server automatically, which makes it useful for testing, and this makes the app accessible  "http://localhost:8080/apollo" by default

```
    ./apollo run-local
```


If you use the "run-local", then the "development" section of the apollo-config.groovy is used, but if you use "deploy", then the "production" section of the apollo-config.groovy is used.


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



