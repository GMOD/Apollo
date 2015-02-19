# Apollo 2.0 Code Review
# 2015-02-18
# Notes by M. Munoz-Torres

Present at BBOP: Nathan, Moni
Present at UM: Colin, Deepak


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


Colin: this is still very similar to Chado. What do we want to do about that? 
Nathan: I am less concerned with in which ways it is similar.
Colin: we are making changes to improve our application, and that's "pretty awesome", but if we are going to support Chado export, we'll have to find a way to map back to Chado. Then won't we be back on the same boat? 
Nathan: Not worried about that right now. There are many ways to go back to Chado. In an ideal world you could write a plugin to Chado and separate everything in the code, Chado from the architecture. We want to keep everything separated in the code.

in the code you go through iteration thing looking a part_of
we are still using ontology, and want to keep it; the data structure, the way you handle features or markers. we can create a service model with these tools, and we could create a separate Apollo Chado plugin (you can create inheretable plugins, which are similar to the main project, but with a few modifications).
If you have an active Chado schema, you could create a grails project out of that. the hard part will be writing a service mapping between what Apollo is storing and what Chado is storing. It will be easier.

Controlers:
If you are writing a regular web-application,,, didn't complete sentence.

Experiment is also a domain, 
ideally we will have a single page, and everything else is going to be a rest operation. 

if you have a def Index it will come in and ,,, didn't complete sentence.
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

Nathan is moving the GBOL code into Service classes, and slowly converting it over.

----------
Colin:
Do we want to make integration tests of the rest API? We don't have a clear vision of what it would be, but could create one. 

Nathan: 
Yes, great idea. Grails supports it out of the box. You just write them like you normally would, Grails creates a db and runs stuff easily.
----------

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

[....]

Next steps: 
Wants to release the RC2 candidate of 1.0.4 on icebox.
What makes the most sense for Deepak and Colin to work on? 
- Colin on the client side, primarily JBrowse. 
- Deepak on ?? Is there something you find interesting to do? 
-- Nathan could set up unit tests and have him write a few test. 
-- There is also a GFF3 export, or 
-- anything else on the 2.0 column of Trello board. When one of those things catches your interest, you could start going down there.
Answer: Deepak will start working on the GFF3 export.

It would be good to have Colin and Deepak working on the new branch.
Most changes currently live on the gwt piece, and on websockets. 
Colin will begin on a separate branch, to include the changes he has done to the client.
Nathan to create a few examples of unit tests so Colin can follow them. 
Nathan will also work on an integration test.

