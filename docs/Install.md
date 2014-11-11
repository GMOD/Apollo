Introduction
------------

This guide will walk you through the server side installation for Web
Apollo. Web Apollo is a web-based application, so the only client side
requirement is a web browser. Note that Web Apollo has only been tested
on Chrome, Firefox, and Safari. It has not been tested with Internet
Explorer.

Quick start guide
-----------------

While there are a number of prerequisites to WebApollo, we hope that
this quick-start guide can help by automating some setup steps. This
"Quick start guide" can be used to initialize a "blank" machine with a
WebApollo instance from scratch.

This guide assumes that you have...
 - Installing system and perl prerequisites
 - Setting up a postgres user and database
 - Downloading WebApollo from github
 - Running the WebApollo build script
 - Creating a new data directory for the JBrowse data
 - Creating a new data directory for the WebApollo annotations
 - Configuring WebApollo using the config.properties file
More discussion of the particular configurations can be seen in the full
[installation guide](Install.md#installation "wikilink").

    # set some environmental variables
    export PGUSER=`whoami`
    export PGPASSWORD=password
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev cpanminus postgresql postgresql-server-dev-all postgresql-server nodejs-legacy git maven
    # install system prerequisites (centOS/redhat)
    sudo yum install epel-release
    sudo yum install cpanminus postgresql postgresql-devel git maven npm expat-devel
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install git maven node cpanminus postgresql wget

    # on centOS/redhat, manually init and start postgres (and make it start on OS boot using chkconfig)
    sudo su -c "service postgresql initdb && service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres (and make it start on OS boot with launchctl)
    ln -sfv /usr/local/opt/postgresql/\*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

    # setup cpanm and install jbrowse and webapollo perl prerequisites
    cpanm --local-lib=~/perl5 local::lib && eval $(perl -I ~/perl5/lib/perl5/ -Mlocal::lib)
    cpanm DateTime Text::Markdown Crypt::PBKDF2 DBI DBD::Pg

    # ubuntu/redhat/centOS - create a new postgres user and database for the webapollo instance. see (Authentication)[Install.md#authentication "wikilink"] section for more details
    sudo su postgres -c "createuser -RDIElPS $PGUSER"
    sudo su postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"
    # macOSX/homebrew, no need to createuser
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

    # clone Apollo repository and download sample data to WEB_APOLLO_ROOT/pyu_data
    git clone --depth 1 https://github.com/gmod/Apollo.git
    cd Apollo
    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

    # initialize PostgreSQL data base for sample data. Enter the password web_apollo_users_admin for firs tstep
    psql -U $PGUSER $WEBAPOLLO_DATABASE < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD

    # add the chromosome names to the webapollo database
    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a

    # build a compressed release package and install jbrowse binaries (also installs JBrowse's perl prerequisites using cpanm)
    make download-jbrowse
    ./install_jbrowse_bin.sh cpanm

    # setup jbrowse data directory in WEB_APOLLO_ROOT/data
    mkdir split_gff
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff
    prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    flatfile-to-json.pl --gff split_gff/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out data

    # add the webapollo plugin to the jbrowse config
    client/apollo/bin/add-webapollo-plugin.pl -i data/trackList.json

    # configure data directories using config.properties
    mkdir annotations
    echo jbrowse.data=`pwd`/data > config.properties 
    echo datastore.directory=`pwd`/annotations >> config.properties 
    echo database.url=jdbc:postgresql:$WEBAPOLLO_DATABASE >> config.properties
    echo database.username=$PGUSER >> config.properties
    echo database.password=$PGPASSWORD >> config.properties
    echo organism=$ORGANISM >> config.properties

    # can be used largely unmodified
    mv sample_config.xml config.xml

    # launch instance for testing, login to `http://localhost:8080/apollo` as web_apollo_admin:web_apollo_admin
    make clean release run

Note: you may have to shutdown any running instances of tomcat before
doing a run.sh for testing. Alternatively, continue to the [Deploying
the servlet](Install.md#deploying-the-servlet "wikilink") for instructions on
deploying to production.

Installation
------------

You can download the latest Web Apollo release as a
[tarball](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org] (not available for 1.x release branch yet). All
installation steps will be done through a shell. We'll be using Tomcat 7
as our servlet container and PostgreSQL as our relational database
management system. We'll use sample data from the Pythium ultimum
genome, provided as a [separate
download](http://icebox.lbl.gov/webapollo/data/pyu_data.tgz).

### Server operating system

Any Unix like system (e.g., Unix, Linux, Mac OS X)

### Prerequisites

Note: see the [Quick-start guide](Install.md#quick-start-guide "wikilink") for the
quickest way to take care of pre-requisites.

-   System prerequisites
    -   Servlet container (must support servlet spec 3.0+) [officially
        supported: Tomcat 7]
    -   Java 7+
    -   Maven3+ (most package managers will have this)
    -   Relational Database Management System [officially supported:
        PostgreSQL]
    -   Git
    -   NodeJS
-   Perl prerequisites that are required for a release build of
    jbrowse+webapollo i.e. "build.sh release" (note: other perl
    prerequisites are installed automatically by
    install\_jbrowse\_bin.sh. Also, "build.sh github" will not require
    these)
    -   DateTime
    -   Text::Markdown
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
-   Sequence search (optional)
    -   Blat (download
        [Linux](http://hgdownload.cse.ucsc.edu/admin/exe/linux.x86_64/)
        or
        [OSX](http://hgdownload.cse.ucsc.edu/admin/exe/macOSX.x86_64/|Mac)
        binaries)

### Tomcat memory

The memory requirements will depend on the the size of your genome and
how many instances of Web Apollo you host in the same Tomcat instance.
We recommend at least 1g for the heap size and 256m for the permgen size
as a starting point. Suggested settings are:

    export CATALINA_OPTS="-Xms512m -Xmx1g -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:+UseConcMarkSweepGC -XX:MaxPermSize=256m"

The location of your Tomcat environment configuration will be dependent
on how you installed it (manually vs using a package manager). It's
recommended that you add this configuration in
`$TOMCAT_BIN_DIR/setenv.sh` where `$TOMCAT_BIN_DIR` is where the
directory where the Tomcat binaries reside.

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

### Authentication

Postgres can use several different authentication methods, but here's a
short summary of the one's commonly seen

-   peer - allows shell based logins without a password (default for command line users)
-   ident - based off of operating system logins (not recommended)
-   md5 - basic password based-logins

The login methods are configured through the pg\_hba.conf file, and it
is instructive to see how they look on different systems. For example on
ubuntu/debian, the pg\_hba.conf file shows

    local   all             postgres                                peer

    # TYPE  DATABASE        USER            ADDRESS                 METHOD

    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            md5
    # IPv6 local connections:
    host    all             all             ::1/128                 md5

This allows command line logins to psql via the postgres user without
any password, as well as to other users. The logins over TCP/IP will use
password based authentication however, which is what we want for
WebApollo. On redhat/centOS however, the default pg\_hba.conf file is
not ideal for WebApollo, and uses ident by default:

    # TYPE  DATABASE        USER            ADDRESS                 METHOD

    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            ident
    # IPv6 local connections:
    host    all             all             ::1/128                 ident

It is important that on redhat, the IPv4 and IPv6 are changed from ident
to md5 so that perl scripts like add\_user.pl can work.

Additionally, if the owner of your web\_apollo\_users table will be a
non-operating system user, then you will want to add a special line for
it so that it does not use peer based authentication. Therefore, an
ideal pga.hba.conf would add a custom line for your POSTGRESUSER (e.g.
web\_apollo\_users\_admin) for local logins, and use md5 based logins
for host IPv4 and IPv6 lines

    # TYPE  DATABASE        USER            ADDRESS                 METHOD
    local   all             web_apollo_users_admin                  md5
    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            md5
    # IPv6 local connections:
    host    all             all             ::1/128                 md5

### User database

Web Apollo uses a database to determine who can access and edit
annotations for a given sequence.

First we’ll need to create a database. You can call it whatever you want
(remember the name as you’ll need to point the configuration to it). For
the purposes of this guide, we’ll call it `web_apollo_users` You might
want to create a separate account to manage the database. We’ll have the
user `web_apollo_users_admin` with password `web_apollo_users_admin` who
has database creation privilege. Depending on how your database server
is setup, you might not need to set a password for the user. See the
[PostgreSQL documentation](http://www.postgresql.org/docs) for more
information. We'll assume that the database is in the same server where
Web Apollo is being installed ("localhost"). These commands will be run
as the *postgres* user.

    $ sudo su postgres
    $ createuser -P web_apollo_users_admin
    Enter password for new role: 
    Enter it again: 
    Shall the new role be a superuser? (y/n) n
    Shall the new role be allowed to create databases? (y/n) y
    Shall the new role be allowed to create more new roles? (y/n) n

Next we'll create the user database.

    $ createdb -U web_apollo_users_admin web_apollo_users

If you get an authentication error, use the -W flag to get a password
prompt.

    $ createdb -U web_apollo_users_admin -W web_apollo_users

Now that the database is created, we need to load the schema to it.

    cd WEB_APOLLO_DIR/tools/user
    psql -U web_apollo_users_admin web_apollo_users < user_database_postgresql.sql

Now the user database has been setup.

Let's populate the database.

First we’ll create an user with access to Web Apollo. We’ll use the
`add_user.pl` script in `WEB_APOLLO_DIR/tools/user`. Let’s create an
user named `web_apollo_admin` with the password `web_apollo_admin`.

    ./add_user.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -u web_apollo_admin -p web_apollo_admin

Next we’ll add the annotation tracks ids for the genomic sequences for
our organism. We’ll use the `add_tracks.pl` script in the same
directory. We need to generate a file of genomic sequence ids for the
script. For convenience, there’s a script called
`extract_seqids_from_fasta.pl` in the same directory which will go
through a FASTA file and extract all the ids from the deflines. Let’s
first create the list of genomic sequence ids. We'll store it in
`~/scratch/seqids.txt`. We’ll want to add the prefix “Annotations-” to
each identifier.

    mkdir ~/scratch
    ./extract_seqids_from_fasta.pl -p Annotations- -i WEB_APOLLO_SAMPLE_DIR/scf1117875582023.fa -o ~/scratch/seqids.txt

Now we’ll add those ids to the user database.

    ./add_tracks.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -t ~/scratch/seqids.txt

Now that we have an user created and the annotation track ids loaded,
we’ll need to give the user permissions to access the sequence. We’ll
have the all permissions (read, write, publish, user manager). We’ll use
the `set_track_permissions.pl` script in the same directory. We’ll need
to provide the script a list of genomic sequence ids, like in the
previous step.

    ./set_track_permissions.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -u web_apollo_admin -t ~/scratch/seqids.txt -a

We’re all done setting up the user database.

Note that we’re only using a subset of the options for all the scripts
mentioned above. You can get more detailed information on any given
script (and other available options) using the “-h” or “--help” flag
when running the script.

Installing WebApollo
--------------------

From the top of inside the downloaded release, you need to run maven to
build a war file. This is then placed in tomcat's webapps directory.
Tomcat will be responsible extracting the file.

**IMPORTANT: the JBrowse data directories should no longer be placed
anywhere inside the Tomcat webapps folder, not even when using
symlinks!! The data directory should be created outside of the webapps
folder to avoid data loss when doing Undeploy operations!!**

### Before you build

You need to configure your instance using a config.properties and a
config.xml file, which are copied into the war file.

-   Copy the sample config / logging files to the right location.

    cd WEB_APOLLO_DIR
    cp sample_config.properties config.properties
    cp sample_config.xml config.xml
    cp sample_log4j2.json log4j2.json
    cp sample_log4j2-test.json log4j2-test.json

-   Edit the config.properties file and config.xml to point to the
    appropriate directories.
    -   Note: You *must* edit the config.properties file to point to
        your jbrowse data directory, e.g.
        jbrowse.data=/opt/apollo/jbrowse/data to point to your data
        directory. The other parameters are optional and can still be
        configured in your config.xml file (to comment out, prepend with
        a \#).

### Building the servlet

    cd WEB_APOLLO_DIR

There are a variety of targets available to build the war. For the
debug|release, make sure you have the prerequisites for building,
including NodeJS, , and

To generate a build, run deploy.sh with some optional parameters

     ./deploy.sh [release|debug|github|help]

This is used to generate a WAR file that is ready for deployment. The
parameters are for generating compiled javascript (release), unoptimized
javascript (debug) or unmodified straight-from-github deployments
(github).

The script creates a war file in the
WEB\_APOLLO\_DIR/target/apollo-1.x.war

### Install JBrowse binaries for WebApollo

For WebApollo, it is best to install the JBrowse binaries using the
following script:

     ./install_jbrowse_bin.sh [cpanm]

This will install the binaries to the system via cpan or cpanm. If you
are using cpanm, you can use environment variables to set specific
install directories, i.e.

     export PERL_CPANM_OPT="--local-lib=~/perl5"

Configuration
=============

Aside from the config.properties file, which is all that is essential to
get the database up and running, there are several other configuration
files that will reside in `WEB_APOLLO_DIR/config/` that can be used to
tailor parameters for your installation and setup BLAT,Chado,etc.
Additionally, adding evidence tracks and JBrowse configuration can be
performed. Please see
[WebApollo\_Configuration\_Guide\_1.x](WebApollo_Configuration_Guide_1.x "wikilink")
for details.

Deploying the servlet
=====================

After the war file is generated by the ./deploy.sh script in the
WEB\_APOLLO\_DIR/target directory (e.g. target/apollo-1.0.war), it can
be copied into the tomcat7 webapps directory:

-   cp WEB\_APOLLO\_DIR/target/apollo-1.x.war
    TOMCAT\_WEBAPPS\_DIR/WebApollo.war

We recommend to *NEVER* expand the war file manually or touch the
contents of the war file after deployment. The configuration steps
should be done in your checkout directory, and the configurations are
packaged into the war file by the ./deploy.sh script.

Accessing your WebApollo installation
=====================================

After copying your WAR file to the tomcat webapps directory, the app
will be automatically started. Let's test out our installation. Point
your browser to `
<nowiki>http://SERVER_ADDRESS:8080/WebApollo</nowiki> `.

The user name and password are both `web_apollo_admin` as we configured
earlier. Enter them into the login dialog.

We only see one reference sequence to annotate since we're only working
with one contig. Click on `scf1117875582023` under the `Name` column.

Now have fun annotating!!!

