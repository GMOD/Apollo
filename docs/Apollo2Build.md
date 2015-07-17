# Web Apollo 2.0 quick-start instructions

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Apollo2Build.md">On GitHub</a>

## Install Grails:
1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.4

## Get the code
- git clone https://github.com/GMOD/Apollo.git Apollo
- cd Apollo


## Basic configuration

Web Apollo 2.0 simplifies and expands options for the database setup. This section will cover the basic options for this.

The basic idea is to setup a new apollo-config.groovy file from some existing samples to initialize your database settings.

## Database options


#### Configure for H2:
- To use H2 for your database, simply copy sample-h2-apollo-config.groovy to apollo-config.groovy
- That is it! H2 is a 'zero-configuration' or 'embedded' database, and it can be run as an 'in-memory' database, or it can write to a file
- Note: it may be better to use other more developed databases like postgres in production, but H2 can be quickly tested since it requires no other external programs to configure it.

#### Configure for PostgreSQL:
- Create a new database (e.g. this can be as simple as `createdb "apollo-production"`)
- Copy sample-postgres-apollo-config.groovy to apollo-config.groovy and make sure to edit the database name appropriately
- Note: the production environment in apollo-config is automatically used when you copy the war file to your tomcat server, and the schema will be initialized automatically.

#### Configure for MySQL:
- Create a new MySQL database (e.g. just use the name apollo-production by default)
- Copy sample-mysql-apollo-config.groovy to apollo-config.groovy


## Deploy the code as a war file

After you have setup your apollo-config.groovy file, we will build a WAR file to deploy.

```
    ./apollo deploy
```

After this completes, jbrowse and other prerequisites are automatically downloaded and packaged into a WAR file.

You can then copy the war file at target/apollo-X.Y.war to your webapps folder as apollo.war or similar, and then the WAR file will be unpacked automatically and run. If you named your file apollo.war, hen you can access your app at "http://localhost:8080/apollo"


### Login to the web interface

After deployment, you can navigate to your server at http://localhost:8080/apollo/ or similar path and you will be
prompted for login information

![Login first time](images/1.png)

Figure 1. "Register First Admin User" screen allows you to create a new admin user.


![Organism configuration](images/2.png)

Figure 2. Navigate to the "Organism tab" and select "Create new organism". Then enter the new information for your
organism. Importantly, the data directory refers to a directory that has been prepared with the JBrowse data loading
scripts from the command line. See the [data loading](Data_loading.md) section for details.

![Open annotator](images/3.png)

Figure 3. Open up the new organism from the drop down tab on the annotator panel.



## Note about the test, development, and production environments

The "production" settings in apollo-config are automatically used when you copy the war file to your tomcat server, and the schema will be initialized automatically.

If you are instead using the "apollo run-local" instead of copying the war file, the "development" environment is used. More details are available in the [configuration section](Configure.md)!


