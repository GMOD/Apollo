
### Tomcat memory

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


This often indicates that credentials for the LocalDbUserAuthentication script were not initialized properly. Please refer to (the install guide)[Install.md] for details on these steps, particularly during the set_user_track_permissions.pl script which sets the permissions for the user.

### Getting logged out when entering JBrowse

This often indicates that the add-webapollo-plugin.pl script wasn't run properly. See (the install guide)[Install.md] for details on this step.
