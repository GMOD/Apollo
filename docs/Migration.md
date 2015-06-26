## Web Apollo 2.0 Migration Guide

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/index.md">On GitHub</a>

This guide explains how to prepare your 2.0 instance as well as migrating data from previous instances.

In all cases you will need to follow the [guide for setting up your 2.0 instance](Apollo2Build.md).   


## Migration from Evaluation to Production:

If you are running your evaluation/development version using ```./apollo run-local```
when you setup your production instance, any prior annotations will use a separate database.  

If you are using the same production instance you can use scripts to [delete all annotations and preferences](../scripts/delete_all_features.sh)
or [just the annotations](../scripts/delete_only_features.sh).  

If you want to start from scratch (including reloading organisms and users), you can just drop the database (when the server is not running)
and the proper tables will be recreated on startup.

## Migration from 1.0 to 2.0:

In each case you will need to use [Apollo 2 web services](web_services_link.png).  We provide examples in the form of [migration scripts](web_services/examples).  

To use the newer examples written in ```groovy```, you'll need to install it in a similar manner as you did grails:  ```gvm install groovy```.   If you have not installed [gvm](http://gvmtool.net/) (or grails) you should be able to run '''curl -s get.gvmtool.net | bash
'''.

We have written new examples using the groovy language, but mostly any language will work (Perl, shell/curl, Python, etc.).


### Migrate Annotations

We provide a [migration script](web_services/examples/groovy/migrate_annotations1to2.groovy) that 
connects to a single Web Apollo 1 instance and populates the annotations for an organism for a set of sequences / (confusingly called tracks as well).  It would be best to develop your script on a development instance of Apollo2 for restricted sequences.

To get the scripts working properly, you'll need to provide the list of sequences (or tracks) to migrate for each organism.
You can get the list of tracks by either using the database (```select * from tracks ;```) or looking 
in the Web Apollo annotations directory (e.g., ```/opt/apollo/annotations/ | grep Annotations | grep -v history | paste -s -d"," - ```).  The latter will only yield tracks that have annotations.


### Migrate Users

You have to add users de novo using something like the [add_users script](web_services/examples/groovy/add_users.groovy). 
In this case you create a csv file (you can start in Excel) with the email, name, password, and role ('user' or 'admin'). 
This is passed into the add_users.groovy script and users are added.  

From Web Apollo 1, you should be able to pull user names out of the database ```select * from users ;```, but there is not much overlap between users in Web Apollo 1 and Apollo2.

If you have only a few users, however, just adding them manually on the users will likely be easier. 

### Add Organisms

If possible adding organisms on the organisms tab is the easiest option if you only have a handful of organisms.  

However, we provide a mechanism via the [add_organism.groovy script](web_services/examples/groovy/add_organism.groovy).
In this case organisms are added individually as organisms are on separate tomcat instances.



