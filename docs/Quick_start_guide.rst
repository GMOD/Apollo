Quick-start guide
=================

View On GitHub

While there are a number of prerequisites to WebApollo, we hope that
this quick-start guide can help by automating some setup steps. This
"Quick start guide" can be used to initialize a "blank" machine with a
WebApollo instance from scratch.

Checklist
---------

This guide will cover the following steps:

-  Downloading Web Apollo
-  Installing system pre-requisites
-  Using ``apollo deploy`` to setup environment + perl pre-requisites
-  Setting up a postgres user and database
-  Running JBrowse scripts to load data
-  Configuring Web Apollo with the config.properties file
-  Running a temporary server using ``apollo run``

Setup environment
^^^^^^^^^^^^^^^^^

First set some environmental variables. These are simply used for the
proceeding steps and don't require anything to be already setup.

::

    export WEB_APOLLO_DB_USER=web_apollo_users_admin
    export WEB_APOLLO_DB_PASS=password
    export WEB_APOLLO_USER=web_apollo_admin
    export WEB_APOLLO_PASS=password
    export WEB_APOLLO_DB=web_apollo_users
    export ORGANISM="Pythium ultimum"
    export JBROWSE_DATA_DIR=/opt/apollo/data
    export WEB_APOLLO_DATA_DIR=/opt/apollo/annotations

Note that WEB\_APOLLO\_DB\* is for the database credentials, and the
other settings are for the website login.

Get prerequisites
^^^^^^^^^^^^^^^^^

Then get some system pre-requisites. These commands will try to get
everything in one bang for several system types.

::

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7 git
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-server postgresql-devel maven expat-devel tomcat git
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat git

See `prerequisites <Prerequisites.md>`__ for more details on the
pre-requisites if you think something isn't working with these.

Download webapollo
^^^^^^^^^^^^^^^^^^

You can download the latest Web Apollo release from
`GitHub <https://github.com/gmod/Apollo.git>`__. We recommend cloning
the repo so that it is easy to receive upstream changes.

Example:

::

    # clone the latest webapollo from GitHub and use the latest release tag
    git clone https://github.com/GMOD/Apollo.git
    git checkout 1.0.3

Kickstart postgres (not needed for ubuntu)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

On debian/ubuntu, postgres is started and added to OS boot
automatically, but on other systems (centOS/redhat, mac OSX) they need
to be kickstarted.

::

    # on centOS/redhat, manually kickstart postgres and allow md5 type logins
    sudo su -c "PGSETUP_INITDB_OPTIONS='--auth-host=md5' postgresql-setup initdb"
    sudo su -c "service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres using launchctl (see homebrew guide for details)
    ln -sfv /usr/local/opt/postgresql/*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

For more details on setting up postgres in CentOS, refer to
https://wiki.postgresql.org/wiki/YUM_Installation (but note that we
encourage using host-based password authentication with --auth-host=md5)

For more details on setting up postgres in Homebrew refer to
https://wiki.postgresql.org/wiki/Homebrew

Initialize postgres database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After starting postgres, you can create a new database for managing
login and track information.

::

    # On debian/ubuntu/redhat/centOS, typically requires "postgres" user to execute commands
    sudo su - postgres -c "createuser -RDIElPS $WEB_APOLLO_DB_USER"
    sudo su - postgres -c "createdb -E UTF-8 -O $WEB_APOLLO_DB_USER $WEB_APOLLO_DB"

    # On macOSX/homebrew there is no need login as the "postgres" user
    createuser -RDIElPS $WEB_APOLLO_DB_USER
    createdb -E UTF-8 -O $WEB_APOLLO_DB_USER $WEB_APOLLO_DB

Note: see \ `database
setup <Database_setup.md#authentication>`__ for more details about the
database setup.

Download sample data
^^^^^^^^^^^^^^^^^^^^

If you are following our example, you can download the sample data here:

::

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

Setup some basic dependencies
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As a first step, use some of the default config files and run apollo
deploy. This will download and install jbrowse binaries.

::

    cp sample_config.properties config.properties
    cp sample_config.xml config.xml
    cp sample_log4j2.json log4j2.json
    cp sample_canned_comments.xml canned_comments.xml
    ./apollo deploy

If there are any errors during this build step, you can check setup.log.
See the `troubleshooting guide <Troubleshooting.md>`__ for common
issues.

Initialize Web Apollo logins and permissions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Initialize the database for logging into WebApollo as follows:

::

    psql -U $WEB_APOLLO_DB_USER $WEB_APOLLO_DB -h localhost < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEB_APOLLO_DB -U $WEB_APOLLO_DB_USER -P $WEB_APOLLO_DB_PASS -u $WEB_APOLLO_USER -p $WEB_APOLLO_PASS

Then we will add permissions on a track-by-track basis by first
extracting the seqids from a FASTA file and adding them to the database.
Carefully observe the arguments to these functions (particularly, adding
the -a option to set\_track\_permissions.pl allows "all" or "admin"
access, and the -p option for extract\_seqids\_from\_fasta is called the
Annotation prefix).

::

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEB_APOLLO_DB -U $WEB_APOLLO_DB_USER -P $WEB_APOLLO_DB_PASS -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEB_APOLLO_DB -U $WEB_APOLLO_DB_USER -P $WEB_APOLLO_DB_PASS -u $WEB_APOLLO_USER -t seqids.txt -a

Note: the reason we use psql with "-h localhost" is to force
password-based host authentication instead of peer authentication.

Setup genome browser data
^^^^^^^^^^^^^^^^^^^^^^^^^

Setup the JBrowse data directory with some of the sample data for
Pythium ultimum. Here, the split\_gff.pl script will separate the
example GFF based on source types, and then the JBROWSE\_DATA\_DIR will
be initialized with prepare-refseqs.pl and flatfile-to-json.pl.

First initialize the directories for storing JBrowse and Annotation
data:

::

    sudo mkdir -p $WEBAPOLLO_DATA_DIR
    sudo mkdir -p $JBROWSE_DATA_DIR
    sudo chown 755 -R $WEBAPOLLO_DATA_DIR
    sudo chown 755 -R $JBROWSE_DATA_DIR

Then you can output some data for the JBrowse data directory with
prepare-refseqs.pl and flatfile-to-json.pl. The
split\_gff\_by\_source.pl script is used to make the example GFF file
separate into sources, so that we can load just the MAKER annotations:

::

    mkdir temp
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d temp
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    bin/flatfile-to-json.pl --gff  temp/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out $JBROWSE_DATA_DIR

For more info on loading data, see the `configuration
guide <Configure.md>`__ guide.

Add the plugin
''''''''''''''

Once the tracks are initialized, the plugin needs to be added to
the JBrowse configuration using the add-webapollo-plugin.pl script,
which takes as input a trackList.json file.

::

    client/apollo/bin/add-webapollo-plugin.pl -i $JBROWSE_DATA_DIR/trackList.json

Update config.properties
^^^^^^^^^^^^^^^^^^^^^^^^

Once we have our data directories and database configuration setup, we
can put this information in the config.properties file.

::

    echo jbrowse.data=$JBROWSE_DATA_DIR > config.properties
    echo datastore.directory=$WEBAPOLLO_DATA_DIR >> config.properties
    echo database.url=jdbc:postgresql:$WEB_APOLLO_DB >> config.properties
    echo database.username=$WEB_APOLLO_DB_USER >> config.properties
    echo database.password=$WEB_APOLLO_DB_PASS >> config.properties
    echo organism=$ORGANISM >> config.properties

Note: the organism property should be a two-word "genus species" ID for
proper chado exports. For more details on chado export see the
`configuration guide <Configure.md>`__.

Launch a temporary Web Apollo instance
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After this setup, you are ready to deploy a new instance.

::

    ./apollo run

This will launch a temporary tomcat instance that you will be able to
access from http://localhost:8080/apollo/ and login with your
$WEB\_APOLLO\_USER and $WEB\_APOLLO\_PASS information.

Congratulations
^^^^^^^^^^^^^^^

If everything works, then you can continue to the `build
guide <Build.md>`__ for more instructions on packaging the build, and to
the `deployment guide <Deploy.md>`__ for information about deploying to
a production server. Additionally, information about configuration and
adding Chado export can be found in the `configuration
guide <Configure.md>`__. More information about loading additional
tracks is available in the `data loading <Data_loading.md>`__ section.
