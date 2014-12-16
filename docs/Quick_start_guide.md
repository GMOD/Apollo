# Quick start guide

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Quick_start_guide.md">On GitHub</a>

While there are a number of prerequisites to WebApollo, we hope that
this quick-start guide can help by automating some setup steps. This
"Quick start guide" can be used to initialize a "blank" machine with a
WebApollo instance from scratch.

## Checklist
This guide will be doing the following steps

 - Installing system and perl prerequisites
 - Setting up a postgres user and database
 - Downloading WebApollo from github
 - Running the WebApollo build script
 - Creating a new data directory for the JBrowse data
 - Creating a new data directory for the WebApollo annotations
 - Configuring WebApollo using the config.properties file

#### Setup environment
First set some environmental variables. These can be customized appropriate to your setup. Note that PGUSER is simply your username in this setup, but if you set it to something different, make sure to see [database setup](Database_setup.md) for details.

    export PGUSER=`whoami`
    export PGPASSWORD=password
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"
    export JBROWSE_DATA_DIR=`pwd`/data
    export WEBAPOLLO_DATA_DIR=`pwd`/annotations


#### Download webapollo

You can download the latest Web Apollo release from [github](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org](http://genomearchitect.org) (the 1.x release branch is not available from genomearchitect yet).


#### Get prerequisites

Then get some system pre-requisites. These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7 git
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-devel maven expat-devel tomcat git
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat git

#### Kickstart postgres (not needed for ubuntu)

On debian/ubuntu, postgres is started and added to OS boot automatically, but on other systems (centOS/redhat, mac OSX) they need to be kickstarted. 

    # on centOS/redhat, manually kickstart postgres and make it start on OS boot with chkconfig
    sudo su -c "service postgresql initdb && service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres and make it start on OS boot with launchctl
    ln -sfv /usr/local/opt/postgresql/*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

#### Initialize postgres database

After starting postgres, you can create a new user and database for Web Apollo authentication.

    # On debian/ubuntu/redhat/centOS,requires postgres user to execute command, hence "sudo su postgres"
    sudo su postgres -c "createuser -RDIElPS $PGUSER"
    sudo su postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"
    # macOSX/homebrew, may not necessary to createuser if using `whoami` for PGUSER
    createuser -RDIElPS $PGUSER
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

Note: see [database setup](Database_setup.md#authentication) for more details about postgres setup, especially if using a non-operating system user for PGUSER.
 
#### Download sample data

If you are following our example, you can download the sample data here:

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

#### Setup some basic dependencies

We will use the `apollo deploy` script to initialize jbrowse and install some basic perl pre-requisites. We can use simply copy the default configs when initializing setups.

    cp sample_canned_comments.xml canned_comments.xml
    cp sample_config.properties config.properties
    cp sample_config.xml config.xml
    ./apollo deploy

If there are any errors during this build step, you can check setup.log. See the [troubleshooting guide](Troubleshooting.md) for common issues.

#### Initialize Web Apollo logins and permissions
Now you may initialize the database tables add a new Web Apollo user as follows.

    psql -U $PGUSER $WEBAPOLLO_DATABASE < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD

The permissions for the Web Apollo user are configured for each track/refseq, so the following scripts are used to (1) extract refseqs from a fasta file (2) add the refseqs to the database and (3) initialize the permissions database.

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a



#### Setup genome browser data
Now we will setup our data directory. In this example we will use the Pythium ultimum sample data. For more info on adding genome browser tracks, see the [configuration guide](Configure.md) guide.

Here, the split_gff.pl script will separate the example GFF based on source types, and then the JBROWSE_DATA_DIR will be initialized with prepare-refseqs.pl and flatfile-to-json.pl.

    mkdir $WEBAPOLLO_DATA_DIR
    mkdir $JBROWSE_DATA_DIR
    mkdir temp
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d temp
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    bin/flatfile-to-json.pl --gff  temp/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out $JBROWSE_DATA_DIR

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

#### Setup additional Web Apollo configuration

In this guide, we have simply added basic properties to config.properties, but more extensive configuration options are available to both JBrowse and Web Apollo. See the [configuration guide](Configure.md) for details.

#### Run a test server

After this setup, you are ready to deploy a new instance. You can use 

    ./apollo run

This will launch a temporary tomcat instances that you will be able to access from http://localhost:8080/apollo/ and login with your $WEBAPOLLO_USER information.

Note: if you already have a tomcat instance running, you may have to shut it down to launch the test server.

#### Congratulations

If everything works, then you can continue to the [build guide](Build.md) for more instructions on packaging the build, and to the [deployment guide](Deploy.md) for information about deploying to a production server. Additionally, information about configuration and adding Chado export can be found in the [configuration guide](Configure.md). More information about loading additional tracks is available in the [data loading](Data_loading.md) section.

