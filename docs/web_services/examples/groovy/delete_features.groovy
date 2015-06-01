#!/usr/bin/env groovy

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

import groovyx.net.http.RESTClient
//import org.json.JSONObject


String usageString = "delete_features.groovy <options>" +
        "Example: " +
        "./delete_features.groovy -url http://localhost:8080/apollo/ -username admin@bio.gov -password supersecret -names NRAP,TCF7L2 -organism Human"

println "starting "

def cli = new CliBuilder(usage: 'delete_features.groovy <options>')
cli.setStopAtNonOption(true)
cli.url('URL to WebApollo instance', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
cli.names('feature names separated by a comma', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
OptionAccessor options
try {
    options = cli.parse(args)

    if (!(options?.url && options?.username && options?.password && options?.names && options?.organism)) {
        println "options missing ${usageString}"
        return
    }

} catch (e) {
    println(e)
    return
}

URL url = new URL(options.url)

def jsonFeatures = []
for(name in options.names.split(",")){
    def jsonObject = [ "name": name]
    jsonFeatures.add(jsonObject)
}

def argumentsArray = [
        username  : options.username,
        password  : options.password,
        features: jsonFeatures,
        organism  : options.organism
]


def client = new RESTClient(options.url)

String fullPath = "${url.path}/annotationEditor/deleteFeature"

def resp = client.post(
        contentType: 'text/javascript',
        path: fullPath,
        body: argumentsArray
)

assert resp.status == 200  // HTTP response code; 404 means not found, etc.
println resp.getData()


