# Migration guide

This guide explains how to prepare your Apollo 2.x instance, and to migrate data from previous Web Apollo versions
into 2.0.

In all cases you will need to follow the [guide for setting up your 2.x instance](Apollo2Build.md).


## Migration from Evaluation to Production:

If you are running your evaluation/development version using `./apollo run-local` when you setup your production
instance, any prior annotations will use a separate database.  

If you are using the same production instance you can use scripts to delete all annotations and preferences:

`scripts/delete_all_features.sh`

or just the annotations:

`scripts/delete_only_features.sh`

If you want to start from scratch (including reloading organisms and users), you can just drop the database (when the
server is not running) and the proper tables will be recreated on startup.

## Migration from 2.0.X to 2.0.Y on production:

### Installation from a downloaded release

- [Download the desired Apollo release](https://github.com/GMOD/Apollo/releases/) from the bottom of each release.   Official releases will be tagged as "Release" and have a green label.
- Expand the archive. 
- Copy your existing apollo-config.groovy file into the directory. 
- Always backup your database!
- Create a new war file as below: ```./apollo deploy```.
- Turn off tomcat and remove the old apollo directory and ```.war``` file in the webapps folder.
- Copy in new .war file with the same name.
- Restart tomcat and you are ready to go.

Note if you you choose to have two different versions of Apollo running, though need to point to different database instances or you will experience problems.

### Installation from a checked out github

If you want bleeding and only moderately tested code (not recommended unless you feel you know what you're doing), you can clone Apollo directly from our source page https://github.com/GMOD/Apollo/

Any upgrading can be taken care of during a pull.  Please note that as we sometimes change the version of JBrowse, so you should do:

```./apollo clean-all```

before building a target for production.

You can the follow the directions for deploying a downloaded release, above.


## Migration from 1.0 to 2.0:

We provide examples in the form of [migration scripts](https://github.com/gmod/apollo/tree/master/docs/web_services/
examples) in the docs/web_services/examples directory. These tools are also described in the [command line tools 
section](Command_line.md).

We have written many of the [command line tools](Command_line.md) examples using the groovy language, but mostly any
language will work (Perl, shell/curl, Python, etc.).


### Migrate Annotations

We provide a [migration script](https://github.com/gmod/apollo/tree/master/docs/web_services/examples/groovy/
migrate_annotations1to2.groovy) that connects to a single Web Apollo 1 instance and populates the annotations for an
organism for a set of sequences / (confusingly called tracks as well).  It would be best to develop your script on a
development instance of Apollo2 for restricted sequences.

To get the scripts working properly, you'll need to provide the list of sequences (or tracks) to migrate for each
organism.  You can get the list of tracks by either using the database (`select * from tracks ;`) or looking in the Web
Apollo annotations directory

``` 
ls -1 /opt/apollo/annotations/ | grep Annotations | grep -v history | paste -s -d"," -
```



### Migrate Users

You have to add users de novo using something like the [add_users.groovy
script](https://github.com/gmod/apollo/tree/master/docs/web_services/examples/groovy/add_users.groovy). In this case you
create a csv file with the email, name, password, and role ('user' or 'admin'). This is passed into the add_users.groovy
script and users are added.  

From Web Apollo 1, you should be able to pull user names out of the database `select * from users ;`, but there is not
much overlap between users in Web Apollo1.x and Apollo2.x.

If you have only a few users, however, just adding them manually on the users will likely be easier. 

### Add Organisms

If possible adding organisms on the organisms tab is the easiest option if you only have a handful of organisms.  

The [add_organism.groovy script](https://github.com/gmod/apollo/tree/master/docs/web_services/examples/groovy/
add_organism.groovy) can help automate this process if you have a large number of migrations to handle.


