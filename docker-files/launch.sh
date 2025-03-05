#!/usr/bin/env bash

WEBAPOLLO_COMMON_DATA="${WEBAPOLLO_COMMON_DATA:-/data/apollo_data}"

if [ ! -e "${WEBAPOLLO_COMMON_DATA}" ]; then
	mkdir -p "${WEBAPOLLO_COMMON_DATA}"
	chmod -R 0755 "${WEBAPOLLO_COMMON_DATA}"
fi

if [ ! -e "${WEBAPOLLO_COMMON_DATA}/test_file" ]; then
	touch "${WEBAPOLLO_COMMON_DATA}"/test_file
fi

WEBAPOLLO_DB_DATA="/var/lib/postgresql/9.6/main"

if [ ! -e ${WEBAPOLLO_DB_DATA} ]; then
	mkdir -p ${WEBAPOLLO_DB_DATA}
fi

if [ -e "${WEBAPOLLO_DB_DATA}/PG_VERSION" ] && [ ! -e "${WEBAPOLLO_DB_DATA}/postgresql.conf" ]; then
	rm -r ${WEBAPOLLO_DB_DATA:?}/*
fi

if [ ! -e "${WEBAPOLLO_DB_DATA}/PG_VERSION" ]; then
	/usr/lib/postgresql/9.6/bin/initdb -D ${WEBAPOLLO_DB_DATA}
fi

export WEBAPOLLO_START_POSTGRES="${WEBAPOLLO_START_POSTGRES:-true}"

if [[ ${WEBAPOLLO_START_POSTGRES} == "true" ]]; then
	/usr/lib/postgresql/9.6/bin/pg_ctl -D ${WEBAPOLLO_DB_DATA} -w start
fi

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

if [[ ${WEBAPOLLO_DB_HOST} != "127.0.0.1" ]]; then
	export WEBAPOLLO_HOST_FLAG="-h ${WEBAPOLLO_DB_HOST}"
	echo "WEBAPOLLO_HOST_FLAG: $WEBAPOLLO_HOST_FLAG"
fi
if [[ ${CHADO_DB_HOST} != "127.0.0.1" ]]; then
	export CHADO_HOST_FLAG="-h ${CHADO_DB_HOST}"
	echo "CHADO_HOST_FLAG: $CHADO_HOST_FLAG"
fi

echo "Waiting for DB"
until /usr/lib/postgresql/9.6/bin/pg_isready $WEBAPOLLO_HOST_FLAG; do
	echo -n "."
	sleep 1
done

echo "Postgres is up, configuring database"

USER=$(whoami)
/usr/lib/postgresql/9.6/bin/psql $WEBAPOLLO_HOST_FLAG -lqt | cut -d \| -f 1 | grep -qw "$USER"
if [[ $? == "1" ]]; then
	echo "User database not found, creating..."
	/usr/lib/postgresql/9.6/bin/createdb $WEBAPOLLO_HOST_FLAG
fi

PGPASSWORD=$WEBAPOLLO_DB_PASSWORD /usr/lib/postgresql/9.6/bin/psql $WEBAPOLLO_HOST_FLAG -U "$WEBAPOLLO_DB_USERNAME" -lqt | cut -d \| -f 1 | grep -qw "$WEBAPOLLO_DB_NAME"
if [[ $? == "1" ]]; then
	echo "Apollo database not found, creating..."
	/usr/lib/postgresql/9.6/bin/createdb $WEBAPOLLO_HOST_FLAG "$WEBAPOLLO_DB_NAME"
	/usr/lib/postgresql/9.6/bin/psql $WEBAPOLLO_HOST_FLAG -c "CREATE USER $WEBAPOLLO_DB_USERNAME WITH PASSWORD '$WEBAPOLLO_DB_PASSWORD';"
	/usr/lib/postgresql/9.6/bin/psql $WEBAPOLLO_HOST_FLAG -c "GRANT ALL PRIVILEGES ON DATABASE $WEBAPOLLO_DB_NAME to $WEBAPOLLO_DB_USERNAME;"
fi

if [[ ${WEBAPOLLO_USE_CHADO} == "true" ]]; then
	echo "Configuring Chado"
	PGPASSWORD=$CHADO_DB_PASSWORD /usr/lib/postgresql/9.6/bin/psql $CHADO_HOST_FLAG -U "$CHADO_DB_USERNAME" -lqt | cut -d \| -f 1 | grep -qw "$CHADO_DB_NAME"
	if [[ $? == "1" ]]; then
		echo "Chado database not found, creating..."
		/usr/lib/postgresql/9.6/bin/createdb $CHADO_HOST_FLAG "$CHADO_DB_NAME"
		/usr/lib/postgresql/9.6/bin/psql $CHADO_HOST_FLAG -c "CREATE USER $CHADO_DB_USERNAME WITH PASSWORD '$CHADO_DB_PASSWORD';"
		/usr/lib/postgresql/9.6/bin/psql $CHADO_HOST_FLAG -c "GRANT ALL PRIVILEGES ON DATABASE $CHADO_DB_NAME to $CHADO_DB_USERNAME;"
		echo "Loading Chado"
		PGPASSWORD=$CHADO_DB_PASSWORD /usr/lib/postgresql/9.6/bin/psql -U "$CHADO_DB_USERNAME" -h "$CHADO_DB_HOST" "$CHADO_DB_NAME" -f /chado.sql
		echo "Loaded Chado"
	fi
else
	echo "Not using chado!"
fi

export CATALINA_HOME="${CATALINA_HOME}"
export CATALINA_BASE="${CATALINA_BASE}"

echo "CATALINA_HOME '${CATALINA_HOME}'"
echo "CATALINA_BASE '${CATALINA_BASE}'"

APOLLO_PATH="${APOLLO_PATH:CONTEXT_PATH}"
FIXED_CTX="${APOLLO_PATH////#}"
WAR_FILE=${CATALINA_BASE}/webapps/${FIXED_CTX}.war

echo "APOLLO PATH '${APOLLO_PATH}'"
echo "FIXED_CTX PATH '${FIXED_CTX}'"
echo "WAR FILE '${WAR_FILE}'"

cp "${CATALINA_BASE}"/apollo.war "${WAR_FILE}"

# Set environment variables for tomcat
bash /createenv.sh

# Launch tomcat, stopping of already running.
"${CATALINA_HOME}"/bin/catalina.sh stop 5 -force
"${CATALINA_HOME}"/bin/catalina.sh run
