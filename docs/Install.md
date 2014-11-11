Introduction
------------

This guide will walk you through the server side installation for Web
Apollo. Web Apollo is a web-based application, so the only client side
requirement is a web browser. Note that Web Apollo has only been tested
on Chrome, Firefox, and Safari. It has not been tested with Internet
Explorer.

Installation
------------

You can download the latest Web Apollo release as a
[tarball](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org] (not available for 1.x release branch yet).

All installation steps will be done through a shell. We'll be using Tomcat 7
as our servlet container and PostgreSQL to manage authentication data.

We'll use sample data from the Pythium ultimum genome, provided as a
[separate download](http://icebox.lbl.gov/webapollo/data/pyu_data.tgz).

### Server operating system

Any Unix like system (e.g., Unix, Linux, Mac OS X)

### Prerequisites

Note: see the [Quick-start guide](Quick_start_guide.md "wikilink") for the
quickest way to take care of pre-requisites.

-   System prerequisites
    -   Servlet container (must support servlet spec 3.0+) [officially
        supported: Tomcat 7]
    -   Java 7+
    -   Maven3+ (most package managers will have this)
    -   Relational Database Management System [officially supported:
        PostgreSQL]
    -   Git
-   Perl prerequisites for WebApollo's authorization scripts
    -   Crypt::PBKDF2
    -   DBI
    -   DBD::Pg
-   Data generation pipeline prerequisites (see [JBrowse
    prerequisites](http://gmod.org/wiki/JBrowse_Configuration_Guide "wikilink") for more
    information on its prerequisites)
    -   System packages
        -   libpng12-0 (optional, for JBrowse imagetrack)
        -   libpng12-dev (optional, for JBrowse imagetrack)
        -   zlib1g (Debian/Ubuntu)
        -   zlib1g-dev (Debian/Ubuntu)
        -   zlib (RedHat/CentOS)
        -   zlib-devel (RedHat/CentOS)
        -   libexpat1-dev (Debian/Ubuntu)
        -   expat-dev (RedHat/CentOS)
-   Sequence search (optional)
    -   Blat (download
        [Linux](http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/)
        or
        [OSX](http://hgdownload.cse.ucsc.edu/admin/exe/macOSX.x86_64/|Mac)
        binaries)
### Conventions

This guide will use the following conventions to make it more concise
(you might want to keep these convention definitions handy so that you
can easily reference them as you go through this guide):

-   WEB\_APOLLO\_DIR
    -   Location where the tarball was uncompressed and will include
        `WebApollo-RELEASE_DATE` (e.g.,
        `~/webapollo/WebApollo-2012-10-08`)
-   WEB\_APOLLO\_SAMPLE\_DIR
    -   Location where the sample tarball was uncompressed (e.g.,
        `~/webapollo/webapollo_sample`)
-   WEB\_APOLLO\_DATA\_DIR
    -   Location for WebApollo annotations (e.g.,
        `/data/webapollo/annotations`)
-   JBROWSE\_DATA\_DIR
    -   Location for JBrowse data (e.g., `/data/webapollo/jbrowse/data`)
-   TOMCAT\_WEBAPPS\_DIR
    -   Location where deployed servlets for Tomcat go (e.g.,
        `/var/lib/tomcat7/webapps`)
-   BLAT\_DIR
    -   Location where the Blat binaries are installed (e.g.,
        `/usr/local/bin`)
-   BLAT\_TMP\_DIR
    -   Location for temporary Blat files (e.g.,
        `/data/webapollo/blat/tmp`)
-   BLAT\_DATABASE
    -   Location for the Blat database (e.g.,
        `/data/webapollo/blat/db/pyu.2bit`)

The Tomcat related paths are the ones used by default in Ubuntu 12.04
and Ubuntu's provided Tomcat7 package. Paths will likely be different in
your system depending on how Tomcat was installed.


