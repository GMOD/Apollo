
## Automated testing architecture

The Apollo unit testing framework uses the grails testing guidelines extensively, which can be reviewed here:
http://grails.github.io/grails-doc/2.4.3/guide/testing.html


Our basic methodology is to run the full test suite with the apollo command:

``` 
apollo test
```


More specific tests can also be run for example by running specific commands for `grails test-app`

``` 
grails test-app :unit-test
```

This runs ALL of the tests in “test/unit”. If you want to test a specific function then write it something like this:

``` 
grails test-app org.bbop.apollo.FeatureService :unit
```



### Notes about the test suites:

1. @Mock includes any domain objects you’ll use.  Unit tests don’t use the database.
2. The setup() function is run for each test
3. The test is composed of blocks of code with `when:` and `then:`. You have to have both or it is not a test. 


Example test:

``` 
@TestFor(FeatureService)
@Mock([Sequence,FeatureLocation,Feature])
  class FeatureServiceSpec extends Specification {
  void setup(){}
  void "convert JSON to Feature Location"(){

  when: "We have a valid json object"
  JSONObject jsonObject = new JSONObject()
  Sequence sequence = new Sequence(name: "Chr3",
    seqChunkSize: 20, start:1, end:100, length:99).save(failOnError: true)
  jsonObject.put(FeatureStringEnum.FMIN.value,73)
  jsonObject.put(FeatureStringEnum.FMAX.value,113)
  jsonObject.put(FeatureStringEnum.STRAND.value, Strand.POSITIVE.value)


  then: "We should return a valid FeatureLocation"
  FeatureLocation featureLocation = 
    service.convertJSONToFeatureLocation(jsonObject,sequence)
  assert featureLocation.sequence.name == "Chr3"
  assert featureLocation.fmin == 73
  assert featureLocation.fmax == 113
  assert featureLocation.strand ==Strand.POSITIVE.value
} }
```

There are 3 "special" types of things to test, which are all important and reflect the grails special functions:
Domains, Controllers, Services.  They will all be in the “test” directory and all be suffixed with “Spec” for a Spock
test.


### Chado

If you test with the [chado export](ChadoExport.md) you will need to make sure you load ontologies into your chado database or integration steps will fail.  If you don't specify chado in your apollo-config.groovy then no further action would be necessary.

    ./scripts/load_chado_schema.sh -u nathandunn -d apollo-chado-test -s chado-schema-with-ontologies.sql.gz -r

