#!/usr/bin/env groovy
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import org.json.JSONObject
import static groovyx.net.http.ContentType.*

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_annotations.groovy <options>" +
        "Example: \n" +
//        "./migrate_annotations.groovy -sourceurl http://remoteserver:8080/apollo/ -destinationurl http://localhost:8080/apollo/ -name amel -username ndunn@me.com -password supersecret"
        "./migrate_annotations.groovy -sourceurl http://localhost:8080/apollo/ -destinationurl newserver -username ndunn@me.com -password demo -organism Honey1"

println "intro"
def cli = new CliBuilder(usage: 'migrate_annotations.groovy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of source WebApollo instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of destination WebApollo instance to which annotations are to be loaded', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
OptionAccessor options

println "clie done"

try {
    options = cli.parse(args)

    if (!(options?.sourceurl && options?.destinationurl && options?.organism && options?.username && options?.password)) {
        return
    }
} catch (e) {
    println(e)
    return
}

def getFeaturesClient = new RESTClient(options.sourceurl)
def getFeaturesResponse = getFeaturesClient.post(
        contentType: 'text/javascript',
        path: '/apollo/annotationEditor/getFeatures',
        body: [operation: 'get_features', 'username': options.username, 'password': options.password, 'track': "Annotations-Group1.1", 'organism': options.name]
)

println "GetFeaturesResponse status: ${getFeaturesResponse.status}"
println "GetFeaturesResponse content: ${getFeaturesResponse.getData()}"

