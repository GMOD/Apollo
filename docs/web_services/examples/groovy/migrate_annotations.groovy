#!/usr/bin/env groovy

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import net.sf.json.JSONArray
import net.sf.json.JSONObject


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_annotations.groovy <options>" +
        "Example: \n" +
        "./migrate_annotations.groovy -sourceurl http://localhost:8080/apollo/ -source_organism amel -destinationurl http://localhost:8080/apollo2 -destination_organism amel2  -username ndunn@me.com -password demo "

println "intro"
def cli = new CliBuilder(usage: 'migrate_annotations.groovy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of source WebApollo instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of destination WebApollo instance to which annotations are to be loaded', required: true, args: 1)
cli.source_organism('source organism common name', required: true, args: 1)
cli.destination_organism('destination organism common name', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
OptionAccessor options

println "clie done"

try {
    options = cli.parse(args)

    if (!(options?.sourceurl && options?.destinationurl && options?.source_organism && options?.destination_organism && options?.username && options?.password)) {
        println "\n"+usageString
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
        body: ['username': options.username, 'password': options.password, 'track': "Annotations-Group1.1", 'organism': options.source_organism]
)

assert getFeaturesResponse.status == 200
println getFeaturesResponse.getData()
if (getFeaturesResponse.getData().features.size() == 0) {
    println "Couldn't establish session with ${options.sourceurl}"
    return
}

count = 0
JSONArray jsonArray = new JSONArray()
for (def entity : getFeaturesResponse.getData().features) {
    jsonArray.add(count, entity as JSONObject)
    count += 1
}

println "JSONARRAY SIZE: ${jsonArray.size()}"

def loadFeaturesClient = new RESTClient(options.destinationurl)
def loadFeaturesResponse = loadFeaturesClient.post(
        contentType: 'text/javascript',
        path: '/apollo/annotationEditor/addTranscript',
        body: [  'username' : options.username, 'password' : options.password, 'track' : "Annotations-Group1.1", 'organism' : options.destination_organism, 'features' : jsonArray ]
)

assert loadFeaturesResponse.status == 200
println "loadFeaturesResponse content: ${loadFeaturesResponse.getData()}"


