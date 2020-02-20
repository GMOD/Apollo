# Troubleshooting guide

### Tomcat memory


Typically, the default memory allowance for the Java Virtual Machine (JVM) is too low. The memory requirements for Web
Apollo will depend on many variables, but in general, we recommend at least 512g for the maximum memory and 512m for the minimum, though a 2 GB maximum seems to be optimal for most server configurations.

#### Suggested Tomcat memory settings

``` 
export CATALINA_OPTS="-Xms512m -Xmx2g \
        -XX:+CMSClassUnloadingEnabled \
        -XX:+CMSPermGenSweepingEnabled \
        -XX:+UseConcMarkSweepGC"
```


In cases where the assembled genome is highly fragmented, additional tuning of memory requirements and garbage
collection will be necessary to maintain the system stable. Below is an example from a research group that maintains
over 40 Apollo instances with assemblies that range from 1,000 to 150,000 scaffolds (reference sequences) and over one hundred users:  

``` 
export CATALINA_OPTS="-Xmx12288m -Xms8192m \
        -XX:ReservedCodeCacheSize=64m \
        -XX:+UseG1GC \
        -XX:+CMSClassUnloadingEnabled \
        -Xloggc:$CATALINA_HOME/logs/gc.log \
        -XX:+PrintHeapAtGC \
        -XX:+PrintGCDetails \
        -XX:+PrintGCTimeStamps"
```

To change your settings, you can *usually* edit the setenv.sh script in `$TOMCAT_BIN_DIR/setenv.sh` where
`$TOMCAT_BIN_DIR` is  the directory where the Tomcat binaries reside.  It is possible that this file doesn't exist by default, but it will be picked up when Tomcat restarts.  Make sure that tomcat can read the file.  

In most cases, creating the setenv.sh should be sufficient but you may have to edit a catalina.sh or another file directly depending on your system and tomcat setup.  For example, on Ubuntu, the file /etc/default/tomcat7 often contains these settings. 

#### Confirm your settings

Your CATALINA_OPTS settings from setenv.sh can be confirmed with a tool like jvisualvm or via the command line with the
`ps` tool.  e.g. `ps -ef | grep java`  should yield something like the following allowing you to confirm that your memory settings have been picked up.

```
root      9848     1  0 Oct22 ?        00:36:44 /usr/lib/jvm/java-8-openjdk-amd64/bin/java -Djava.util.logging.config.file=/usr/local/tomcat/current/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Xms1g -Xmx2g -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:+UseConcMarkSweepGC -Dj
```

#### Re-install after changing settings

If you start seeing memory leaks (`java.lang.OutOfMemoryError: Java heap space`) after doing an update, you might try
re-installing, as the live re-deploy itself can cause memory leaks or an inconsistent software state. 

If you have named your web application named `Apollo.war` then you can remove all of these files from your webapps
directory and re-deploy.

- Run `apollo deploy`  
- Undeploy any existing Apollo instances
- Stop tomcat
- Copy the war file to the webapps folder
- Start tomcat 

### Tomcat permissions

Preferably, when running Apollo or any webserver, you should not run Tomcat as root. Therefore, when deploying your
war file to tomcat or another web application server, you may need to tune your file permissions to make sure Tomcat is
able to access your files.

On many production systems, tomcat will typically belong to a user and group called something like 'tomcat'. Make sure
that the 'tomcat' user can read your "webapps" directory (where you placed your war file) and write into the annotations
and any other relevant directory (e.g. tomcat/logs).   As such, it is sometimes helpful to add the user you logged-in as
to the same group as your tomcat user and set group write permissions for both.

Consider using a package manager to install Tomcat so that proper security settings are installed, or to use the jsvc
http://tomcat.apache.org/tomcat-7.0-doc/security-howto.html#Non-Tomcat_settings


### Errors with JBrowse

#### JBrowse tools don't show up in ```bin``` directory (or install at all) after install or typing ```install_jbrowse.sh```

If the ```bin``` directory with JBrowse tools doesn't show up after calling ```install_jbrowse.sh``` JBrowse is having trouble installing itself for a few possible reasons.   If these do not work, please observe the [JBrowse troubleshooting](http://jbrowse.org/code/JBrowse-1.12.1/docs/tutorial/#Troubleshooting) and [JBrowse install](https://jbrowse.org/install/) pages, as well and the ```setup.log``` file created during the installation process. 

##### cpanm or other components are not installed

Make sure the [appropriate JBrowse libraries](http://gmod.org/wiki/JBrowse_Configuration_Guide#Making_a_New_JBrowse) are installed on your system.

If you see ```chmod: cannot access `web-app/jbrowse/bin/cpanm': No such file or directory ``` make sure to install [cpanm](http://search.cpan.org/~miyagawa/App-cpanminus-1.7040/lib/App/cpanminus.pm).

##### Git tool is too old

Git expects to clone a single branch which is supported in git 1.7.10 and greater.  The output when that fails looks something like this:

```
Buildfile: build.xml

copy.apollo.plugin.webapp:

setup-jbrowse:

git.clone:
[exec] Result: 129
```

The solution is to upgrade git to 1.7.10 or greater or remove the line with the ```--single-branch``` option in ```build.xml```. 

##### Accessing git behind a firewall. 

If you are behind a firewall, checking out code using the ```git://``` protocol may not be allowed, but that is the default.    The output will look something like this:

```
setup-jbrowse:

git.clone:
     [exec] Submodule 'src/FileSaver' (git://github.com/dkasenberg/FileSaver.js.git) registered for path 'src/FileSaver'
     [exec] Submodule 'src/dbind' (git://github.com/rbuels/dbind.git) registered for path 'src/dbind'
    . . . .
     [exec] Submodule 'src/xstyle' (git://github.com/kriszyp/xstyle.git) registered for path 'src/xstyle'
     [exec] Result: 1
```

with possibly more output below. 

Type:

```git config --global url."https://".insteadOf git://``` 

in the command-line and then re-install using ```./apollo clean-all``` ```./apollo run-local``` (or deploy).



#### e.g. "Can't locate Hash/Merge.pm in @INC" or "Can't locate JBlibs.pm in @INC"

If you are trying to run the jbrowse binaries but get these sorts of errors, try running `install_jbrowse.sh` which will
initialize as many pre-requisites as possible including JBLibs and other JBrowse dependencies. 

#### Rebuilding JBrowse

You can manually clear jbrowse files from web-app/jbrowse and re-run `apollo deploy` to rebuild JBrowse.

#### RequestError: Unable to load ... Apollo2/jbrowse/data/trackList.json status: 500

Apollo2 does fairly strict JSON validation so make sure your trackList.json file is valid JSON

If you still get this error after validating please forward the issue to our github issue tracker.


### Complaints about 8080 being in use

Please check that you don't already have a tomcat running `netstat -tan | grep 8080`. Sometimes tomcat does not exit
properly.  `ps -ef | grep java` and then `kill -9` the offending processing.

Note that you can also configure tomcat to run on different ports, or you can launch a temporary instance of apollo with
`apollo run-local 8085` for example to avoid the port conflict.

### Unable to open the h2 / default database for writing

If you receive an error similar to this:

``` 
SEVERE: Unable to create initial connections of pool.
org.h2.jdbc.JdbcSQLException: Error opening database: 
    "Could not save properties /var/lib/tomcat7/prodDb.lock.db" [8000-176]
```

Then this is due to the production server trying to write an h2 instance in an area it doesn't have permissions to.  If
you use H2 (which is great for testing or single-user user, but not for full-blown production) make sure that:

You can modify the specified data directory for the H2 database in the apollo-config.groovy. For example, using the
/tmp/ directory, or some other directory:

``` 
url = "jdbc:h2:/tmp/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
```

This will write a H2 db file to `/tmp/prodDB.db`.  If you don't specify an absolute path it will try to write in the
same directory that tomcat is running in e.g., `/var/lib/tomcat7/` which can have permission issues.


More detail on database configuration when specifying the `apollo-config.groovy` file is available in the
[setup guide](Setup.md).


### Grails cache errors

In some instances you can't write to the default cache location on disk.  Part of an example config log:

``` 
2015-07-03 14:37:39,675 [main] ERROR context.GrailsContextLoaderListener  - Error initializing the application: null
java.lang.NullPointerException
        at grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean$ReloadableCacheManager.rebuild(GrailsEhCacheManagerFactoryBean.java:171)
        at grails.plugin.cache.ehcache.EhcacheConfigLoader.reload(EhcacheConfigLoader.groovy:63)
        at grails.plugin.cache.ConfigLoader.reload(ConfigLoader.groovy:42)
```

There are several solutions to this, but all involve updating the `apollo-config.groovy` file to override the caching 
defined in the [Config.groovy](https://github.com/GMOD/Apollo/blob/master/grails-app/conf/Config.groovy#L103).

#### Disabling the cache:

``` 
    grails.cache.config = {
        cache {
            enabled = false
            name 'globalcache'
        }
    }
```

This can also be done by removing the plugin.  In [```grails-app/conf/BuildConfig```](https://github.com/GMOD/Apollo/blob/master/grails-app/conf/BuildConfig.groovy) remove / comment out the line and re-building:

     compile ':cache-ehcache:1.0.5'


#### Disallow writing overflow to disk

Can be used for small instances

``` 
    grails.cache.config = {
        // avoid ehcache naming conflict to run multiple WA instances
        provider {
            name "ehcache-apollo-"+(new Date().format("yyyyMMddHHmmss"))
        }
        cache {
            enabled = true
            name 'globalcache'
            eternal false
            overflowToDisk false   // THIS IS THE IMPORTANT LINE
            maxElementsInMemory 100000
        }
    }
```

#### Specify the overflow directory

Best for high load servers, which will need the cache.  Make sure your tomcat /
web-server user can write to that directory:

``` 
    // copy from Config.groovy except where noted
    grails.cache.config = {
    ... 
        cache {
        ...  
            maxElementsOnDisk 10000000
            // this is the important part, below!
            diskStore{
                path '/opt/apollo/cache-directory'
            }
        }
        ...
    }
```

#### JSON in the URL with newer versions of Tomcat

When JSON is added to the URL string (e.g., `addStores` and `addTracks`) you may get this error with newer patched versions of Tomcat 7.0.73, 8.0.39, 8.5.7:

     java.lang.IllegalArgumentException: Invalid character found in the request target. The valid characters are defined in RFC 7230 and RFC 3986

To fix these, the best solution we've come up with (and there may be many) is to explicitly allow these characters, which you can do starting with Tomcat versions: 7.0.76, 8.0.42, 8.5.12.
This is done by adding the following line to `$CATALINA_HOME/conf/catalina.properties`:

    tomcat.util.http.parser.HttpParser.requestTargetAllow=|{}

Information on the [grails ehcache plugin](http://grails-plugins.github.io/grails-cache-ehcache/guide/usage.html) (see
"Overriding values") and [ehcache itself](http://ehcache.org/documentation/2.8/integrations/grails).

#### Java mismatch
If you get an ```Unsupported major.minor error``` or similar, please confirm that the version of java that tomcat is 
running ```ps -ef | grep java``` is the same as the one you used to build. Setting JAVA_HOME to the Java 8 JDK should fix most problems.


### Mysql invalid TimeStamp error

For certain version of MySQL we might get errors of this nature:

> SQLException occurred when processing request: [GET] /apollo/annotator/getAppState
Value '0000-00-00 00:00:00' can not be represented as java.sql.Timestamp. Stacktrace follows:
java.sql.SQLException: Value '0000-00-00 00:00:00' can not be represented as java.sql.Timestamp

The fix is to set the ```zeroDateTimeBehavior=convertToNull``` to the url connect screen.  Originally [identified here](https://github.com/GMOD/Apollo/issues/1170).  Here is an example URL:


    jdbc:mysql://localhost/apollo_production?zeroDateTimeBehavior=convertToNull&autoReconnect=true&characterEncoding=UTF-8&characterSetResults=UTF-8
