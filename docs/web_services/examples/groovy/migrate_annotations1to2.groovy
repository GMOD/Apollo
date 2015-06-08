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

String usageString = "migrate_annotations1to2.groovy <options>" +
        "Example: \n" +
        "./migrate_annotations1to2.groovyy -username1 ndunn@me.com -password1 demo -username2 demo -password2 demo -sourceurl http://localhost:8080/apollo -organism amel -destinationurl http://localhost:8080/apollo2 -sequence_names Group1.1,Group1.10,Group1.2"

def cli = new CliBuilder(usage: 'migrate_annotations.groovyy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of WebApollo 1.0.x instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of WebApollo 2.0.x instance to which annotations are to be loaded', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
cli.username1('username1', required: true, args: 1)
cli.password1('password1', required: true, args: 1)
cli.username2('username2', required: true, args: 1)
cli.password2('password2', required: true, args: 1)
cli.sequence_names('sequence_names', required: false, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.sourceurl && options?.destinationurl && options?.organism && options?.username2 && options?.password2 && options?.username1 && options?.password1 && options?.sequence_names)) {
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
        username: options.username2,
        password: options.password2
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
def responseArray = doLogin(options.sourceurl, options.username1, options.password1)
println "response array: ${responseArray}"
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
String cookieFile = "${options.username1}_cookies.txt"
for (String sequence in sequenceArray) {
    String sequenceName = sequencePrefix + sequence
    String fullPath = "${url.path}/AnnotationEditorService"
    def getFeaturesClient = new RESTClient(options.sourceurl)
    def body = ['username': options.username1, 'password': options.password1, 'track': sequenceName, 'operation': 'get_features' ]
    println "repsonseArray ${responseArray}"
    body << responseArray
//    body.put(responseArray.key,responseArray.value)
    println "body: "+body
    def getfeaturesResponse = getFeaturesClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: body
    )
    println getfeaturesResponse.getData()
}

def doLogin(url, username, password) {
    def jsonSlurper = new JsonSlurper()
    def cmd = "../shell/doLogin.sh ${url} ${username} ${password}"
//    def cmd = "http://localhost:8080/apollo/"
//    String cookieFile = "${username}_cookies.txt"
//    def cmd = "/bin/bash curl -c ${cookieFile} -H 'Content-Type:application/json' -d \"{'username': '${username}', 'password': '${password}'}\" \"${url}/Login?operation=login\" 2> /dev/null"
    println "cmd object[${cmd}]"
    def proc = cmd.execute()
    proc.waitFor()
    
    if (proc.exitValue() != 0) {
        println "Error while login to ${url} with username ${username}"
        return
    }
    def responseArray = jsonSlurper.parseText(proc.getText())
    return responseArray
}