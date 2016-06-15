# Quick-start Developer's guide

Here we will introduce how to setup Apollo on your server. In general, there are two modes of deploying Apollo.

There is "development mode" where the application is launched in a temporary server (automatically) and there is
"production mode", which will typically require an external separate database and tomcat server where you can deploy the
generated `war` file.

This guide will cover the "development mode" scenario which should be easy to start.  **To setup in a production environment, please see the [setup](Setup.md) guide.**


### Grails

Installing Grails is made easier by using [SDKMAN](http://sdkman.io/) (formerly GVM) which can automatically setup
grails for you. 

1. `curl -s http://get.sdkman.io | bash`
2. `sdk install grails 2.5.4`
3. `sdk install gradle 2.11`
 
If you don't install Grails or Gradle using SDKMAN, these will automatically be downloaded in the current directory by executing grailsw or gradlew locally.

### Groovy

It is not required but you can also install the groovy command line

`sdk install groovy`


### Get the code

To setup Apollo, you can download our [latest release](https://github.com/GMOD/Apollo/releases/latest) from our [official releases](https://github.com/GMOD/Apollo/releases/).   Version 2.0.2 is available as a [.tar.gz](https://github.com/GMOD/Apollo/archive/2.0.2.tar.gz) or a [.zip](https://github.com/GMOD/Apollo/archive/2.0.2.zip) file. 

Alternatively you can check it out from git as directly as follows:

1. git clone https://github.com/GMOD/Apollo.git Apollo
1. cd Apollo
1. git checkout 2.0.3  # otherwise you will be getting the more active / less tested master branch


### Verify install requirements

We can now perform a quick-start of the application in "development mode" with this command:

``` 
./apollo run-local
```

The jbrowse and perl pre-requisites will be installed during this step, and if there is a success, then a temporary
server will be automatically launched at `http://localhost:8080/apollo`.

Note: You can also supply a port number e.g. `apollo run-local 8085` if there are conflicts on port 8080.

Also note: if there are any errors at this step, check the setup.log file for errors. You can refer to the
[troubleshooting guide](Troubleshooting.md) and often it just means the pre-requisites or perl modules failed.

Also also note: the "development mode" uses an in-memory H2 database for storing data by default. The setup guide will
show you how to configure custom database settings.


## Setting up the application

### Setup a production server

**To setup in a production environment, please see the [setup](Setup.md) guide.**  To setup (as opposed to a development server as above), you must [properly configure a servlet container like Tomcat or Jetty](Setup.md) with [sufficient memory](Troubleshooting.md#tomcat-memory).

### Adding data to Apollo

After we have a server setup, we will want to add a new organism to the panel. If you are a new user, you will want to
setup this data with the jbrowse pre-processing scripts. You can see the [data loading guide](Data_loading.md) for more
details, but essentially, you will want to load a reference genome and an annotations file at a minimum:

``` 
bin/prepare-refseqs.pl --fasta yourgenome.fasta --out /opt/apollo/data

bin/flatfile-to-json.pl --gff yourannotations.gff --type mRNA \
        --trackLabel AnnotationsGff --out /opt/apollo/data
```


### Login to the web interface

After you access your application at http://localhost:8080/apollo/ then you will be prompted for login information

![Login first time](images/1.png)

Figure 1. "Register First Admin User" screen allows you to create a new admin user.


![Organism configuration](images/2.png)

Figure 2. Navigate to the "Organism tab" and select "Create new organism". Then enter the new information for your
organism. Importantly, the data directory refers to a directory that has been prepared with the JBrowse data loading
scripts from the command line. See the [data loading](Data_loading.md) section for details.

![Open annotator](images/3.png)

Figure 3. Open up the new organism from the drop down tab on the annotator panel.



## Conclusion

If you completed this setup, you can then begin adding new users and performing annotations. Please continue to the
[setup guide](Setup.md) for deploying the webapp to production or visit the [troubleshooting guide](Troubleshooting.md)
if you encounter problems during setup.
