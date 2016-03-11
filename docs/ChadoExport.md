# Chado Export

Following are the steps for setting up a Chado data source that is compatible with Apollo Chado Export.

### Create a Chado database

First create a database in PostgreSQL for Chado.

Note: Initial testing has only been done on PostgreSQL.

Default name is `apollo_chado` and `apollo_chado_production` for development and production environment, respectively.

### Create a Chado user

Now, create a database user that has all access privileges to the newly created Chado database.

### Load Chado schema and ontologies

Apollo assumes that the Chado database has Chado schema v1.2  or greater and has the following ontologies loaded:
1. Relations Ontology
2. Sequence Ontology
3. Gene Ontology


The quickest and easiest way to do this is to use prebuilt Chado schemas.
Apollo provides a prebuilt Chado schema with the necessary ontologies. (thanks to Eric Rasche at [Center for Phage Technology, TAMU](https://cpt.tamu.edu/computer-resources/chado-prebuilt-schema/))


Users can load this prebuilt Chado schema as follows:
```
createdb CHADO_DB
gunzip -c chado-schema-with-ontologies.sql.gz | psql -U CHADO_USER -h DATABASE_HOST -d CHADO_DB
```

e.g., 

```
createdb apollo-chado
gunzip -c chado-schema-with-ontologies.sql.gz | psql -U postgres -h localhost -d apollo-chado
```

Note that you will also need to do this for your testing and production instances, as well.  

### Configure data sources

In `apollo-config.groovy`, specify the proper database name, database user name and database user password.

### Export via UI

Users can export existing annotations to the Chado database via the Annotator Panel -> Ref Sequence -> Export.

### Export via web services

Users can also leverage the Apollo web services API to export annotations to Chado.
As a demonstration, a sample script, `export_annotations_to_chado.groovy` is provided.

```
export_annotations_to_chado.groovy -organism ORGANISM_COMMON_NAME -username APOLLO_USERNAME -password APOLLO_PASSWORD -url http://localhost:8080/apollo
```
