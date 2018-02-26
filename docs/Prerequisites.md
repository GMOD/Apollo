## Pre-requisites


### Client pre-requisites

Apollo is a web-based application, so the only client side
requirement is a web browser. Apollo has been tested on Chrome, Firefox, and Safari
and matches the web browser requirements for JBrowse (see [jbrowse.org](http://jbrowse.org) for details).

### Server-side pre-requisites

Note: see the [Apollo 2.x quick-start](Apollo2Build.md) for the
quickest way to take care of pre-requisites.

-   System pre-requisites (see quick-start guide for simple setup)
    -   Any Unix like system (e.g., Unix, Linux, Mac OS X).
    -   Servlet container (must support servlet spec 3.0+) such as tomcat 8 for production (not needed for development).
    -   Java 8+  OpenJDK or Oracle should work.
    -   [npm 2.X or better / node.js](https://nodejs.org/en/download/package-manager/)
    -   Grails (optional, but good for development).   The easiest way to install is using sdkman, see [Apollo 2.x quick-start](Apollo2Build.md) for this step).
    -   Ant 1.8+ (most package managers will have this).
    -   A database (RDMS) system. Sample configurations for PostgreSQL and MySQL are available. H2 configuration does not require any manual installation.
    -   Basic tools like Git, Curl, a text editor, etc.
-   Data generation pipeline pre-requisites (for full list see http://gmod.org/wiki/JBrowse_Configuration_Guide)
    -   System packages:
        -   libpng12-0 (optional, for JBrowse imagetrack)
        -   libpng12-dev (optional, for JBrowse imagetrack)
        -   zlib1g (Debian/Ubuntu)
        -   zlib1g-dev (Debian/Ubuntu)
        -   zlib (RedHat/CentOS)
        -   zlib-devel (RedHat/CentOS)
        -   libexpat1-dev (Debian/Ubuntu)
        -   expat-dev (RedHat/CentOS)
-   Perl pre-requisites:
    -   Apollo will automatically try to install all perl-pre-requisites.
    -   If you are building Apollo in "release" mode, perl 5.10 or up will be required
-   Sequence search (optional).
    -   Blat (download [Linux](http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/) or [OSX](http://hgdownload.cse.ucsc.edu/admin/exe/macOSX.x86_64/) binaries).

#### Package manager commands

To install system pre-requisites, you can try the following commands


##### Debian/Ubuntu 16

`sudo apt-get install openjdk-8-jdk curl libexpat1-dev postgresql postgresql-server-dev-all tomcat8 git`

##### CentOS/RedHat

`sudo yum install postgresql postgresql-server postgresql-devel expat-devel tomcat git curl`

##### MacOSX/Homebrew

`brew install postgresql tomcat git`


