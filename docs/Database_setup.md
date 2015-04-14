# Database setup

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Database_setup.md">On GitHub</a>

Web Apollo 2.0 simplifies and expands options for the database setup. This section will cover the basic options for this.

### Options


- H2 database - an embedded in-memory zero-configuration database
- PostgreSQL - an established database system that is popular for Chado compatibility
- MySQL - an established database system that has familiarity in the PHP community

### Create new postgres user (optional)

    $ sudo su postgres # unneeded for some systems (e.g. homebrew)
    $ createuser -P web_apollo_users_admin
    Enter password for new role: 
    Enter it again: 
    Shall the new role be a superuser? (y/n) n
    Shall the new role be allowed to create databases? (y/n) y
    Shall the new role be allowed to create more new roles? (y/n) n

### Create new authentication database

Now we can create a new database that is owned by the web_apollo_users_admin user. We will use the "-h localhost" flag to force host based authentication instead of peer authentication.

    $ createdb -U web_apollo_users_admin web_apollo_users -h localhost

Now that the database is created, we need to load the schema to it.

    psql -U web_apollo_users_admin web_apollo_users -h localhost < tools/user/user_database_postgresql.sql

Now the user database has been setup, and you can run accessory scripts to add users and tracks.

### Create a Web Apollo login

Next we'll use the `add_user.pl` script in `WEB_APOLLO_DIR/tools/user` to create a user with access to Web Apollo. Let's create a user in the Web Apollo database named `web_apollo_admin` with the password `web_apollo_admin`.

    tools/user/add_user.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -u web_apollo_admin -p web_apollo_admin

Note that the default mode add_user is to produce encrypted passwords using the EncryptedLocalDbUserAuthentication with the PBKDF2 algorithm. Other options for authentication include:

- EncryptedLocalDbUserAuthentication (Default as of 1.0.4)
- LocalDbUserAuthentication
- DrupalDbUserAuthentication (For Mozilla Persona)
- RemoteUserAuthentication (For LDAP)
- OauthUserAuthentication (For Oauth providers)

Use the authentication_class tag in config.xml to configure this option.


### Initialize permissions for Web Apollo login

Next we'll add the annotation tracks ids for the genomic sequences for
our organism. We'll use the `add_tracks.pl` script to add the chromosome
or sequence IDs to our database. But first we need to generate a file of the
genomic sequence ids for the script. For convenience, there's a script called
`extract_seqids_from_fasta.pl` in the same directory which will go
through a FASTA file and extract all the ids from the deflines. Let's
first create the list of genomic sequence ids. We'll store it in
`~/scratch/seqids.txt`. We'll want to add the prefix "Annotations-" to
each identifier.



    mkdir ~/scratch
    tools/user/extract_seqids_from_fasta.pl -p Annotations- -i WEB_APOLLO_SAMPLE_DIR/scf1117875582023.fa -o ~/scratch/seqids.txt

Now we'll add those ids to the user database.

    tools/user/add_tracks.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -t ~/scratch/seqids.txt

Now that we have an user created and the annotation track ids loaded,
we'll need to give the user permissions to access the sequence. We'll
have the all permissions (read, write, publish, user manager). We'll use
the `set_track_permissions.pl` script in the same directory. We'll need
to provide the script a list of genomic sequence ids, like in the
previous step.

    tools/user/set_track_permissions.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -u web_apollo_admin -t ~/scratch/seqids.txt -a


### Finished
We're all done setting up the user database. You can now add your postgres user and password login to config.properties. Follow the [build guide](Build.md) for more details

Note that we're only using a subset of the options for all the scripts
mentioned above. You can get more detailed information on any given
script (and other available options) using the "-h" or "--help" flag
when running the script.

