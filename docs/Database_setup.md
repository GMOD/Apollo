## Database setup

WebApollo uses a database backed authentication by default that uses postgres.
This is the so called LocalDbUserAuthentication class. To configure it, you must
setup a postgres database.

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

