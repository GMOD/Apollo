#!/usr/bin/env groovy
import groovy.json.JsonBuilder
import groovy.json.internal.LazyMap
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import net.sf.json.JSONArray
import net.sf.json.JSONObject


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_annotations.groovyy <options>" +
        "Example: \n" +
        "./migrate_annotations.groovyy -username ndunn@me.com -password demo -sourceurl http://localhost:8080/apollo -source_organism amel -destinationurl http://localhost:8080/apollo2 -destination_organism amel2 -sequence_names Group1.1,Group1.10,Group1.2 "

def cli = new CliBuilder(usage: 'migrate_annotations.groovyy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of WebApollo 1.0.x instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of WebApollo 2.0.x instance to which annotations are to be loaded', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
cli.sequence_names('sequence_names', required: false, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.sourceurl && options?.destinationurl && options?.organism && options?.username && options?.password && options?.sequence_names)) {
        println "\n"+usageString
        return
    }
} catch (e) {
    println(e)
    return
}

//login first
URL url = new URL(options.sourceurl)
String loginPath = "${url.path}/Login?operation=login"
println loginPath

def loginClient = new RESTClient(options.sourceurl)

def argumentsArray = [
        username: options.username,
        password : options.password,
        operation : 'login' 
]

def loginResponse = loginClient.get(
        contentType : 'text/javascript',
        path : loginPath,
        body : argumentsArray
)

println loginResponse.status
println loginResponse.getData()


//final String sequencePrefix = "Annotations-"
//uniqueNamesMap = [:]
//featuresMap = [:]
//
//sequenceArray = options.sequence_names.tokenize(',')
//
//for (String sequence in sequenceArray) {
//    String sequenceName = sequencePrefix + sequence
//    URL url = new URL(options.sourceurl)
//    String fullPath = "${url.path}/AnnotationEditorService"
//    def getFeaturesClient = new RESTClient(options.sourceurl)
//    def getfeaturesResponse = getFeaturesClient.post(
//            contentType: 'text/javascript',
//            path: fullPath,
//            body: ['username': options.username, 'password': options.password, 'track': sequenceName, 'operation': 'get_features' ]
//    )
//
//    println getfeaturesResponse.getData()
//
//}