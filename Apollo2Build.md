git clone https://github.com/GMOD/Apollo.git grails-apollo
cd grails-apollo
git checkout grails1
git status   # should be grails1
ant debug  # just once
./copy_client.sh # every time JS code in client changes 



It runs with H2, postgreSQL, etc.

Configure for H2:
- copy sample-h2-apollo-config.groovy to apollo-config.groovy and update the data directory

Configure for Apollo:
- Create a new database (I chose apollo by default)
- There are no user-level anything yet, so no need to add tracks, users, etc.  those will come later.
- copy sample-postgres-apollo-config.groovy to apollo-config.groovy and update


To run in dev-mode:
1 - curl -s get.gvmtool.net | bash
2 - gvm install grails 2.4.4
3 - open two terminals A and B
4 - terminal A (grails run-app)
5 - terminal B (ant devmode or ant gwtc if not doing any GWT development)
6 - setup jbrowse data here with user read permissions:  /opt/apollo/jbrowse/data


To run in production:
1 - curl -s get.gvmtool.net | bash
2 - gvm install grails 2.4.4
3 - ant gwtc
4 - grails war
5 - Copy target/apollo-2.0-SNAPSHOT.war to your tomcat webapps directory
6 - setup jbrowse data here with user read permissions:  /opt/apollo/jbrowse/data


Note: We will hopefully be wrapping this via the "apollo" binary at some point, but one thing at a time.


Architecture notes:
- Grails code is in normal grails directories under "grails-app"
- GWT-only code is under src/gwt except
    - Code shared between the client and the server is under src/gwt/org/bbop/apollo/gwt/shared
- Client code is under client (still)
- Tests are under "Test"
- Old (presumably inactive code) is under src/main/webapp
- New source (loaded into VM) is under src/java or src/groovy except for grails specific code.
- Web code (not much) is either under web-app (and where jbrowse is copied) or under grails-app/assets (these are compiled down).
- GWT-specifc CSS can also be found in: src/gwt/org/bbop/apollo/gwt/client/resources/ , but it inherits the CSS on its current page, as well.


