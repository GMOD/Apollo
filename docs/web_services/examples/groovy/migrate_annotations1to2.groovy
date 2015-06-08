#!/usr/bin/env groovy
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient

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
def jsonSlurper = new JsonSlurper()
String cookieFile = "${options.username2}_cookies.txt"

//def argumentsArray = [
//        operation: 'login',
//        username: options.username2,
//        password: options.password2
//]

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

def responseArray = doLogin(options.sourceurl, options.username1, options.password1,cookieFile)
println "response array: ${responseArray}"
if (responseArray == null) {
    println "Could not communicate with ${options.sourceurl}"
    return
}
//if (responseArray.error) {
//    println "Error: ${responseArray.error}"
//    return
//}

final String sequencePrefix = "Annotations-"
uniqueNamesMap = [:]
featuresMap = [:]


sequenceArray = options.sequence_names.tokenize(',')
for (String sequence in sequenceArray) {
    String sequenceName = sequencePrefix + sequence
    def featuresResponse = getFeature(options.sourceurl,sequenceName,cookieFile)
    featuresMap.put(sequence,featuresResponse)
//    println featuresResponse
}

def getFeature(url,track,cookieFile){

//    curl -b demo_cookies.txt -c demo_cookies.txt -e "http://icebox.lbl.gov/WebApolloDemo/" --data "{ 'operation': 'get_features', 'track': 'Annotations-Group1.10'}" http://icebox.lbl.gov/WebApolloDemo/AnnotationEditorService
    String json = "{ 'operation': 'get_features', 'track': '${track}'}"
    def process = ["curl","-b",cookieFile,"-c",cookieFile,"-e",url,"--data",json,"${url}/AnnotationEditorService"].execute()
    def response = process.text
    if(process.exitValue()!=0){
        println process.errorStream.text
    }
    def jsonResponse = new JsonSlurper().parseText(response)
    return jsonResponse

}

def doLogin(url, username, password,cookieFile) {
    String json = "{'username': '${username}', 'password': '${password}'}"
    def process = ["curl","-c",cookieFile,"-H","Content-Type:application/json","-d",json,"${url}/Login?operation=login"].execute()
    def response = process.text
    if(process.exitValue()!=0){
        println process.errorStream.text
    }
    def jsonResponse = new JsonSlurper().parseText(response)
    return jsonResponse
}