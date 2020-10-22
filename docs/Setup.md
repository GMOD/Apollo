# Setup guide


The quick-start guide showed how to quickly launch a temporary instance of Apollo, but deploying the application to
production normally involves some extra steps.


The general idea behind your deployment is to create a `apollo-config.groovy` file from some existing sample files which
have sample settings for various database engines.


## Pre-requisites

The server will minimally need to have Java 8 or greater, [Grails](https://grails.org/), [git](https://git-scm.com/),
[ant](http://ant.apache.org/), a servlet container e.g. [tomcat7+](http://tomcat.apache.org/), jetty, or resin. An
external database such as PostgreSQL (9 or 10 preferred) is generally used for production, but instructions for MySQL 
or the H2 Java database (which may also be run embedded) are also provided.

To build the system natively JDK8 is required (typically OpenJDK8).  To run the war, Java 8 or greater should be fine. 

**Important note**:  The default memory for Tomcat and Jetty is insufficient to run Apollo (and most other web apps).   
You should [increase the memory according to these instructions](Troubleshooting.md#tomcat-memory).

Other possible [build settings for JBrowse](http://gmod.org/wiki/JBrowse_Configuration_Guide):
 
Ubuntu / Debian

     sudo apt-get install zlib1g zlib1g-dev libexpat1-dev libpng-dev libgd2-noxpm-dev build-essential git python-software-properties python make
    
RedHat / CentOS

     sudo apt-get install zlib zlib-dev expat-dev libpng-dev libgd2-noxpm-dev build-essential git python-software-properties python make
     
     
It is recommended to use the [default version of JBrowse or better](https://github.com/GMOD/Apollo/blob/develop/grails-app/conf/Config.groovy#L406) (though it does not work with JBrowse 2 yet).

There are [additional requirements](Apollo2Build.md) if doing development with Apollo.

### Install node and yarn

Node versions 6-12 have been tested and work.   [nvm](https://github.com/creationix/nvm) and ``nvm install 8``` is recommended.

    npm install -g yarn

### Install jdk
     
Build settings for Apollo specifically.  Recent versions of tomcat7 will work, though tomcat 8 and 9 are preferred.  If it does not install automatically there are a number of ways to [build tomcat on linux](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04):
     
    sudo apt-get install ant openjdk-8-jdk 
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/  # or set in .bashrc / .profile / .zshrc
    export JAVA_HOME=`/usr/libexec/java_home -v 1.8` # OR

    
If you need to have multiple versions of java (note [#2222](https://github.com/GMOD/Apollo/issues/2222)), you will need to specify the version for tomcat.  In tomcat8 on Ubuntu you'll need to set the `/etc/default/tomcat8` file JAVA_HOME explicitly:

    JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

Download Apollo from the [latest release](https://github.com/GMOD/Apollo/releases/latest/) under source-code and unzip.  
Test installation by running ```./apollo run-local``` and see that the web-server starts up on http://localhost:8080/apollo/.  
To setup for production continue onto configuration below after install . 


### Database configuration

Apollo supports several database backends, and you can choose sample configurations from using H2, Postgres, or
MySQL by default.

Each has a file called `sample-h2-apollo-config.groovy` or `sample-postgres-apollo-config.groovy` that is designed to be
renamed to apollo-config.groovy before running `apollo deploy`.   Additionally, you can also run via [docker](Docker.md).


Furthermore, the `apollo-config.groovy` has different groovy environments for test, development, and production modes.
The environment will be selected automatically selected depending on how it is run, e.g:

* `apollo deploy` use the production environment (i.e. when you copy the war file to your production
server `apollo run-local` or `apollo debug` use the development environment (i.e. when you are running it locally)
* `apollo test` uses the test environment (i.e. only when running unit tests)


#### Configure for H2:
- H2 is an embedded database engine, so no external setups are needed. Simply copy sample-h2-apollo-config.groovy to
  apollo-config.groovy.
    - The default dev environment (`apollo run-local` or `apollo run-app`) is in memory so you will have to change that to file.
- If you use H2 with tomcat or jetty in production you have to set the permissions for the file path in production correctly (e.g. `jdbc:h2:/mypath/prodDb`, `chown -u tomcat:tomcat /mypath/prodDb.*.db`).
    - If you use the local relative path `jdbc:h2:./prodDb` and tomcat8 the path will likely be: `/usr/share/tomcat8/prodDb*db`

#### Configure for PostgreSQL:
- Create a new database with postgres and add a user for production mode.  Here are [a few ways to do this in PostgreSQL](PostgreSQLSetup.md).
- Copy the sample-postgres-apollo-config.groovy to apollo-config.groovy. 


#### Configure for MySQL:
- Create a new MySQL database for production mode (i.e. run ``create database `apollo-production``` in the mysql
  console) and copy the sample-postgres-apollo-config.groovy to apollo-config.groovy.


#### Apollo in Galaxy
Apollo can always be used externally from Galaxy, but there are a few integrations available as well.

- [Using Docker compose](https://github.com/GMOD/docker-compose-galaxy-annotation).
- [From the Test Toolshed](https://testtoolshed.g2.bx.psu.edu/view/eric-rasche/apollo/df7a90763b3c).
- [Using the Galaxy Genome Annotation Toolsuite](https://github.com/galaxy-genome-annotation/docker-galaxy-genome-annotation).

### Database schema

After you startup the application, the database schema (tables, etc.) is automatically setup. You don't have to
initialize any database schemas yourself.

## Deploy the application

The `apollo run-local` command only launches a temporary server and should really not be used in production, so to
deploy to production, we build a new WAR file with the `apollo deploy` command. After you have setup your
`apollo-config.groovy` file, and it has the appropriate username, password, and JDBC URL in it, then we can run the
command:

``` 
./apollo deploy
```


This command will package the application and it will download any missing pre-requisites (jbrowse) into a WAR file in
the "target/" subfolder. After it completes, you can then copy the WAR file (e.g. ```apollo-2.0.4.war```) from the target folder
to the ```web-app``` folder of your [web container](https://en.wikipedia.org/wiki/Web_container#open_source_Web_containers) installation.
If you name the file ```apollo.war``` in your webapps folder, then you can access your app at "http://localhost:8080/apollo"

We test primarily on [Apache Tomcat (7.0.62+ and 8)](http://tomcat.apache.org/).  **Make sure to [set your Tomcat memory](https://github.com/GMOD/Apollo/blob/master/docs/Troubleshooting.md#tomcat-memory) to an appropriate size or Apollo will run slow / crash.**



Alternatively, as we alluded to previously, you can also launch a temporary instance of the server which is useful for
testing

``` 
./apollo run-local 8085
```

This temporary server will be accessible at "http://localhost:8085/apollo"

### Tomcat configuration

If you have tracks that have deep nested features that will result in a feature JSON larger than 10MB or if you have a client
 that sends requests to the Apollo server as JSON of size larger than 10MB then you will have to modify `src/war/templates/web.xml`.

Specifically the following block in `web.xml`:
```
    <context-param>
        <param-name>org.apache.tomcat.websocket.textBufferSize</param-name>
        <param-value>10000000</param-value>
    </context-param>
    <context-param>
        <param-name>org.apache.tomcat.websocket.binaryBufferSize</param-name>
        <param-value>10000000</param-value>
    </context-param>
```

Note: The `<param-value>` is in bytes.

### Memory configuration

Changing the memory used by Apollo in production must be [configured within Tomcat directly](Troubleshooting#tomcat-memory).

The default memory assigned to Apollo to run commands in Apollo is 2048 MB. This can be changed in your
`apollo-config.groovy` by uncommenting the memory configuration block:

```
// Uncomment to change the default memory configurations
grails.project.fork = [
        test   : false,
        // configure settings for the run-app JVM
        run    : [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024, forkReserve: false],
        // configure settings for the run-war JVM
        war    : [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024, forkReserve: false],
        // configure settings for the Console UI JVM
        console: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024]
]
```

### Note on database settings

If you use the `apollo run-local` command, then the "development" section of the apollo-config.groovy is used (or an
temporary in-memory H2 database is used if no apollo-config.groovy exists).

If you use the WAR file generated by the `apollo deploy` command on your own webserver, then the "production" section of
the apollo-config.groovy is used.

## Detailed build instructions


While the shortcut `apollo deploy` takes care of basic application deployment, understanding the full build process of
Apollo can help you to optimize and improve your deployed instances.

To learn more about the architecture of webapollo, view the [architecture guide](Architecture.md). 

