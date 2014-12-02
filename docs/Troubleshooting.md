
### Tomcat memory

<a href="https://github.com/GMOD/Apollo/blob/master/docs/Troubleshooting.md">On GitHub</a>


Many times the default memory allowance is too low.
The memory requirements of WebApollo will depend on the the size of your genome and
how many instances of Web Apollo you host in the same Tomcat instance, but in general,
we recommend at least 1g for the heap size and 256m for the permgen size
as a starting point. Suggested settings are:

    export CATALINA_OPTS="-Xms512m -Xmx1g -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled -XX:+UseConcMarkSweepGC -XX:MaxPermSize=256m"

To use this setting, edit the setenv.sh script in 
`$TOMCAT_BIN_DIR/setenv.sh` where `$TOMCAT_BIN_DIR` is where the
directory where the Tomcat binaries reside.

### No refseqs when opening up selectTrack.jsp


This problem often indicates that credentials for the LocalDbUserAuthentication script were not initialized properly because only tracks that the user has permissions for will be shown. Please refer to [the quick install guide](Quick_install_guide.md) for details on these steps, paying attention particularly to the set_user_track_permissions.pl script which sets the permissions for which which refseqs a user can access.

### Getting logged out when entering JBrowse

This often indicates that the add-webapollo-plugin.pl script wasn't run properly, which will update JBrowse's configuration and load the Web Apollo plugin. See [the install guide](Build.md) for details on this step.


### No error message from failed Web Apollo login

Web Apollo uses a custom error reporting valve. To setup, add `errorReportValveClass="org.bbop.apollo.web.ErrorReportValve"` as an attribute to the existing <Host> element in tomcat's server.xml (e.g. /var/lib/tomcat7/conf/server.xml)

    <Host name="localhost" appBase="webapps" 
      unpackWARs="true" autoDeploy="true" 
      errorReportValveClass="org.bbop.apollo.web.ErrorReportValve">
    </Host>


### Errors running JBrowse scripts

##### e.g. "Can't locate Hash/Merge.pm in @INC" or "Can't locate JBlibs.pm in @INC"

If are trying to run the jbrowse binaries but get these sorts of errors, try `install_jbrowse.sh` which will initialize as many pre-requisites as possible including JBLibs and other JBrowse dependencies. 

### Errors during apollo deploy or install_jbrowse.sh

##### e.g. "cd: src/main/webapp/jbrowse: No such file or directory"

If you get this error, then you may need to re-run `apollo deploy` or even do a `apollo clean-all && apollo deploy`. You may also want to review the [developers guide](Developer.md) for how to create a precompiled package.


