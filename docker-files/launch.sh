#!/usr/bin/env bash


WEBAPOLLO_COMMON_DATA="${WEBAPOLLO_COMMON_DATA:-/data/apollo_data}"

if [ ! -e ${WEBAPOLLO_COMMON_DATA} ]; then
	mkdir -p ${WEBAPOLLO_COMMON_DATA}
	chmod -R 0755 ${WEBAPOLLO_COMMON_DATA}
fi

if [ ! -e "${WEBAPOLLO_COMMON_DATA}/test_file" ];then
	su -c "touch ${WEBAPOLLO_COMMON_DATA}/test_file"
fi

WEBAPOLLO_DB_DATA="/var/lib/postgresql/9.6/main"

if [ ! -e ${WEBAPOLLO_DB_DATA} ]; then
	mkdir -p ${WEBAPOLLO_DB_DATA}
	chown -R postgres:postgres ${WEBAPOLLO_DB_DATA}
fi

if [ ! -e "${WEBAPOLLO_DB_DATA}/PG_VERSION" ];then
	su -c "/usr/lib/postgresql/9.6/bin/initdb -D ${WEBAPOLLO_DB_DATA}" postgres
fi

service postgresql start

export WEBAPOLLO_DB_HOST="${WEBAPOLLO_DB_HOST:-127.0.0.1}"
export WEBAPOLLO_DB_NAME="${WEBAPOLLO_DB_NAME:-apollo}"
export WEBAPOLLO_DB_USERNAME="${WEBAPOLLO_DB_USERNAME:-apollo}"
export WEBAPOLLO_DB_PASSWORD="${WEBAPOLLO_DB_PASSWORD:-apollo}"


# TODO: use variable throughout
export WEBAPOLLO_USE_CHADO="${WEBAPOLLO_USE_CHADO:true}"

export CHADO_DB_HOST="${CHADO_DB_HOST:-127.0.0.1}"
export CHADO_DB_NAME="${CHADO_DB_NAME:-chado}"
export CHADO_DB_USERNAME="${CHADO_DB_USERNAME:-apollo}"
export CHADO_DB_PASSWORD="${CHADO_DB_PASSWORD:-apollo}"

if [[ "${WEBAPOLLO_DB_HOST}" != "127.0.0.1" ]]; then
    export WEBAPOLLO_HOST_FLAG="-h ${WEBAPOLLO_DB_HOST}"
fi
if [[ "${CHADO_DB_HOST}" != "127.0.0.1" ]]; then
    export CHADO_HOST_FLAG="-h ${CHADO_DB_HOST}"
fi

echo "WEBAPOLLO_HOST_FLAG: $WEBAPOLLO_HOST_FLAG"
echo "CHADO_HOST_FLAG: $CHADO_HOST_FLAG"

echo "Waiting for DB"
until pg_isready $WEBAPOLLO_HOST_FLAG; do
	echo -n "."
	sleep 1;
done

echo "Postgres is up, configuring database"

su postgres -c "PGPASSWORD=$WEBAPOLLO_DB_PASSWORD psql $WEBAPOLLO_HOST_FLAG -U $WEBAPOLLO_DB_USERNAME -lqt | cut -d \| -f 1 | grep -qw $WEBAPOLLO_DB_NAME"
if [[ "$?" == "1" ]]; then
	echo "Apollo database not found, creating..."
	su postgres -c "createdb $WEBAPOLLO_HOST_FLAG $WEBAPOLLO_DB_NAME"
	su postgres -c "psql $WEBAPOLLO_HOST_FLAG -c \"CREATE USER $WEBAPOLLO_DB_USERNAME WITH PASSWORD '$WEBAPOLLO_DB_PASSWORD';\""
	su postgres -c "psql $WEBAPOLLO_HOST_FLAG -c \"GRANT ALL PRIVILEGES ON DATABASE $WEBAPOLLO_DB_NAME to $WEBAPOLLO_DB_USERNAME;\""
fi

if [[ "${WEBAPOLLO_USE_CHADO}" == "true" ]]; then
    echo "Configuring Chado"
    su postgres -c "PGPASSWORD=$CHADO_DB_PASSWORD psql $CHADO_HOST_FLAG -U $CHADO_DB_USERNAME -lqt | cut -d \| -f 1 | grep -qw $CHADO_DB_NAME"
    if [[ "$?" == "1" ]]; then
        echo "Chado database not found, creating..."
        su postgres -c "createdb $CHADO_HOST_FLAG $CHADO_DB_NAME"
        su postgres -c "psql $CHADO_HOST_FLAG -c \"CREATE USER $CHADO_DB_USERNAME WITH PASSWORD '$CHADO_DB_PASSWORD';\""
        su postgres -c "psql $CHADO_HOST_FLAG -c 'GRANT ALL PRIVILEGES ON DATABASE \"$CHADO_DB_NAME\" to $CHADO_DB_USERNAME;'"
        echo "Loading Chado"
        su postgres -c "PGPASSWORD=$CHADO_DB_PASSWORD psql -U $CHADO_DB_USERNAME -h $CHADO_DB_HOST $CHADO_DB_NAME -f /chado.sql"
        echo "Loaded Chado"
    fi
else
    echo "Not using chado!"
fi

export CATALINA_HOME="${CATALINA_HOME:-/usr/local/tomcat/}"

APOLLO_PATH="${APOLLO_PATH:${CONTEXT_PATH}}"
FIXED_CTX=$(echo "${APOLLO_PATH}" | sed 's|/|#|g')
WAR_FILE=${CATALINA_HOME}/webapps/${FIXED_CTX}.war

echo "APOLLO PATH ${APOLLO_PATH}"
echo "FIXED_CTX PATH ${FIXED_CTX}"
echo "WAR FILE ${WAR_FILE}"

cp ${CATALINA_HOME}/apollo.war ${WAR_FILE}

catalina.sh run
