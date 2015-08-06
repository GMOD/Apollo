# Quick-start guide

Here we will introduce how to setup WebApollo on your server. In general, there are two modes of deploying WebApollo.

There is "development mode" where the application is launched in a temporary server (automatically) and there is "production mode", which will typically require an external separate database and tomcat server where you can deploy the generated `war` file.

This guide will cover the "development mode" scenario which is easy to start. See the [setup](Setup) guide to cover the "production mode" setup.

## Install prerequisites

You will minimally need to install Java (Oracle or OpenJDK's version, Java (7 or greater), ([Grails](https://grails.org/), [git](https://git-scm.com/), [ant](http://ant.apache.org/), and a Java web server (servlet container) which is generally [tomcat, minimally 7.0.28](http://tomcat.apache.org/), jetty, or resin. An external database such as PostgreSQL or MySQL is generally used for production, but an embedded H2 database will also be used in this guide for ease of deployment.

See the [pre-requisites](Prerequisites.md) for more details.


### Grails

Installing grails is made easier by using [GVM](http://gvmtool.net/) which can automatically setup grails for you. We will use grails 2.4.5 for Web Apollo

1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.5

### Get the code

To setup WebApollo, you can download the code from github:

- git clone https://github.com/GMOD/Apollo.git Apollo
- cd Apollo


### Verify install requirements

We can now perform a quick-start of the application in "development mode" with this command:

```
    apollo run-local
```

The jbrowse and perl pre-requisites will be installed during this step, but if there is a success, then a temporary server will be automatically launched at `http://localhost:8080/apollo`.

Note: You can also supply a port number e.g. `apollo run-local 8085` if there are conflicts on port 8080.

## Setting up the application


After we have a server setup, we will want to add a new organism to the panel. If you are a new user, you will want to setup this data with the jbrowse pre-processing scripts. You can see the [data loading guide](Data_loading.md) for more details, but essentially, you will want to load a reference genome and an annotations file at a minimum:

```
    bin/prepare-refseqs.pl --fasta yourgenome.fasta --out /some/directory
    bin/flatfile-to-json.pl --gff yourannotations.fasta --type mRNA --trackLabel Annotations --out /some/directory
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

If you completed this setup, you can then begin adding new users and performing annotations. Please continue to the [setup guide](Setup.md) for deploying the webapp to production or visit the [troubleshooting guide](Troubleshooting.md) if you encounter problems during setup.
