Building WebApollo
--------------------

To build WebApollo, we will first edit the configuration files, and *then* run Maven to create a WAR package.

### Before you build

You need to configure your instance using a config.properties and a
config.xml file, which are copied into the war file.

-   Copy the sample config / logging files to the right location.

<!-- blank code comment -->

    cp sample_config.properties config.properties
    cp sample_config.xml config.xml
    cp sample_log4j2.json log4j2.json
    cp sample_log4j2-test.json log4j2-test.json

-   Edit the config.properties file to point to the appropriate directories. A sample config might look like:

<!-- blank comment comment -->

    jbrowse.data=/apollo/data
    datastore.directory=/apollo/annotations
    database.url=jdbc:postgresql:web_apollo_users
    database.username=postgres_user
    database.password=postgres_password
    organism=Pythium ultimum

**IMPORTANT: the jbrowse.data directory should not be placed
anywhere inside the Tomcat webapps folder, not even using
symlinks!! To avoid data loss when doing Tomcat Undeploy operations,
users are advised not to be modifying the contents of the webapps folder
for anything besides deploying new WAR files**

### Building the servlet

Web Apollo uses maven to create a WAR package which bundles your config files. After setting up the config, you can use the command:

    mvn package

This will produce the a WAR file in WEB\_APOLLO\_DIR/target/ (e.g. target/apollo-1.0-SNAPSHOT.war) that is ready to deploy. See [deploying webapollo](Deploy.md) for the next steps for deploying.

Alternatively, developers or user's with custom builds are advised to review the [developer's guide](Developer.md) for further instructions on creating their own builds.
