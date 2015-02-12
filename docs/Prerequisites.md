## Prerequisites

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Developer.md">On GitHub</a>


### Client Prerequisites

Web Apollo is a web-based application, so the only client side
requirement is a web browser. Web Apollo has been tested on Chrome, Firefox, and Safari
and matches the web browser requirements for JBrowse (see [jbrowse.org](http://jbrowse.org) for details).

### Server-side Prerequisites
Note: see the [Quick-start guide](Quick_start_guide.md) for the
quickest way to take care of pre-requisites.

-   System prerequisites
    -   Any Unix like system (e.g., Unix, Linux, Mac OS X)
    -   Servlet container (must support servlet spec 3.0+) [officially
        supported: Tomcat 7]
    -   Java 7+
    -   Maven3+ (most package managers will have this)
    -   Ant 1.8+ (most package managers will have this)
    -   Relational Database Management System [officially supported:
        PostgreSQL]
    -   Git
-   Data generation pipeline prerequisites (see [JBrowse
    prerequisites](http://gmod.org/wiki/JBrowse_Configuration_Guide) for more
    information)
    -   System packages
        -   libpng12-0 (optional, for JBrowse imagetrack)
        -   libpng12-dev (optional, for JBrowse imagetrack)
        -   zlib1g (Debian/Ubuntu)
        -   zlib1g-dev (Debian/Ubuntu)
        -   zlib (RedHat/CentOS)
        -   zlib-devel (RedHat/CentOS)
        -   libexpat1-dev (Debian/Ubuntu)
        -   expat-dev (RedHat/CentOS)
-   Perl prerequisites:
    -   Perl pre-requesites will be automatically installed by `install_jbrowse.sh`
    -   If you experience problems with this perl setup, please review setup.log and post to mailing list
-   Sequence search (optional)
    -   Blat (download
        [Linux](http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/)
        or
        [OSX](http://hgdownload.cse.ucsc.edu/admin/exe/macOSX.x86_64/|Mac)
        binaries)

#### Automatically get prerequisites

These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7 git
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-server postgresql-devel maven expat-devel tomcat git
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat git


