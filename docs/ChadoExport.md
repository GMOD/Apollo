# Chado Export Configuration

Following are the steps for setting up a Chado data source that is compatible with Apollo Chado Export.

### Create a Chado database

First create a database in PostgreSQL for Chado.

Note: Initial testing has only been done on PostgreSQL.

Default name is `apollo-chado` and `apollo-production-chado` for development and production environment, respectively.

### Create a Chado user

Now, create a database user that has all access privileges to the newly created Chado database.

### Load Chado schema and ontologies

Apollo assumes that the Chado database has Chado schema v1.2 or greater and has the following ontologies loaded:

1. Relations Ontology
2. Sequence Ontology
3. Gene Ontology


The quickest and easiest way to do this is to use prebuilt Chado schemas.
Apollo provides a prebuilt Chado schema with the necessary ontologies. (thanks to Eric Rasche at [Center for Phage Technology, TAMU](https://cpt.tamu.edu/computer-resources/chado-prebuilt-schema/))


Users can load this prebuilt Chado schema as follows:
```
scripts/load_chado_schema.sh -u <USER> -d <CHADO_DATABASE> -h <HOST> -p <PORT> -s <CHADO_SCHEMA_SQL>
```

If there is already an existing database with the same name and if you would like to dump and create a clean database:
```
scripts/load_chado_schema.sh -u <USER> -d <CHADO_DATABASE> -h <HOST> -p <PORT> -s <CHADO_SCHEMA_SQL> -r
```

The '-r' flag tells the script to perform a pg_dump if `<CHADO_DATABASE>` exists.


e.g., 

```
scripts/load_chado_schema.sh -u postgres -d apollo-chado -h localhost -p 5432 -r -s chado-schema-with-ontologies.sql.gz

```

The file `chado-schema-with-ontologies.sql.gz` can be found in `Apollo/scripts/` directory.

The `load_chado_schema.sh` script creates log files which can be inspected to see if loading the schema was successful.

Note that you will also need to do this for your testing and production instances, as well.

### Configure data sources

In `apollo-config.groovy`, uncomment the configuration for `datasource_chado` and specify the proper database name, database user name and database user password.

### Export via UI

Users can export existing annotations to the Chado database via the Annotator Panel -> Ref Sequence -> Export.

### Export via web services

Users can also leverage the Apollo web services API to export annotations to Chado.
As a demonstration, a sample script, `export_annotations_to_chado.groovy` is provided.

Usage for the script:

```
export_annotations_to_chado.groovy -organism ORGANISM_COMMON_NAME -username APOLLO_USERNAME -password APOLLO_PASSWORD -url http://localhost:8080/apollo
```
