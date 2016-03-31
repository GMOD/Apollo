#!/bin/bash

usage() {
    echo ""
    echo "Usage: $0 -u <psql_user> -d <database_name> -h <host> -p <port> -s <chado prebuilt schema> [ -r ]"
    echo ""
    echo "Options:"
    echo "  -u :          PostgreSQL username"
    echo "  -d :          Name of the database to which the chado schema and ontologies are to be loaded"
    echo "  -h :          Database host (default: localhost)"
    echo "  -p :          Port (default: 5432)"
    echo "  -s :          Chado schema to load (*sql.gz)"
    echo "  -r :          Flag that triggers pg_dump if database already exists (optional)"
    echo ""
    exit
}

check_config() {
    PSQL_CREATEDB=$(which createdb)
    PSQL_DROPDB=$(which dropdb)
    PSQL_EXEC=$(which psql)

    if ! [ -x $PSQL_EXEC ] ; then
        echo "You must install PostgreSQL and 'psql' must be accessible in current PATH"
        exit
    fi

    if ! [ -x $PSQL_CREATEDB ] ; then
        echo "'createdb' must be accessible in current PATH"
        exit
    fi

    if ! [ -x $PSQL_DROPDB ] ; then
        echo "'dropdb' must be accessible in current PATH"
        exit
    fi
}

load_chado_schema() {
    file_type=`echo ${CHADO_SCHEMA##*.}`
    echo "Loading chado schema ${CHADO_SCHEMA} to database '$2'."

    if [ $file_type == "gz" ]; then
        gunzip -c $CHADO_SCHEMA | psql -U $1 -d $2 -h $3 -p $4 &> $STDLOG
    else
        psql -U $1 -d $2 -h $3 -p $4 < $CHADO_SCHEMA &> $STDLOG
    fi
}

dump_database() {
    OUTPUT=$2"_database_dump_"$TIMESTAMP".sql"
    echo pg_dump -U $1 -d $2 -h $3 -p $4 -f $OUTPUT -b
    pg_dump -U $1 -d $2 -h $3 -p $4 -f $OUTPUT -b
}

# Default
PORT="5432"
HOST="localhost"
DEFAULTDB="template1"
PGDUMP=0
TIMESTAMP=`date +%F_%T`

STDLOG="load_chado_schema_${TIMESTAMP}.log"

if [ $# -eq 0 ]; then
    usage
    exit
fi

while getopts "h:p:u:d:s:r" opt; do
    case "$opt" in
    h)  HOST=$OPTARG
        ;;
    p)  PORT=$OPTARG
        ;;
    u)  PG_USER=$OPTARG
        ;;
    d)  DB=$OPTARG
        ;;
    s)  CHADO_SCHEMA=$OPTARG
        ;;
    r)  PGDUMP=1
        ;;
    *)  usage
        ;;
    ?)  usage
        ;;
    esac
done

if [ -z "${PG_USER}" ] || [ -z "${DB}" ] || [ -z "${CHADO_SCHEMA}" ] ; then
    usage
fi

if [ ! -f ${CHADO_SCHEMA} ]; then
    echo "File ${CHADO_SCHEMA} not found."
    exit
fi

check_config

psql -U $PG_USER -h $HOST -p $PORT -d $DEFAULTDB -c "CREATE DATABASE \"$DB\""
EXIT_STATUS=$?

if [ $EXIT_STATUS -eq 0 ]; then
    # CREATE DATABASE command was successful (i.e. there was no pre-existing database with the same name)
    load_chado_schema $PG_USER $DB $HOST $PORT

elif [ $EXIT_STATUS -eq 1 ]; then
    # CREATE DATABASE command was unsuccessful because there was a pre-existing database with the same name
    if [ $PGDUMP -eq 1 ]; then
        # -r option was provided at run-time; will attempt to backup the existing database
        echo "Database '$DB' already exists. Backing up data via pg_dump."
        dump_database $PG_USER $DB $HOST $PORT
        if [ $? -eq 0 ]; then
            # PG_DUMP was successful
            echo "pg_dump was successful."
            echo "Dropping and creating database '$DB'."
            # DROP DATABASE after a PG_DUMP
            psql -U $PG_USER -h $HOST -p $PORT -d $DEFAULTDB -c "DROP DATABASE \"$DB\""
            if [ $? -ne 0 ]; then
                echo "Cannot drop database '$DB' due to lack of privileges or existing open connections."
                exit
            fi

            # CREATE DATABASE
            psql -U $PG_USER -h $HOST -p $PORT -d $DEFAULTDB -c "CREATE DATABASE \"$DB\""
            if [ $? -ne 0 ]; then
                echo "Cannot create database '$DB' due to lack of privileges."
                exit
            fi

            # finally, load chado schema
            load_chado_schema $PG_USER $DB $HOST $PORT
        fi
    else
        # -r option was not provided at run-time; will not try to do anything
        echo "Database '$DB' already exists. If you would like to do a pg_dump, to backup its contents, run the script again with '-r' flag."
    fi

else
    # CREATE DATABASE command was unsuccessful for other reasons
    echo "Cannot create database '$DB' due to improper connection parameters, lack of privileges or non-existent user '$PG_USER'."
    exit
fi

echo "Chado schema loaded successfully to ${DB}. Check $STDLOG for more information."