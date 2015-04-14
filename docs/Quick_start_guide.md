# Quick start guide

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Quick_start_guide.md">On GitHub</a>

## Checklist

This guide will cover the following steps:

 - Downloading Web Apollo
 - Installing system pre-requisites
 - Using `apollo deploy` to setup environment + perl pre-requisites
 - Setting up a database (MySQL/PostgreSQL/H2)
 - Running JBrowse scripts to load data


#### Download webapollo

You can download the latest Web Apollo 2.0 release from [GitHub](https://github.com/gmod/Apollo.git)

Example:

    # clone the latest Web Apollo 2.0 from GitHub and use the latest release tag
    git clone https://github.com/GMOD/Apollo.git
    git checkout grails1


#### Get prerequisites

Then get some system pre-requisites. These commands will try to get everything in one bang for several system types.

    # install system prerequisites (debian/ubuntu)
    sudo apt-get install openjdk-7-jdk curl libexpat1-dev postgresql postgresql-server-dev-all maven tomcat7 git
    # install system prerequisites (centOS/redhat)
    sudo yum install postgresql postgresql-server postgresql-devel maven expat-devel tomcat git curl
    # install system prerequisites (macOSX/homebrew), read the postgresql start guide
    brew install maven postgresql wget tomcat git


See [prerequisites](Prerequisites.md) for more details on the pre-requisites if you think something isn't working with these.
 
#### Download sample data

If you are following our example, you can download the sample data here:

    wget http://icebox.lbl.gov/webapollo/data/pyu_data.tgz
    tar xvzf pyu_data.tgz



#### Setup genome browser data

Setup the JBrowse data directory with some of the sample data for Pythium ultimum. Here, the split_gff.pl script will separate the example GFF based on source types and we will save the data in a global directory. Note: you may have to configure the settings on your chosen directory appropriately (example, chown tomcat:tomcat /opt/apollo/data)

    mkdir /opt/apollo/data
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d pyu_data
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out /opt/apollo/data
    bin/flatfile-to-json.pl --gff  pyu_data/maker.gff --arrowheadClass trellis-arrowhead \
        --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
        --className container-16px --type mRNA --trackLabel maker --out /opt/apollo/data


For more info on adding genome browser tracks, see the [configuration guide](Configure.md) guide.


##### Add webapollo plugin to the genome browser
Once the tracks are initialized, the webapollo plugin needs to be added to the JBrowse configuration using the add-webapollo-plugin.pl script.

    bin/add-webapollo-plugin.pl -i /opt/apollo/data/trackList.json

#### Launch a Web Apollo instance

After this setup, you are ready to deploy a new instance.

    ./apollo run-app

This will launch a temporary tomcat instances that you will be able to access from http://localhost:8080/apollo/


#### Congratulations

If everything works, then you can continue to the login page which will prompt for new login information. Then the organism information can be added via the sidebar.
