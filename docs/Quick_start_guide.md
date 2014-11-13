# Quick start guide


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
First set some environmental variables. Note that PGUSER is simply your username in this setup, but if you set it to something different, make sure to see [database setup](Database_setup.md) for details.

    export PGUSER=`whoami`
    export PGPASSWORD=password
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"

#### Download webapollo

You can download the latest Web Apollo release from [github](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org](http://genomearchitect.org) (the 1.x release branch is not available from genomearchitect yet).


#### Get prerequisites

Then get some system pre-requisites. These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-devel maven expat-devel tomcat
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat

#### Kickstart postgres (not needed for ubuntu)
One debian/ubuntu, postgres is started automatically so this is unnecessary when it is installed, but on others (centOS/redhat, mac OSX) they need to be kickstarted. You can manually init and start postgres

    # on centOS/redhat, manually kickstart postgres and make it start on OS boot with chkconfig
    sudo su -c "service postgresql initdb && service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres and make it start on OS boot with launchctl
    ln -sfv /usr/local/opt/postgresql/\*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

#### Initialize postgres database
We will setup a new user and database for Web Apollo. Note: see [database setup](Database_setup.md#authentication) for more details about postgres setup details

    # On debian/ubuntu/redhat/centOS,requires postgres user to execute command, hence "sudo su postgres"
    sudo su postgres -c "createuser -RDIElPS $PGUSER"
    sudo su postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"
    # macOSX/homebrew , not necessary to login to postgres user
    createuser -RDIElPS $PGUSER
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

#### Download sample data

If you are following our example, you can download the sample data

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

#### Setup some basic dependencies

We will use Web Apollo's setup.sh script to install perl scripts and dependencies scripts to WEB\_APOLLO\_ROOT/bin/

    ./setup.sh

If there are any errors during this build step, you can check setup.log.

#### Initialize Web Apollo logins and permissions
Now we will initialize the database tables and setup permissions for a new Web Apollo user

    psql -U $PGUSER $WEBAPOLLO_DATABASE < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD

The permissions for the Web Apollo user are configured on a track by track basis by adding the sequences from our FASTA file to the database 

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a


#### Setup genome browser data
Now we will setup our data directory. In this example we will use the Pythium ultimum sample data. For more info on adding genome browser tracks, see the [configuration guide](Configure.md) guide.

Here, the split_gff.pl script will split our example GFF into different types, and we will load the reference sequence FASTA file with prepare-refseqs.pl and the Maker GFF with flatfile-to-json.pl.

    mkdir split_gff
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    bin/flatfile-to-json.pl --gff split_gff/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out data

##### Add webapollo plugin to the genome browser
Once the tracks are initialized, we can add the webapollo plugin to the jbrowse config using the add-webapollo-plugin.pl script.

    client/apollo/bin/add-webapollo-plugin.pl -i data/trackList.json

#### Configure the directories and database info in config.properties

Once we have our data directories and database configuration setup, we can put this information in the config.properties file.

    mkdir annotations
    echo jbrowse.data=`pwd`/data > config.properties
    echo datastore.directory=`pwd`/annotations >> config.properties
    echo database.url=jdbc:postgresql:$WEBAPOLLO_DATABASE >> config.properties
    echo database.username=$PGUSER >> config.properties
    echo database.password=$PGPASSWORD >> config.properties
    echo organism=$ORGANISM >> config.properties

It's best to set this file up before creating the Maven package.

Note: here we used local directory paths for storing the jbrowse data and Web Apollo annotations,
but you may choose to to store these directories elsewhere.

#### (Optional) Setup other webapollo configurations in config.xml

The config.xml offers many options for customizing Web Apollo, but you can use most of the defaults from sample_config.xml for our purposes.

    mv sample_config.xml config.xml

For more details on the config.xml options, see the [configuration guide](Configure.md).

#### Run a test server

You can use run.sh to launch a temporary Tomcat server for testing.

    ./run.sh

Then you will be able to access Web Apollo from  http://localhost:8080/apollo/ and login with your web_apollo_user information. 

#### Congratulations

If everything works, then you can continue to the [build guide](Build.md) for more instructions on packaging the build, and to the [deployment guide](Deploy.md) for information about deploying to a production server.

