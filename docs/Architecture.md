## Architecture notes

### Overview and developer's guide

See the [build doc](Apollo2Build.md) for the official developer's guide.

Minimally, the apollo application can be launched by running `apollo run-local`. This starts up a temporary tomcat
server automatically. It will also simply use a in-memory H2 database if a different database configuration isn't setup
yet.

For development purposes, you can also enable automatic code reloading which helps for fast iteration.
    
    
- `grails -reloading run-app` will allow changes to the server side code to be auto-reloaded. 
- `ant devmode` will provide auto-reloading of GWT code changes
- `scripts/copy_client.sh` will copy the plugin code to the web-apps folder to update the plugin javascript

The `apollo` script automatically does several of these functions.


Note: Changes to domain/database objects will require an application restart, but, a very cool feature of our
application is that the whole database doesn't need reloading after a database change.

If you look at the `apollo` binary, you'll see that the code for `grails run-app` and others are automatically launched
during `apollo run-local`.

Also, as always during web development, yoe will want to clear the cache to see changes ("shift-reload" on most
browsers).

### Overview

![](architecture2.png)

[PDF schema](https://github.com/GMOD/Apollo/blob/master/docs/schemaupdates.pdf)

The main components of the Apollo 2.x application are:

- [Grails 2 Server](http://grails.org) with the current version set in the [application.properties](https://github.com/GMOD/Apollo/blob/master/application.properties)
- Datastore: configured via Hibernate / Grails whcih can use most anything supported by JDBC / hibernate (primarily,
  Postgres, MySQL, H2)
- JBrowse / Apollo Plugin: JS / HTML5 [JBrowse doc](http://jbrowse.org/code/JBrowse-1.11.6/docs/) and [main
  site](http://jbrowse.org)
- GWT client: provides the sidebar.   Can be written in another front-end language, as well.  [GWT
  doc](http://www.gwtproject.org/)


### Basic layout

- Grails code is in normal grails directories under "grails-app"
- GWT-only code is under "src/gwt"
    - Code shared between the client and the server is under "src/gwt/org/bbop/apollo/gwt/shared"
- Client code is under "client" (still)
- Tests are under "test"
- Old (presumably inactive code) is under "src/main/webapp"
- New source (loaded into VM) is under "src/java" or "src/groovy" except for grails specific code.
- Web code (not much) is either under "web-app" (and where jbrowse is copied) or under "grails-app/assets" (these are
  compiled down).
- GWT-specifc CSS can also be found in: "src/gwt/org/bbop/apollo/gwt/client/resources/" but it inherits the CSS on its
  current page, as well.



#### Main components

The main components of the Apollo 2.x application (the four most important are 1 through 4):

1. The domain classes; these are the main objects
2. Controllers, which route those domains and provide URL routes; provides rest services
3. Views: annotator and index and the only ones that matter for Apollo
4. Services: very important because all of the controllers should typically have routes, then particular business logic
  should go into the service.
5. Configuration files: The grails-app/conf folder contains central conf files, but the apollo-config.groovy
  file in your root directory can override these central configs (i.e. it is not necessary to edit DataSource.groovy)
6. Grails-app/assets: all your javascript live here. efficient way to deliver this stuff
7. Resources: web-app directory: css, images, and the jbrowse directory + WA plugin are initialized here. 
8. Client directory: The WA plugin is copied or compiled along with jbrowse to the web-app directory

### Schema/domain classes

Domain classes: the most important domain class everywhere is the Feature; it is the key to everything that we do. The
way a domain class is built: 

The domain classes represent a database table. The way it works with "Feature", which is inherited by many other
classes, is that all features are stored in the same table, the difference is that in SQL, there is a class table and
when it pulls these tables from the database --- it queries it and then converts it into the right class.  There are a
number of constrains you can set. 

Very important: the hasMany maps the one-to-many relationship within the database. It can have many locations. the
parentFeatureRelationships is where you map this one-to-many relationship.  You also have to have a single item
relationship.

You can add extra methods to the domain objects, but this is generally not necessary. 

Note: In the DataStore configuration, setting called "auditable = true" means that a new table, a feature auditing tool,
is keeping track of history for the specified objects

#### Feature class

All features inherit an ontologyId and specify a cvTerm, although CvTerms are being phased out.

Subclasses of "Feature" will specify the ontologyId, but "Feature" itself is too generic, for example, so it does not
have an ontologyId.


#### Sequence class

Sequences are the method for WA to grabs sequences used to have a cache built-in mechanism doesn't want to have that
anymore to avoid running into memory problems.


#### Feature locations

Features such as genes all have a feature location belongs to a particular sequence. If you have a feature with
subclasses, it can exist within many locations, and each location belongs to its own sequence.

#### Feature relationship

Feature relationships can define parent/child relationships as well as SO terms i.e. SO "part_of" relationships

#### Feature enums

The FeatureString enum: allows for mapping names for concepts, and it is useful to use these enums without worrying
about string mappings inside the application.


### Running the application

If you go through and run this grails application when you send the URL request, then methods that are sent through the
AnnotationEditorController  (formerly called AnnotationEditorService) dynamically calls a method using handleOperation.

The AnnotatorController serves the page that the annotator is on. This doesn't map to a particular domain object.

In most cases when we have these methods, it unwraps the data that is sent through into JSON object as a set of
variables. Then it is processed into java objects and routed back to JSON to send back. 

When annotator creates a transcript, it is then released to requestHandlingService and it sends it to an annotation
event, which sends it to a WebSocket, and it's then broadcasted to everyone.

#### Websockets and listeners

All clients subscribe to AnnotationNotifications for new transcripts and events.

If an add_transcript operation occurs, this is broadcasted via the websocket. The server side broadcasts this event, and
then it does a JSON roundtrip to render the results and sends the return object that belongs to an AnnotationEvent.
 
Procedure transcript is created --> goes to the server --> adds a transcript locally --> announces it to everyone.

We used to use long polling request model for "push notifications" but now we use Spring with the SockJS, which uses
websockets but it can fall back to long-polling.

There is another component of the broadcasting called brokerMessagingTemplate is the converter to broadcast the event


#### Controllers

Grails controllers are a fairly easy concept for "routing" URLs and info to methods in the code.


#### Services

Grails services are classes that perform business logic.  (In IntelliJ, these are indicated by green buttons on the
definitions to show that these are Injected Spring Bean classes)

The word @Transactional means that every operation that is not private is handled via a transaction. In the old model
there were a lot of files that were recreated each time, even though they did the same. Now we define a class and can
use it again and again. And there can be transactions within transaction. I could also call other services within
services.

addTranscript generateTranscript

The different services do exactly what their name implies. It may not always be clear in what particular service each
class should be in, but it can be changed later. It is easy also to make changes to the names as well. 



##### Grails views

- Most of Views are under grails-app
  - everything conforms to the MVC backend model for the Grails application. 
- Most of java, css, html is under web-app directory
  - Application logic for groovy, gwt, java, etc live here. we could put our old servlets there, but not recommended. 





### Main configuration

The central configuration files are defined in grails-app/conf/ folder, however the user normally only edits their
personal config in apollo-config.groovy. That is because the user config file will override those in the central
configuration. See [Configure.md](Configure.md) for details.

#### Database configuration


The "root" database configuration is specified by grails-app/conf/DataSource.groovy but it is generally over-ridden by
the user's apollo-config.groovy


It is recommended that the user takes sample-postgres-apollo-config.groovy or sample-mysql-apollo-config.groovy and
copies it to apollo-config.groovy for their application.



The default database driver is the h2 database, which is an "embedded" database that doesn't require installing postgres
or mysql, but it is not generally seen as performant as postgres or mysql though. 


Note: there are three environments that can be setup: a development environment, a test environment, and a production
environment, and these are basically assigned automatically depending on how you deploy the app.

* Development environment - "apollo run-local" or "apollo debug"

* Test environment - "apollo test"

* Production environment - "apollo deploy" 


Note: If there are no users and no annotations, a bootstrap procedure can also automatically create some annotations and
users to start up the app so there is something in there to begin with.


#### UrlMapping configuration:

The UrlMappings are stored in grails-app/conf/UrlMappings.groovy

The UrlMappings sets up a mapping from routes to controllers

Standard and customized mappings go in here. The way we route jbrowse to organism data directories is also controlled
here.  The organismJBrowseDirectory is set for a particular session, per user. If none specified, it brings up a default
one. 


#### Build configuration

The build configuration is stored in grails-app/conf/BuildConfig.groovy

If there are libraries that are missing are are to be added, you can add them here.

Additionally, the build system uses the "apollo" script and the "build.xml" to control the compilation and resource
steps.


#### Central config

The central configuration is stored in grails-app/conf/Config.groovy


The central Grails config contains logging, app config, and also can reference external configs. The external config can
override settings without even touching the application code using this method

In our application, we use the apollo-config.groovy then everything in there supersedes this file.

The log4j area can enable logging levels. You can turn on the "debug grails.app" to output all the webapollo debug info,
or also set the "grails.debug" environment variable for java too.

There is also some Apollo configuration here, and it is mostly covered by the [configuration section](Configure.md).



### GWT web-app

When GWT compiles, it loads files into the web-app directory. When it loads up annotator, it goes to annotator index
(the way things get loaded) it does an include annotator.nocache.js file, and with that, it includes all GWT stuff for
the /annotator/index route. The src/gwt/org/bbop/apollo/gwt/ contains much code and the
src/gwt/org/bbop/apollo/gwt/Annotator.gwt.xml is a central config file for the GWT web-app.



#### User interface definitions

A Bootstrap/GWT interface handles the tabs on the right for the new UI.  The annotator object is at the root of
everything.

Example definition: MainPanel.ui.xml 


### Tests

#### Unit tests

Unit tests and some basic javascript tests are running on Travis-CI (see .travis.yml for example script).

You can also run "apollo test" to run the tests locally. It will use the "test" database configuration automatically. 


Also see the [testing notes](Testing_notes.md) for more details.

