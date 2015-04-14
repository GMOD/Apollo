
### Tomcat memory

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Troubleshooting.md">On GitHub</a>


In many ocassions the default memory allowance is too low. The memory requirements of Web Apollo will depend on the the size of your genome and how many instances of Web Apollo you host in the same Tomcat instance, but in general, we recommend at least 1g for the heap size and 256m for the PermGen size as a starting point. Suggested settings are:

    export CATALINA_OPTS="-Xms512m -Xmx1g -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:+UseConcMarkSweepGC -XX:MaxPermSize=256m"

In cases where the assembled genome is highly fragmented, additional tuning of memory requirements and garbage collection will be necessary to maintain the system stable. Below is an example from a research group that maintains over 40 Apollo instances with assemblies that range from 1,000 to 150,000 scaffolds (reference sequences):  

    "-Xmx12288m -Xms8192m -XX:PermSize=256m -XX:MaxPermSize=1024m -XX:ReservedCodeCacheSize=64m -XX:+UseG1GC -XX:+CMSClassUnloadingEnabled -Xloggc:$CATALINA_HOME/logs/gc.log -XX:+PrintHeapAtGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

To use this setting, edit the setenv.sh script in 
`$TOMCAT_BIN_DIR/setenv.sh` where `$TOMCAT_BIN_DIR` is where the
directory where the Tomcat binaries reside.

#### Memory fixes for special cases
Some members of our community have contributed information on how they 


### Tomcat permissions

When deploying your war file to tomcat or another web application server, you may run into permissions problems in your annotations directory or your web application directory.   Tomcat on many production systems will typically belong to a user and group called something like 'tomcat'.    Make sure that that user can read in the tomcat "webapps" directory (where you placed your war file) and write into the annotations and any other relevant directory (e.g. tomcat/logs).   As such, it is sometimes helpful to add the user you logged-in as to the same group as your tomcat user and set group write permissions for both. 

http://tomcat.apache.org/tomcat-7.0-doc/security-howto.html#Non-Tomcat_settings


### Getting logged out when entering JBrowse

This often indicates that the add-webapollo-plugin.pl script wasn't run properly, which will update JBrowse's configuration and load the Web Apollo plugin. See the [data generation](Data_loading.md) for details on this step.


### Errors running JBrowse scripts

##### e.g. "Can't locate Hash/Merge.pm in @INC" or "Can't locate JBlibs.pm in @INC"

If are trying to run the jbrowse binaries but get these sorts of errors, try `install_jbrowse.sh` which will initialize as many pre-requisites as possible including JBLibs and other JBrowse dependencies. 

### Errors during apollo deploy or install_jbrowse.sh

##### e.g. "cd: src/main/webapp/jbrowse: No such file or directory"

If you get this error, then you may need to re-run `apollo deploy` or even do a `apollo clean-all && apollo deploy`.


### Differences between JBrowse and WebApollo


The "linkTemplate" track configuration parameter in JBrowse is overridden by WebApollo's feature edge matcher and drag and drop functions. It is recommended to use menuTemplate instead.
