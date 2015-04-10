
## Install Grails:
1. curl -s get.gvmtool.net | bash
2. gvm install grails 2.4.4

## Get The code
- git clone https://github.com/GMOD/Apollo.git grails-apollo
- cd grails-apollo
- git checkout grails1

## Deploy the code as a war file
- ./apollo deploy
- war file is created in target/apollo-X.Y.war
- deploy to your tomcat system (typically in the webapps tomcat directory)


## Run the code
It runs with H2, postgreSQL, etc.

#### Configure for H2:
- copy sample-h2-apollo-config.groovy to apollo-config.groovy and update the data directory

#### Configure for Apollo:
- Create a new database (I chose apollo by default)
- There are no user-level anything yet, so no need to add tracks, users, etc.  those will come later.
- copy sample-postgres-apollo-config.groovy to apollo-config.groovy and update



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


## Testing Notes:
The unit testing framework is defined in better detail than I will describe here: http://grails.github.io/grails-doc/2.4.3/guide/testing.html
Spock (what grails uses) is defined here: https://code.google.com/p/spock/wiki/SpockBasics


My basic methodology is to enter the prompt by typing “grails” and then type

    test-app :unit-test

This runs ALL of the tests in “test/unit” If you want to test a specific function then write it something like this:

    test-app org.bbop.apollo.FeatureService :unit 

This runs the tests in FeatureServiceSpec . . some of which is below.  Some important points:

1. @Mock includes any domain objects you’ll use.  Unit tests don’t use the database.
2. setup() is run for each test I believe 
3. when: “” then: “”   You have to have both or it is not a test. 
4. Notice the valid groovy notation  .name == “Chr3”, it implies the Java .equals() function . . everywhere . . . . groovy rox

```
    @TestFor(FeatureService)
    @Mock([Sequence,FeatureLocation,Feature])
      class FeatureServiceSpec extends Specification {
      void setup(){}
      void "convert JSON to Feature Location"(){
    
      when: "We have a valid json object"
      JSONObject jsonObject = new JSONObject()
      Sequence sequence = new Sequence(name: "Chr3",seqChunkPrefix: "abc",seqChunkSize: 20,start:1,end:100,length:99,sequenceDirectory: "/tmp").save(failOnError: true)
      jsonObject.put(FeatureStringEnum.FMIN.value,73)
      jsonObject.put(FeatureStringEnum.FMAX.value,113)
      jsonObject.put(FeatureStringEnum.STRAND.value, Strand.POSITIVE.value)

    
      then: "We should return a valid FeatureLocation"
      FeatureLocation featureLocation = service.convertJSONToFeatureLocation(jsonObject,sequence)
      assert featureLocation.sequence.name == "Chr3"
      assert featureLocation.fmin == 73
      assert featureLocation.fmax == 113
      assert featureLocation.strand ==Strand.POSITIVE.value
    } }
```

There are 3 “special” types of things to test, which are all important and reflect the grails special functions: Domains, Controllers, Services.  They will all be in the “test” directory and all be suffixed with “Spec” for a Spock test.



