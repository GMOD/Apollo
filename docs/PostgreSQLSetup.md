# PostgreSQL Setup guide

There are a couple ways to setup a PostgreSQL.  One would be as a trusted user (e.g. postgres):

## Setup as a non-trusted user "database_user" with a secure password for a production database named "apollo-production"

- On debian/ubuntu/redhat/centOS,requires postgres user to execute command, hence "sudo su postgres"
```
sudo su postgres -c "createuser -RDIElPS database_user"
sudo su postgres -c "createdb -E UTF-8 -O database_user apollo-production"
```
- On macOSX/homebrew, not necessary to login to postgres user

```
createuser -RDIElPS database_user 
createdb -O database_user apollo-production
```


- In ```apollo-config.groovy``` your username will be the name of the user and you should provide the password.

## Setup as a trusted postgres user with a database named "apollo-production"

- On debian/ubuntu/redhat/centOS,requires postgres user to execute command, hence "sudo su postgres"
```
sudo su postgres -c "createuser -RDIElPS $PGUSER"
sudo su postgres -c "createdb -E UTF-8 -O $PGUSER apollo-production"
```
- On macOSX/homebrew, not necessary to login to postgres user
```
createuser -RDIElPS $PGUSER
createdb -O $PGUSER apollo-production
```

- In ```apollo-config.groovy``` your username will be postgres and you should comment out the password line.

Note: Using a tool like [pgtune](http://pgtune.leopard.in.ua/) might help to tune PostgreSQL settings.  
