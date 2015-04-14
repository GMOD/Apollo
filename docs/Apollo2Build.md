
## Install Grails:
1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.4

## Get The code
- git clone https://github.com/GMOD/Apollo.git grails-apollo
- cd grails-apollo
- git checkout grails1


## Basic configuration
There are sample configurations that allow Apollo to run with H2, PostgreSQL, etc.

#### Configure for H2:
- copy sample-h2-apollo-config.groovy to apollo-config.groovy and update the data directory

#### Configure for PostgreSQL:
- Create a new database (I chose apollo by default)
- There are no user-level anything yet, so no need to add tracks, users, etc.  those will come later.
- copy sample-postgres-apollo-config.groovy to apollo-config.groovy and update


## Deploy the code as a war file
- ./apollo deploy
- war file is created in target/apollo-X.Y.war
- deploy to your tomcat system (typically in the webapps tomcat directory)


==========

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


