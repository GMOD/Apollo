# Apollo

![Apollo Logo](https://github.com/GMOD/docker-apollo/raw/master/img/ApolloLogo_100x36.png)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2573060.svg)](https://doi.org/10.5281/zenodo.2573060)




> Apollo is a browser-based tool for visualisation and editing of sequence
> annotations. It is designed for distributed community annotation efforts,
> where numerous people may be working on the same sequences in geographically
> different locations; real-time updating keeps all users in sync during the
> editing process.

## Running the Container

The container is publicly available as `gmod/apollo:latest`. 

There are a large number of environment variables that can be adjusted to suit
your site's needs. These can be seen in the
[apollo-config.groovy](https://github.com/GMOD/Apollo/blob/master/sample-docker-apollo-config.groovy)
file.

## Quickstart

This procedure starts tomcat in a standard virtualized environment with a PostgreSQL database with [Chado](http://gmod.org/wiki/Introduction_to_Chado).

Install [docker](https://docs.docker.com/engine/installation/) for your system if not previously done.

Choose an option:

- To test a versioned release to test installation, e.g.: `docker run -it -p 8888:8080  -v /directory/to/jbrowse/files:/data quay.io/gmod/docker-apollo:2.3.1`  [Other available versions](https://quay.io/repository/gmod/docker-apollo?tab=tags)

- Install a latest release to test installation: `docker run -it -p 8888:8080 -v /directory/to/jbrowse/files:/data gmod/apollo:latest` 
  -  To make sure you have the latest pull with ```docker pull gmod/apollo``` to fetch newer versions
  
## Production

- To **run in production** against **persistent** JBrowse data and a **persistent** database you should:
    - Create an empty directory for database data, e.g. `postgres-data`.
    - Put JBrowse data in a directory, e.g. `/jbrowse/root/directory/`.
    - `docker run -it -v /jbrowse/root/directory/:/data -v /postgres/data/directory:/var/lib/postgresql -p 8888:8080 quay.io/gmod/docker-apollo:latest`
    
- See [docker run instructions](https://docs.docker.com/engine/reference/run/) to run as a daemon (`-d`) and with a fresh container each time (`--rm`) depending on your use-case.

- You can run production using the build created by quay.io instead (https://quay.io/repository/gmod/docker-apollo):
    - `docker run -it -v /jbrowse/root/directory/:/data -v postgres-data:/var/lib/postgresql  -p 8888:8080 quay.io/gmod/docker-apollo:latest`

You can configure options if need be (though default will work) by setting environmental variables for [apollo-config.groovy](https://github.com/GMOD/docker-apollo/blob/master/apollo-config.groovy) by passing through via [multiple `-e` parameters](https://vsupalov.com/docker-arg-env-variable-guide/) :

    - `docker run -it -e APOLLO_ADMIN_PASSWORD=superdupersecrect -v /jbrowse/root/directory/:/data -v postgres-data:/var/lib/postgresql  -p 8888:8080 quay.io/gmod/docker-apollo:latest`

In all cases, Apollo will be available at [http://localhost:8888/](http://localhost:8888/) (or 8888 if you don't configure the port)

When you use the above mount directory ```/jbrowse/root/directory``` and your genome is in 
```/jbrowse/root/directory/myawesomegenome``` you'll point to the directory: ```/data/myawesomegenome```.

- Change the root path of the url (e.g., <http://localhost:8888/otherpath>) by adding the argument `-e APOLLO_PATH=otherpath` when running.

NOTE: If you don't use a locally mounted PostgreSQL database (e.g., creating an empty directory and mounting using `-v postgres-data:/var/lib/postgresql`)
or [set appropriate environment variables](https://docs.docker.com/engine/reference/commandline/run/) for a remote database 
( see variables [defined here](https://github.com/GMOD/docker-apollo/blob/master/launch.sh)) your annotations and setup will not be persisted.

### Logging In

The default credentials in this image are:

| Credentials |                    |
| ---         | ------------------ |
| Username    | `admin@local.host` |
| Password    | `password`         |


### Example Workflow


1. Make the following directories somewhere with write permissions: `postgres-data` and `jbrowse-data`. 
1. Copy your jbrowse data into `jbrowse-data`.  We provide [working sample data](http://genomearchitect.readthedocs.io/en/latest/Apollo2Build.html#adding-sample-data).
1. Run the docker-command:  `docker run -it -v /absolute/path/to/jbrowse-data:/data -v /absolute/path/to/postgres-data:/var/lib/postgresql -p 8888:8080 quay.io/gmod/docker-apollo:latest`
1. Login to the server at `http://localhost:8888/`
1. Add an organism per the [instructions under Figure 2](http://genomearchitect.readthedocs.io/en/latest/Apollo2Build.html#login-to-the-web-interface).   Using yeast as an example, if you copy the data into `jbrowse-data/yeast` then on the server 
you'll add the directory: `/data/yeast`. 

![](./img/small-sample.png)

## Apollo Run-time OPTIONS

Apollo run-time options are specified in the [createenv.sh](createenv.sh) file.  

These are picked up in the [apollo-config.groovy](apollo-config.groovy) file and follows the rules of [regular apollo configuration](https://github.com/GMOD/Apollo/blob/develop/docs/Configure.md). 


Special cases include CHADO.  By default it is on, but use `WEBAPOLLO_USE_CHADO=false` to turn off. 

