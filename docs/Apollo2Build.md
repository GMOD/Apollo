## Web Apollo 2.0 build instructions

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/index.md">On GitHub</a>

## Install Grails:
1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.4

## Get The code
- git clone https://github.com/GMOD/Apollo.git grails-apollo
- cd grails-apollo
- git checkout grails1


## Basic configuration

Web Apollo 2.0 simplifies and expands options for the database setup. This section will cover the basic options for this.

## Database options



#### Configure for H2:
- copy sample-h2-apollo-config.groovy to apollo-config.groovy and update the data directory

#### Configure for PostgreSQL:
- Create a new database (e.g. default is just named `apollo`)
- Copy sample-postgres-apollo-config.groovy to apollo-config.groovy and re-run apollo deploy
- Note: There is no need to run the old add-tracks/add-users/set-track-permissions pipeline now.

#### Configure for MySQL:
- Create a new database (e.g. apollo by default)
- Copy sample-mysql-apollo-config.groovy to apollo-config.groovy and re-run apollo deploy


## Deploy the code as a war file
- ./apollo deploy
- copy the war file at target/apollo-X.Y.war to your webapps folder as apollo.war or similar


### Login to the web interface

After deployment, you can navigate to your server at http://localhost:8080/apollo/ or similar path and you will be
prompted for login information

![Login first time](images/1.png)

Figure 1. Register First User screen allows you to create a new admin user interface


![Organism configuration](images/2.png)

Figure 2. Navigate to the "Organism tab" and select "Create new organism". Then enter the new information for your
organism. Importantly, the data directory refers to a directory that has been prepared with the JBrowse data loading
scripts from the command line. See the [data loading] (Data_loading.md) section for details

![Open annotator](images/3.png)

Figure 3. Open up the new organism from the drop down tab on the annotator panel.
