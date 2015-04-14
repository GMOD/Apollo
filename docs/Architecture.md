

## Architecture notes:
- Grails code is in normal grails directories under "grails-app"
- GWT-only code is under src/gwt except
    - Code shared between the client and the server is under src/gwt/org/bbop/apollo/gwt/shared
- Client code is under client (still)
- Tests are under "Test"
- Old (presumably inactive code) is under src/main/webapp
- New source (loaded into VM) is under src/java or src/groovy except for grails specific code.
- Web code (not much) is either under web-app (and where jbrowse is copied) or under grails-app/assets (these are compiled down).
- GWT-specifc CSS can also be found in: src/gwt/org/bbop/apollo/gwt/client/resources/ , but it inherits the CSS on its current page, as well.


A doc in progress.

For reference:
https://github.com/GMOD/Apollo/blob/grails1/doc/architecture2.png
https://github.com/GMOD/Apollo/blob/grails1/doc/schemaupdates.pdf

The main components are:
+ Grails Server (formally a simple servlet 3 container)
+ Datastore: configured via Hibernate / Grails . . can use most anything supported by JDBC / hibernate as well as MongoDB (and possibly ElasticSearch) . . in theory
+ JBrowse / Apollo Plugin: JS / HTML5
+ GWT client: provides the sidebar.   Can be written in another front-end language, as well.



GRAILS
Official doc is here:  http://grails.github.io/grails-doc/2.4.x/

The main pieces to a Grails application (the four most important are 1 through 4).
1 - the domain classes; these are the main objects
2 - Controllers, which route those domains and provide URL routes; provides rest services
3 - views: annotator and index and the only ones that matter for Apollo
4 - Services: very important because all of the controllers should typically have routes, then particular business logic should go into the service.
5 - configuration files
6 - grails-app/assets: all your javascript live here. efficient way to deliver this stuff
7 - web-app directory: just css, images, and this is where the jbrowse directory and the WA plugin live.

In Grails views ...
most of things are under grails-app: everuthing conforms to some model to what a grails application is. 
most of java, css, html is under web-app directory
src - groovy, gwt, java, etc live here. we could put our old servlets there, but not recommended. 


Domain classes: most important domain class everywhere is the Feature; is the key to everything that we do. the way a domain class is built (Nathan cleaning as he explains): 
The main class represents a database table, the way it works with "Feature", which is inherited by many other classes. All features are stored in the same table, the differences in SQL, there is a class table and when it pulls these tables from the database --- it queries it and then pulls it (converts it) into the right class.
There are a number of constrains you can set. 

Very important: the hasMany maps the one-to-many relationship within the database. It can have many locations. the parentFeatureRelationships is where you map this one-to-many relationship. 
You also have to have a single item relationship.

You can add any methods you want to.  Nathan tried to not add any methods, except things for FeatureLocation. things that are obvious, but not of any sufficient calculation. 

auditable = true means that a new table, a feature auditing tool, is keeping track of history for the specified objects

all features inherit an ontologyId and specifies a cvTerm
Nathan trying to use ontologyId everywhere he can, and also conserving cvTerm for those places where ontologyId is not present. 

"Feature" is too generic, for example, so it does not have an ontologyId

"Feature": 

Sequence chunk: the way it grabs sequences
used to have a cache built-in mechanism
doesn't want to have that anymore to avoid running into memory problems.


Feature locations: the way this works, you can have a feature, like a gene, and each feature location belongs to a particular seq.
if you have a feature, it can exist within many locations, and each location belongs to its own sequence.
Feature relationship: important also! They had subject and object (Nathan likes parent and child, for convenience). parentFeatureRelationships are things where parentFeature (I am the parent), childFeatureRelationships are the ones where childFeature (I am the child)
There are a few things that are not part of the SO "part_of"

That's Domains. there is probably more stuff in there than there needs to be; Nathan will continue to clean more stuff now.

If you have a def Index it will come in and ,,, didn't complete sentence.
if you go through and run this grails application when you send the URL request  (old JBrowse sent post request to AnnotationEditorService, now sends to AnnotationEditorController; manages everything with a handleOperation)
the method called Operation determines what method is run; because everything is consistent, we can do that. We unwrap the method, figure out the action, and assign it.

These are 90% of the controllers. Most of the other things this controller def does is to handle web service requests. Sometimes we use the IOS controller, but nothing we need to highlight. 
The AnnotatorController serves the page that the annotator is on. This doesn't map to a particular domain object.

Going back into what happens: 
in most cases when we have these methods, it unwraps the data that is sent through into JSON object as a set of variables. Then it is processed into java objects and routed back to JSON to send back. 

How do you handle the collaborative aspect, like adding a transcript? 
when annotator creates a transcript, it is then released to requestHandlingService 
it sends it to an annotation event, which sends it to a WebSocket, and it's then broadcasted to everyone

create socket and listener
subscribe to all topics and AnnotationNotification that come
if add transcript operation occurs, this is broadcasted locally.
then it does a JSON roundtrip to render
while it is in there it also sends the return object into a cube that belongs to an AnnotationEvent

As so: 
transcript is created --> goes to the server --> adds a transcript locally --> announces is to everyone via the client

we used to use long polling before websockets. now Spring is in charge of sending the message.

brokerMessagingTemplate is the converter to broadcast the event


Controllers:
Route url and info to methods


Services: 
is something that does business logic
(green buttons on the left of the intelliJ window in this section shows that it an Injected Spring Bean)

The word @Transactional menas that every operation that is not private is handled via a transaction
in the old model there were a lot of files that were recreated each time, even though they did the same. Now we define a class and can use it again and again. And there can be transactions within transaction. I could also call other services within services.

addTranscript 
generateTranscript


To pull the Editor: …. meh

Views: …. 

The different services do exactly what their name implies. It may not always be clear in what particular service each class should be in, but it can be changed later. It is easy also to make changes to the names as well. 

Nathan created a FeatureString enum: creating names for everything, and it is useful to rename things and not worry about spelling.



Configuration:
Bootstrap gets launched every time you start up a server. Whatever you want to put in that startup goes there. Also processes organisms into the right directory. If there are no users and no annotations, it creates some annotations and users so there is something in there to begin with.

UrlMappings:
standard and customized mappings go in here. 
the way we write jbrowse goes here
I set the organismJBrowseDirectory for a particular session, per user. If none specified, it brings up a default one. 

/org.bbop.apollo/Security filters under UrlMappings

/spring/BuildConfig.groovy
this is how you add stuff. if there are libraries missing, you use this. 

/spring/Config.groovy
this Grails config location says that if there is a file name called -config.groovy then everything in there supersedes this file.
A lot of it is added automatically. 
the log4j area … not worried to much. 

there is an apollo-specific configuration here: some of it has no baring on reality, and it exists because it is created but does not get used. 

/spring/DataSource.groovy
by default uses an h2.Driver
there are three environments: a development one, a test one, and a production environment.
there is a create-drop database for when it is being developed.


web-app

when GWT compiles, it does it into this directory. when it loads up annotator, it goes to annotator index (the way things get loaded) it does an include annotator.nocache.js file, and with that, it includes all GWT stuff. There is also a JAVA file … didn't finish that sentence.

when loading the annotator/index 


there are unit tests pre boxed, not finished
there is also a integration test area. not worked on yet.

A Bootstrap/GWT interface handles the tabs on the right for the new UI.
The annotator object is at the root of everything. the source path is client. 

MainPanel.ui.xml 

