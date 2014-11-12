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

First set some environmental variables

    export PGUSER=web_apollo_users_admin
    export PGPASSWORD=web_apollo_users_admin
    export WEBAPOLLO_USER=web_apollo_admin
    export WEBAPOLLO_PASSWORD=web_apollo_admin
    export WEBAPOLLO_DATABASE=web_apollo_users
    export ORGANISM="Pythium ultimum"

Then get some system pre-requisites. This tries to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk libexpat1-dev cpanminus postgresql postgresql-server-dev-all postgresql-server maven tomcat
    # install system prerequisites (centOS/redhat)
    sudo yum install cpanminus postgresql postgresql-devel maven expat-devel tomcat
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven cpanminus postgresql wget tomcat

    # on centOS/redhat, manually init and start postgres (and make it start on OS boot using chkconfig)
    sudo su -c "service postgresql initdb && service postgresql start"
    sudo su -c "chkconfig postgresql on"

    # on macOSX/homebrew, manually kickstart postgres (and make it start on OS boot with launchctl)
    ln -sfv /usr/local/opt/postgresql/\*.plist ~/Library/LaunchAgents
    launchctl load ~/Library/LaunchAgents/homebrew.mxcl.postgresql.plist

On setup cpanm and install jbrowse and webapollo perl prerequisites

    cpanm --local-lib=~/perl5 local::lib && eval $(perl -I ~/perl5/lib/perl5/ -Mlocal::lib)
    cpanm DateTime Text::Markdown Crypt::PBKDF2 DBI DBD::Pg

 We will need some additional steps to setup postgres. On ubuntu/redhat/centOS we can create a new postgres user and database for the webapollo instance.
 Note: see [database setup](Database_setup.md#authentication) section for more details about postgres.

    sudo su postgres -c "createuser -RDIElPS $PGUSER"
    sudo su postgres -c "createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE"
    # macOSX/homebrew, no need to createuser
    createdb -E UTF-8 -O $PGUSER $WEBAPOLLO_DATABASE

Download a webapollo release from the releases page

    git https://github.com/GMOD/Apollo/archive/1.0.0-RC2.tar.gz 
    tar xvzf 1.0.0-RC2.tar.gz
    cd Apollo-1.0.0-RC2

Get the sample data

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz

Initialize PostgreSQL data base for sample data. Enter the password web_apollo_users_admin for first step

    psql -U $PGUSER $WEBAPOLLO_DATABASE < tools/user/user_database_postgresql.sql
    tools/user/add_user.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -p $WEBAPOLLO_PASSWORD

Add the chromosome names to the webapollo database

    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i pyu_data/scf1117875582023.fa -o seqids.txt
    tools/user/add_tracks.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -t seqids.txt
    tools/user/set_track_permissions.pl -D $WEBAPOLLO_DATABASE -U $PGUSER -P $PGPASSWORD -u $WEBAPOLLO_USER -t seqids.txt -a

Build a release package by downloading the pre-compiled jbrowse

    make clean download-release package

Install jbrowse perl scripts using cpanm. This will allow you to run the command line scripts from anywhere.

    ./install_jbrowse_bin.sh cpanm


Setup jbrowse data directory in WEB_APOLLO_ROOT/data using the sample data. split_gff.pl will split the Maker GFF into different types, and flatfile-to-json.pl will load the main track.

    mkdir split_gff
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff
    prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out data
    flatfile-to-json.pl --gff split_gff/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out data

Once the tracks are initialized, we can add the webapollo plugin to the jbrowse config using the add-webapollo-plugin.pl script.

    client/apollo/bin/add-webapollo-plugin.pl -i data/trackList.json

Configure data directories using config.properties

    mkdir annotations
    echo jbrowse.data=`pwd`/data > config.properties
    echo datastore.directory=`pwd`/annotations >> config.properties
    echo database.url=jdbc:postgresql:$WEBAPOLLO_DATABASE >> config.properties
    echo database.username=$PGUSER >> config.properties
    echo database.password=$PGPASSWORD >> config.properties
    echo organism=$ORGANISM >> config.properties

We will use the sample_config.xml unmodified in our own config, though some things may want to be customized later

    mv sample_config.xml config.xml

Build a release packagelaunch instance for testing and launch a temporary server using run.sh

    make clean release package

Now your release package should be ready in the target/ directory. For deployment, see the [deployment guide](Deploy.md).

