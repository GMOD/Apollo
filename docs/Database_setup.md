# Database setup

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Database_setup.md">On GitHub</a>

WebApollo uses a database backed authentication by default that uses postgres.
This is the so called LocalDbUserAuthentication class. To configure it, you must
setup a postgres database.

## Background: postgres authentication methods

If you are a postgres pro, you can skip this section, but for new users, understanding postgres
authentication methods is important. There are several different types of authentication methods
that can be configured for a postgres database, the most common ones being:

-   peer - allows shell based logins without a password
-   ident - based off of operating system logins
-   md5 - basic password based-logins

For webapollo, "peer" is recommended for "local" logins, an "md5" is recommended for "host" logins.
The login methods are configured through the pg\_hba.conf file, which can be different depending 
on the system you are on.

### Debian/Ubuntu default pg_hba.conf (not essential to change)

On ubuntu/debian, the pg\_hba.conf shows the following:

    local   all             postgres                                peer

    # TYPE  DATABASE        USER            ADDRESS                 METHOD

    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            md5
    # IPv6 local connections:
    host    all             all             ::1/128                 md5

This means on ubuntu, command line logins to psql are allowed for the postgres user without any
password. Logins over TCP/IP will use password based authentication however, which is what we
want for WebApollo.

### CentOS/RedHat default pg_hba.conf (should be changed)
On redhat/centOS however, the default pg\_hba.conf file is not ideal for WebApollo, and uses ident by default:

    # TYPE  DATABASE        USER            ADDRESS                 METHOD

    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            ident
    # IPv6 local connections:
    host    all             all             ::1/128                 ident

It is important that on redhat, the IPv4 and IPv6 are changed from ident
to md5 so that perl scripts like add\_user.pl can work.

### An ideal pg_hba.conf for WebApollo
An ideal pg_hba.conf file would allow password based authentication for TCP/IP connections to the database. Ideally, we might also add authentication for a non-operating system user, `web_apollo_users_admin` to manage the Web Apollo database.

    local   all             postgres                                peer
    local   all             web_apollo_users_admin                  md5

    # TYPE  DATABASE        USER            ADDRESS                 METHOD

    # "local" is for Unix domain socket connections only
    local   all             all                                     peer
    # IPv4 local connections:
    host    all             all             127.0.0.1/32            md5
    # IPv6 local connections:
    host    all             all             ::1/128                 md5



## User database

Web Apollo uses a database to determine who can access and edit
annotations for a given sequence.

First we'll need to create a database to store our authentication data.
You can call this database whatever you want, but remember the name of it as you'll need to point the configuration to it. For the purposes of this guide, we'll call it `web_apollo_users`. 

Also, you might want to create a new postgres user account to manage the database. We'll have the user `web_apollo_users_admin` with password `web_apollo_users_admin` who has database creation privilege. This is not essential, but it makes it easier to administer because it doesn't tie the Web Apollo database to a specific operating system user.

Depending on how your database server
is setup, you might not need to set a password for the user. See the
[PostgreSQL documentation](http://www.postgresql.org/docs) for more
information. We'll assume that the database is in the same server where
Web Apollo is being installed ("localhost"). These commands should be run
as the *postgres* user.

### Create new postgres user (optional)

    $ sudo su postgres
    $ createuser -P web_apollo_users_admin
    Enter password for new role: 
    Enter it again: 
    Shall the new role be a superuser? (y/n) n
    Shall the new role be allowed to create databases? (y/n) y
    Shall the new role be allowed to create more new roles? (y/n) n

### Create new authentication database

    $ createdb -U web_apollo_users_admin web_apollo_users

If you get an authentication error, use the -W flag to get a password
prompt.

    $ createdb -U web_apollo_users_admin -W web_apollo_users

If you are not using the web_apollo_users_admin login, you can also simply use

    $ createdb web_apollo_users

Now that the database is created, we need to load the schema to it.

    psql -U web_apollo_users_admin web_apollo_users < tools/user/user_database_postgresql.sql

Now the user database has been setup.

### Create a Web Apollo login

Next we'll use the `add_user.pl` script in `WEB_APOLLO_DIR/tools/user` to create a user with access to Web Apollo. Let's create a user in the Web Apollo database named `web_apollo_admin` with the password `web_apollo_admin`.

    tools/user/add_user.pl -D web_apollo_users -U web_apollo_users_admin -P web_apollo_users_admin -u web_apollo_admin -p web_apollo_admin


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

