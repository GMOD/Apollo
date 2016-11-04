# Setup guide



The quick-start guide showed how to quickly launch a temporary instance of Apollo, but deploying the application to
production normally involves some extra steps.


The general idea behind your deployment is to create a `apollo-config.groovy` file from some existing sample files which
have sample settings for various database engines.


## Production pre-requisites

You will minimally need to have Java 7 or greater, [Grails](https://grails.org/), [git](https://git-scm.com/),
[ant](http://ant.apache.org/), a servlet container e.g. [tomcat7+](http://tomcat.apache.org/), jetty, or resin. An
external database such as PostgreSQL or MySQL is generally used for production, but instructions for the H2 database is
also provided.

**Important note**:  The default memory for Tomcat and Jetty is insufficient to run Apollo (and most other web apps).   
You should [increase the memory according to these instructions](Troubleshooting.md#tomcat-memory).

Other possible [build settings for JBrowse](http://gmod.org/wiki/JBrowse_Configuration_Guide) (based on an Ubuntu 16 install):

     sudo apt-get update && sudo apt-get install zlib1g-dev libpng-dev libgd2-noxpm-dev build-essential git python-software-properties 
     curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
     sudo apt-get install nodejs 
     
Note that npm (installed with nodejs) must be version 2 or better.  If not installed from the above instructions, most [stable versions of node.js](https://nodejs.org/en/download/package-manager/) will supply this.  

    sudo npm install -g bower 
     
Build settings for Apollo specifically.  Recent versions of tomcat7 will work, though tomcat8 is preferred.  If it does not install automatically there are a number of ways to [build tomcat on linux](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04):
     
    sudo apt-get install ant openjdk-8-jdk 
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/  # or set in .bashrc / .project

Download Apollo from the [latest release](https://github.com/GMOD/Apollo/releases/latest/) under source-code and unzip.  Test installation by running ```./apollo run-local``` and see that the web-server starts up on http://localhost:8080/apollo/.  To setup for production continue onto configuration below after install . 

### Database configuration

Apollo supports several database backends, and you can choose sample configurations from using H2, Postgres, or
MySQL by default.

Each has a file called `sample-h2-apollo-config.groovy` or `sample-postgres-apollo-config.groovy` that is designed to be
renamed to apollo-config.groovy before running `apollo deploy`. Additionally there is a
`sample-docker-apollo-config.groovy` which allows control of the configuration via environment variables.

Furthermore, the `apollo-config.groovy` has different groovy environments for test, development, and production modes.
The environment will be selected automatically selected depending on how it is run, e.g:

* `apollo deploy` or `apollo release` use the production environment (i.e. when you copy the war file to your production
server `apollo run-local` or `apollo debug` use the development environment (i.e. when you are running it locally)
* `apollo test` uses the test environment (i.e. only when running unit tests)



#### Configure for H2:
- H2 is an embedded database engine, so no external setups are needed. Simply copy sample-h2-apollo-config.groovy to
  apollo-config.groovy.

#### Configure for PostgreSQL:
- Create a new database with postgres and add a user for production mode.  Here are [a few ways to do this in PostgreSQL](PostgreSQLSetup.md).
- Copy the sample-postgres-apollo-config.groovy to apollo-config.groovy. 



#### Configure for MySQL:
- Create a new MySQL database for production mode (i.e. run ``create database `apollo-production``` in the mysql
  console) and copy the sample-postgres-apollo-config.groovy to apollo-config.groovy.


#### Configure for Docker:
- Set up and export all of the environment variables you wish to configure. At bare minimum you will likely wish to set
  `WEBAPOLLO_DB_USERNAME`, `WEBAPOLLO_DB_PASSWORD`, `WEBAPOLLO_DB_DRIVER`, `WEBAPOLLO_DB_DIALECT`, and
`WEBAPOLLO_DB_URI`
- Create a new database in your chosen database backend and copy the sample-docker-apollo-config.groovy to
  apollo-config.groovy.
- [Instructions and a script for launching docker with apollo and PostgreSQL](https://github.com/GMOD/docker-apollo).

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


### Note on database settings

If you use the `apollo run-local` command, then the "development" section of the apollo-config.groovy is used (or an
temporary in-memory H2 database is used if no apollo-config.groovy exists).

If you use the WAR file generated by the `apollo deploy` command on your own webserver, then the "production" section of
the apollo-config.groovy is used.

## Detailed build instructions


While the shortcut `apollo deploy` takes care of basic application deployment, understanding the full build process of
Apollo can help you to optimize and improve your deployed instances.

To learn more about the architecture of webapollo, view the [architecture guide](Architecture.md) but the main idea here
is to learn how to use `apollo release` to construct a build that includes javascript minimization


### Pre-requisites for Javascript minimization

In addition to the system [pre-requisites](Prerequisites.md), the javascript compilation will use nodejs, which can be
installed from a package manager on many platforms. Recommended setup for different platforms:


``` 
sudo apt-get install nodejs
sudo yum install epel-release npm
brew install node
```

#### Install extra perl modules

Building apollo in release mode also requires some extra Perl modules, namely Text::Markdown and DateTime. One way to
install them:

``` 
bin/cpanm -l extlib DateTime Text::Markdown
```

### Performing the javascript minimization

To build a Apollo release with Javascript minimization, you can use the command

``` 
./apollo release
```

This will compile JBrowse and Apollo javascript code into minimized files so that the number of HTTP requests that the
client needs to make are reduced.

In all other respects, `apollo release` is exactly the same as `apollo deploy` though.


### Performing active development

To perform active development of the codebase, use

``` 
./apollo debug
```

This will launch a temporary instance of Apollo by running `grails run-app` and `ant devmode` at the same time,
which means that any changes to the Java files will be picked up, allowing fast iteration.

If you modify the javascript files (i.e. the client directory), you can run `scripts/copy_client.sh` and these will be
picked up on-the-fly too.


