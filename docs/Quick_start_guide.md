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
First set some environmental variables

    export PGUSER=web_apollo_users_admin
    export PGPASSWORD=web_apollo_users_admin
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"

#### Get prerequisites

Then get some system pre-requisites. These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev cpanminus postgresql postgresql-server-dev-all postgresql-server maven tomcat
    # install system prerequisites (centOS/redhat)
    sudo yum install cpanminus postgresql postgresql-devel maven expat-devel tomcat
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven cpanminus postgresql wget tomcat

#### Kickstart postgres (not needed for ubuntu)
One debian/ubuntu, postgres is started automatically so this is unnecessary when it is installed, but on others (centOS/redhat, mac OSX) they need to be kickstarted. You can manually init and start postgres

    # on centOS/redhat, manually kickstart postgres and make it start on OS boot with chkconfig
    sudo su -c "service postgresql initdb && service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres and make it start on OS boot with launchctl
    ln -sfv /usr/local/opt/postgresql/\*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

#### Configure cpanminus
Next, we will configure cpanminus and install some webapollo-specific perl prerequisites

    cpanm --local-lib=~/perl5 local::lib && eval $(perl -I ~/perl5/lib/perl5/ -Mlocal::lib)
    cpanm Crypt::PBKDF2 DBI DBD::Pg

#### Initialize postgres database
We will setup a new user and database for Web Apollo. Note: see [database setup](Database_setup.md#authentication) for more details about postgres setup details

    sudo su postgres -c "createuser -RDIElPS $PGUSER"
    sudo su postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"
    # macOSX/homebrew, not necessary to use postgres user to login
    createuser -RDIElPS $PGUSER
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

#### Download Web Apollo and sample data
Now we will get the latest WebApollo release distribution

    wget https://github.com/GMOD/Apollo/releases/download/1.0.0-RC2/Apollo-1.0.0-RC2-release.tar.gz
    tar xvzf Apollo-1.0.0-RC2-release.tar.gz

If you are following our example, you can also download the sample data

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

#### Initialize Web Apollo logins and permissions
Now we will initialize the database tables and setup permissions for a new Web Apollo user

    psql -U $PGUSER $WEBAPOLLO_DATABASE < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD

The permissions for the Web Apollo user are configured on a track by track basis by adding the sequences from our FASTA file to the database 

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a

We will install the jbrowse perl scripts using cpanm. This will allow you to run the data processing pipeline scripts from anywhere.

    ./install_jbrowse_bin.sh cpanm

#### Setup genome browser data
Now we will setup our data directory. In this example we will use the sample data. For other purposes, refer to the [configuration guide](Configure.md) guide for more details.

Here, the split_gff.pl script will split our example GFF into different types, and we will use flatfile-to-json.pl to load the maker track.

    mkdir split_gff
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff
    prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out /apollo/data
    flatfile-to-json.pl --gff split_gff/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out /apollo/data

##### Add webapollo plugin to the genome browser
Once the tracks are initialized, we can add the webapollo plugin to the jbrowse config using the add-webapollo-plugin.pl script.

    client/apollo/bin/add-webapollo-plugin.pl -i data/trackList.json

#### Configure the locations of the data directories and database login
Configure data directories using config.properties (note: here we use /apollo/data and /apollo/annotations)

    mkdir annotations
    echo jbrowse.data=/apollo/data > config.properties
    echo datastore.directory=/apollo/annotations >> config.properties
    echo database.url=jdbc:postgresql:$WEBAPOLLO_DATABASE >> config.properties
    echo database.username=$PGUSER >> config.properties
    echo database.password=$PGPASSWORD >> config.properties
    echo organism=$ORGANISM >> config.properties

The sample_config.xml can be used largely unmodified, so we will just copy it for now. For more details on the configuration, see the [configuration guide](Configure.md)

    mv sample_config.xml config.xml


#### Build the release package

Build a release package

    mvn package

Your Web Apollo instance is now ready to deploy! For deployment instructions, see the [deployment guide](Deploy.md).

