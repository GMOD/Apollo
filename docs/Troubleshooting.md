
### Tomcat memory

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Troubleshooting.md">On GitHub</a>


Typically, the default memory allowance for the Java Virtual Machine (JVM) is too low. The memory requirements for Web Apollo will depend on many variables, but in general, we recommend at least 1g for the heap size and 256m for the PermGen size as a starting point. 

#### Suggested Tomcat memory settings

    export CATALINA_OPTS="-Xms512m -Xmx1g -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:+UseConcMarkSweepGC -XX:MaxPermSize=256m"

In cases where the assembled genome is highly fragmented, additional tuning of memory requirements and garbage collection will be necessary to maintain the system stable. Below is an example from a research group that maintains over 40 Apollo instances with assemblies that range from 1,000 to 150,000 scaffolds (reference sequences):  

    export CATALINA_OPTS="-Xmx12288m -Xms8192m -XX:PermSize=256m -XX:MaxPermSize=1024m -XX:ReservedCodeCacheSize=64m -XX:+UseG1GC -XX:+CMSClassUnloadingEnabled -Xloggc:$CATALINA_HOME/logs/gc.log -XX:+PrintHeapAtGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

To change your settings, you can edit the setenv.sh script in 
`$TOMCAT_BIN_DIR/setenv.sh` where `$TOMCAT_BIN_DIR` is where the directory where the Tomcat binaries reside. It is possible that this file doesn't exist by default, but it will be picked up when Tomcat restarts.


#### Confirm your settings

`ps -ef | grep -i tomcat`

You should see your memory settings in the command that runs tomcat.   

`/usr/local/bin/java -Dfile.encoding=UTF-8 -Xmx2048M -Xms64M -XX:MaxPermSize=1024m `

#### Re-install after changing settings

If you start seeing memory leaks (`java.lang.OutOfMemoryError: Java heap space`) after doing an update, you might try re-installing, as the live re-deploy itself can cause memory leaks or an inconsistent software state. 

If you have named your web application named "WebApollo.war" then you can remove all of these files from your webapps directory and re-deploy.

- `apollo deploy`  (or `apollo release` for javascript-minimization)
- stop tomcat
- `rm -f $TOMCAT_WEBAPPS_DIR/webapps/WebApollo.war && sudo rm -rf $TOMCAT_WEBAPPS_DIR/webapps/WebApollo`
- start tomcat 
- `cp target/apollo-XXX.war $TOMCAT_WEBAPPS_DIR/webapps/WebApollo.war`

### Tomcat permissions

Preferably, when running WebApollo or any webserver, you should not run Tomcat as root. Therefore, when deploying your war file to tomcat or another web application server, you may need to tune your file permissions to make sure Tomcat is able to access your files.

On many production systems, tomcat will typically belong to a user and group called something like 'tomcat'. Make sure that the 'tomcat' user can read your "webapps" directory (where you placed your war file) and write into the annotations and any other relevant directory (e.g. tomcat/logs).   As such, it is sometimes helpful to add the user you logged-in as to the same group as your tomcat user and set group write permissions for both.

Consider using a package manager to install Tomcat so that proper security settings are installed, or to use the jsvc 
http://tomcat.apache.org/tomcat-7.0-doc/security-howto.html#Non-Tomcat_settings


### Errors running JBrowse scripts

##### e.g. "Can't locate Hash/Merge.pm in @INC" or "Can't locate JBlibs.pm in @INC"

If are trying to run the jbrowse binaries but get these sorts of errors, try `install_jbrowse.sh` which will initialize as many pre-requisites as possible including JBLibs and other JBrowse dependencies. 

### Errors during apollo deploy or install_jbrowse.sh

##### e.g. "cd: src/main/webapp/jbrowse: No such file or directory"

If you get this error, then you may need to re-run `apollo deploy` or even do a `apollo clean-all && apollo deploy`.


### Differences between JBrowse and WebApollo

The "linkTemplate" track configuration parameter in JBrowse is overridden by WebApollo's feature edge matcher and drag and drop functions. It is recommended to use menuTemplate instead.

### Complaints about 8080 being in use

Please check that you don't already have a tomcat running ``netstat -tan | grep 8080``.  
Sometimes tomcat does not exit properly.  ``ps -ef | grep java`` and then ``kill -9`` the offending processing.

### Unable to open the h2 / default database for writing

```
SEVERE: Unable to create initial connections of pool.
org.h2.jdbc.JdbcSQLException: Error opening database: "Could not save properties /var/lib/tomcat7/prodDb.lock.db" [8000-176]
```

This is due to the production server trying to write an h2 instance in an area it doesn't have permissions to.
If you use H2 (which is great for testing or single-user user, but not for full-blown production) make sure that:

- you can write to the specified data directory for production.  Here is the default from ```DataSource.groovy```:

            url = "jdbc:h2:/tmp/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            
which will write a db file to ```/tmp/prodDB.db```.  If you don't specify an absolute path it will try to write in the same directory
that tomcat is running in e.g., ``/var/lib/tomcat7/``.

The solution is to copy over one of the ``sample-XXX-config.groovy`` files to ``apollo-config.groovy`` in your home directory and configure for your needs.

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Configure.md">More detail on configuration</a> of the ``apollo-config.groovy`` file.


### Cache Error

In some instances you can't write to the default cache location on disk.  Part of an example config log:

```
2015-07-03 14:37:39,675 [main] ERROR context.GrailsContextLoaderListener  - Error initializing the application: null
    java.lang.NullPointerException
            at grails.plugin.cache.ehcache.GrailsEhCacheManagerFactoryBean$ReloadableCacheManager.rebuild(GrailsEhCacheManagerFactoryBean.java:171)
            at grails.plugin.cache.ehcache.EhcacheConfigLoader.reload(EhcacheConfigLoader.groovy:63)
            at grails.plugin.cache.ConfigLoader.reload(ConfigLoader.groovy:42)
```

There are several solutions to this, but all involve updating the ```apollo-config.groovy``` file to override the caching defined in the [Config.groovy](https://github.com/GMOD/Apollo/blob/master/grails-app/conf/Config.groovy#L103).

1) Disabling the cache:
```
grails.cache.config = {
    cache {
        enabled = false
        name 'globalcache'
    }
}
```

2) Disallow writing overflow to disk (best solution for small installations):

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
        overflowToDisk false   // THIS IT THE IMPORTANT LINE!
        maxElementsInMemory 100000
    }
}
```

3) Specify the overflow directory (best for high load servers, which will need the cache).  Make sure your tomcat / web-server user can write to that directory:
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


Information on the [grails ehcache plugin](http://grails-plugins.github.io/grails-cache-ehcache/guide/usage.html) (see "Overriding values") and
[ehcache itself](http://ehcache.org/documentation/2.8/integrations/grails).
 

