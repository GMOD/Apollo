# Quick start guide

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Quick_start_guide.md">On GitHub</a>

While there are a number of prerequisites to WebApollo, we hope that
this quick-start guide can help by automating some setup steps. This
"Quick start guide" can be used to initialize a "blank" machine with a
WebApollo instance from scratch.

## Checklist

This guide will cover the following steps:

 - Downloading Web Apollo
 - Installing system pre-requisites
 - Using `apollo deploy` to setup environment + perl pre-requisites
 - Setting up a postgres user and database
 - Running JBrowse scripts to load data
 - Configuring Web Apollo with the config.properties file
 - Running a temporary server using `apollo run`

#### Setup environment

First set some environmental variables. These are simply used for the proceeding steps and don't require anything to be already setup.

    export PGUSER=web_apollo_users_admin
    export PGPASSWORD=password
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"
    export JBROWSE_DATA_DIR=`pwd`/data
    export WEBAPOLLO_DATA_DIR=`pwd`/annotations


#### Download webapollo

You can download the latest Web Apollo release from [GitHub](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org](http://genomearchitect.org) (the 1.x release branch is not available from genomearchitect yet).

Example:

    # clone the latest webapollo from GitHub and use the latest release tag
    git clone https://github.com/GMOD/Apollo.git
    git checkout 1.0.3


#### Get prerequisites

Then get some system pre-requisites. These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7 git
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-server postgresql-devel maven expat-devel tomcat git
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat git


See [prerequisites](Prerequisites.md) for more details on the pre-requisites if you think something isn't working with these.

#### Kickstart postgres (not needed for ubuntu)

On debian/ubuntu, postgres is started and added to OS boot automatically, but on other systems (centOS/redhat, mac OSX) they need to be kickstarted. 

    # on centOS/redhat, manually kickstart postgres and allow md5 type logins
    sudo su -c "PGSETUP_INITDB_OPTIONS='--auth-host=md5' postgresql-setup initdb"
    sudo su -c "service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres using launchctl (see homebrew guide for details)
    ln -sfv /usr/local/opt/postgresql/*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist


For more details on setting up postgres in CentOS, refer to [https://wiki.postgresql.org/wiki/YUM_Installation](https://wiki.postgresql.org/wiki/YUM_Installation) (but note that we encourage using host-based password authentication with --auth-host=md5)

For more details on setting up postgres in Homebrew refer to [https://wiki.postgresql.org/wiki/Homebrew](https://wiki.postgresql.org/wiki/Homebrew)

#### Initialize postgres database

After starting postgres, you can create a new database for managing login and track information.

    # On debian/ubuntu/redhat/centOS, typically requires "postgres" user to execute commands
    sudo su - postgres -c "createuser -RDIElPS $PGUSER"
    sudo su - postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"

    # On macOSX/homebrew there is no need login as the postgres user
    createuser -RDIElPS $PGUSER
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

Note: see [database setup](Database_setup.md#authentication) for more details about the database setup.
 
#### Download sample data

If you are following our example, you can download the sample data here:

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

#### Setup some basic dependencies

We will use the `apollo deploy` script to initialize jbrowse and install some basic perl pre-requisites. We can use simply copy the default configs when initializing setups.

    ./install_jbrowse.sh
    
If there are any errors during this build step, you can check setup.log. See the [troubleshooting guide](Troubleshooting.md) for common issues.

Also, if you are using your own custom JBrowse repository, point to it using ./install_jbrowse.sh /location/of/repo and it will simply install the binaries and perl pre-requisites locally using cpanm.


#### Initialize Web Apollo logins and permissions


Initialize the database for logging into WebApollo as follows:

    psql -U $PGUSER $WEBAPOLLO_DATABASE -h localhost < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD


Then we will add permissions on a track-by-track basis by first extracting the seqids from a FASTA file and adding them to the database. Carefully observe the arguments to these functions (particularly, adding the -a option to set_track_permissions.pl allows "all" or "admin" access, and the -p option for extract_seqids_from_fasta is called the Annotation prefix).

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a

Note: the reason we use psql with "-h localhost" is to force password-based host authentication instead of peer authentication.

#### Setup genome browser data

Setup the JBrowse data directory with some of the sample data for Pythium ultimum. Here, the split_gff.pl script will separate the example GFF based on source types, and then the JBROWSE_DATA_DIR will be initialized with prepare-refseqs.pl and flatfile-to-json.pl.

    mkdir $WEBAPOLLO_DATA_DIR
    mkdir $JBROWSE_DATA_DIR
    mkdir temp
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d temp
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    bin/flatfile-to-json.pl --gff  temp/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out $JBROWSE_DATA_DIR

    
For more info on adding genome browser tracks, see the [configuration guide](Configure.md) guide.


##### Add webapollo plugin to the genome browser
Once the tracks are initialized, the webapollo plugin needs to be added to the JBrowse configuration using the add-webapollo-plugin.pl script.

    client/apollo/bin/add-webapollo-plugin.pl -i $JBROWSE_DATA_DIR/trackList.json

#### Update config.properties

Once we have our data directories and database configuration setup, we can put this information in the config.properties file.


    echo jbrowse.data=$JBROWSE_DATA_DIR > config.properties
    echo datastore.directory=$WEBAPOLLO_DATA_DIR >> config.properties
    echo database.url=jdbc:postgresql:$WEBAPOLLO_DATABASE >> config.properties
    echo database.username=$PGUSER >> config.properties
    echo database.password=$PGPASSWORD >> config.properties
    echo organism=$ORGANISM >> config.properties


Note: the organism property should be a two-word "genus species" ID for proper chado exports. For more details on chado export see the [configuration guide](Configure.md).

#### Launch a temporary Web Apollo instance

After this setup, you are ready to deploy a new instance.

#### Run a test server

After this setup, you are ready to deploy a new instance. You can copy the basic config files and run the instance on a temporary server:

     cp sample_canned_comments.xml canned_comments.xml
     cp sample_config.properties config.properties
     cp sample_config.xml config.xml
     cp sample_log4j2.json log4j2.json
    ./apollo run

This will launch a temporary tomcat instances that you will be able to access from http://localhost:8080/apollo/ and login with your $WEBAPOLLO_USER and $WEBAPOLLO_PASSWORD information.

Note: if you already have a tomcat instance running, you may have to shut it down to launch the test server.

#### Congratulations

If everything works, then you can use `apollo deploy` and then copy the WAR file into a production instance. You can also continue to the [build guide](Build.md) for more instructions on packaging the build, and to the [deployment guide](Deploy.md) for information about deploying to a production server. Additionally, information about configuration and adding Chado export can be found in the [configuration guide](Configure.md). More information about loading additional tracks is available in the [data loading](Data_loading.md) section.

