#!/usr/bin/env groovy
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import net.sf.json.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_annotations.groovyy <options>" +
        "Example: \n" +
        "./migrate_annotations.groovyy -username ndunn@me.com -password demo -sourceurl http://localhost:8080/apollo -organism amel -destinationurl http://localhost:8080/apollo2 -sequence_names Group1.1,Group1.10,Group1.2"

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

URL url = new URL(options.sourceurl)
String loginPath = "${url.path}/Login"

def argumentsArray = [
        operation: 'login',
        username: options.username,
        password: options.password
]

//login using RESTClient
//try {
//    def loginClient = new RESTClient(options.sourceurl)
//    def loginResponse = loginClient.post(
//            contentType: 'text/javascript',
//            path: loginPath,
//            body: argumentsArray
//    )
//    println loginResponse.status
//    println loginResponse.getData()
//
//} catch(HttpResponseException h) {
//    println h.response.getData()
//}

//do login using curl
def responseArray = doLogin(options.sourceurl, options.username, options.password)
if (responseArray == null) {
    println "Could not communicate with ${options.sourceurl}"
    return
}
if (responseArray.error) {
    println "Error: ${responseArray.error}"
    return
}

final String sequencePrefix = "Annotations-"
uniqueNamesMap = [:]
featuresMap = [:]

sequenceArray = options.sequence_names.tokenize(',')
for (String sequence in sequenceArray) {
    String sequenceName = sequencePrefix + sequence
    String fullPath = "${url.path}/AnnotationEditorService"
    def getFeaturesClient = new RESTClient(options.sourceurl)
    def getfeaturesResponse = getFeaturesClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: ['username': options.username, 'password': options.password, 'track': sequenceName, 'operation': 'get_features' ]
    )
    println getfeaturesResponse.getData()
}

def doLogin(url, username, password) {
    def jsonSlurper = new JsonSlurper()
    def cmd = "../shell/doLogin.sh ${url} ${username} ${password}"
    def proc = cmd.execute()
    proc.waitFor()
    
    if (proc.exitValue() != 0) {
        println "Error while login to ${options.url} with username ${options.username}"
        return
    }
    def responseArray = jsonSlurper.parseText(proc.getText())
    return responseArray
}