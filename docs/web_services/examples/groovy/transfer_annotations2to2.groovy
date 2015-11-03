#!/usr/bin/env groovy
import groovyx.net.http.RESTClient
import net.sf.json.JSONArray
import net.sf.json.JSONObject

/**
 *
 */

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

String usageString = "transfer_annotations2to2.groovy <options>" +
        "Example: \n" +
        "./transfer_annotations2to2.groovy -username ndunn@me.com -password demo -sourceurl http://localhost:8080/apollo -source_organism amel -destinationurl http://localhost:8080/apollo2 -destination_organism amel2 -sequence_names Group1.1,Group1.10,Group1.2 "

def cli = new CliBuilder(usage: 'transfer_annotations.groovyy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of source Apollo instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of destination Apollo instance to which annotations are to be loaded', required: true, args: 1)
cli.source_organism('source organism common name', required: true, args: 1)
cli.destination_organism('destination organism common name', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
cli.sequence_names('sequence_names', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options

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

uniqueNamesMap = [:]
featuresMap = [:]
JSONObject newArray = new JSONObject()
ArrayList sequenceArray = new JSONArray()
JSONArray addFeaturesArray = new JSONArray()
JSONArray addTranscriptArray = new JSONArray()

if (!options.sequence_names) {
    // fetching sequences that are accessible by user
    sequenceArray = getAllSequencesForUsername(options.sourceurl, options.username, options.password, options.source_organism, options.ignoressl)
    if (sequenceArray == null) {
        println "Error: Could not fetch sequences for organism ${options.source_organism}."
        return
    }
}
else {
    sequenceArray = options.sequence_names.tokenize(',')
}

// For each sequence, fetching annotations from sourceurl
for (String sequence in sequenceArray) {
    String sequenceName = sequence
    URL url = new URL(options.sourceurl)
    String fullPath = "${url.path}/annotationEditor/getFeatures"
    def getFeaturesClient = new RESTClient(options.sourceurl)
    if (options.ignoressl) { getFeaturesClient.ignoreSSLIssues() }
    def getFeaturesResponse = getFeaturesClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: ['username': options.username, 'password': options.password, 'track': sequenceName, 'organism': options.source_organism]
    )

    if (getFeaturesResponse.status != 200) {
        println "Error: Source ${options.sourceurl} responded with ${getFeaturesResponse.status} status"
        return
    }
    //println getFeaturesResponse.getData().toString()
    if (getFeaturesResponse.getData().features == null) {
        println "Sequence ${sequenceName} does not exist for ${options.source_organism} at source URL ${options.sourceurl}"
        continue
    }
    else if (getFeaturesResponse.getData().features.size() == 0) {
        println "Request to fetch features from Sequence ${sequenceName} for ${options.source_organism} at source URL ${options.sourceurl} did not return any features"
        featuresMap.put(sequenceName, 0)
        continue
    }

    def featuresFromSource  = getFeaturesResponse.getData().features // contains list of mRNAs; Size == number of annotations on chromosome
    
    for (def entity : featuresFromSource) {
        JSONObject entityJSONObject = entity as JSONObject
        newArray.location = entityJSONObject.location
        newArray.type = entityJSONObject.type
        newArray.name = entityJSONObject.name
        //tmp.name = entityJSONObject.name.tokenize('-')[0]
        newArray.children = assignNewUniqueName(entityJSONObject.children)
        if (entityJSONObject.type.name == 'repeat_region' || entityJSONObject.type.name == 'transposable_element') {
            addFeaturesArray.add(0, newArray)
        }
        else {
            addTranscriptArray.add(0, newArray)
        }
    }
    
    if (addFeaturesArray.size() > 0) {
        //println "ADDFEATURESARRAY: ${addFeaturesArray.toString()}"
        def response = triggerAddFeature(options.destinationurl, options.username, options.password, options.destination_organism, sequenceName, addFeaturesArray, options.ignoressl)
        if (response == null) { return }
        println "addFeature response size: ${response.size()}"
    }
    if (addTranscriptArray.size() > 0) {
        //println "ADDTRANSCRIPTARRAY: ${addTranscriptArray.toString()}"
        def response = triggerAddTranscript(options.destinationurl, options.username, options.password, options.destination_organism, sequenceName, addTranscriptArray, options.ignoressl)
        if (response == null) { return }
        println "addTranscript response size: ${response.size()}"
    }

    featuresMap.put(sequenceName, (addFeaturesArray.size() + addTranscriptArray.size()))
}

println "\n::: STATISTICS :::\nSource URL :${options.sourceurl}\nSource Organism: ${options.source_organism}"
println "Destination URL :${options.destinationurl}\nDestination Organism: ${options.destination_organism}"


if (featuresMap.size() > 0) {
    println "\nFeatures Exported from each sequence:"
    featuresMap.each{ println "${it.key} : ${it.value}" }
}

if (uniqueNamesMap.size() > 0) {
    println "\nold uniquename : new uniquename"
    uniqueNamesMap.each{ println "${it.key} : ${it.value}" }
}


String generateUniqueName() {
    return UUID.randomUUID().toString()

}

JSONArray assignNewUniqueName(JSONArray inputArray) {
    JSONArray returnArray = new JSONArray()
    String oldUniqueName, newUniqueName
    idx = 0
    for (def eachEntity : inputArray) {
        oldUniqueName = eachEntity.uniquename
        newUniqueName = generateUniqueName()
        eachEntity.uniquename = newUniqueName
        returnArray.add(idx, eachEntity)
        uniqueNamesMap.put(oldUniqueName, newUniqueName)
        idx += 1
    }
    return returnArray
}

JSONObject triggerAddFeature(String destinationurl, String username, String password, String organism, String sequenceName, JSONArray featuresArray, Boolean ignoressl = false) {
    URL url = new URL(destinationurl)
    String fullPath = "${url.path}/annotationEditor/addFeature"
    def addFeatureClient = new RESTClient(url)
    if (ignoressl) { addFeatureClient.ignoreSSLIssues() }
    def addFeatureResponse = addFeatureClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: [  'username' : username, 'password' : password, 'track' : sequenceName, 'organism' : organism, 'features' : featuresArray ]
    )

    assert addFeatureResponse.status == 200
    if (addFeatureResponse.getData().size() == 0) {
        println "Error: Server did not respond properly while trying to call /addFeature"
        return
    }
    else {
        return addFeatureResponse.getData()
        
    }
}

JSONObject triggerAddTranscript(String destinationurl, String username, String password, String organism, String sequenceName, JSONArray featuresArray, Boolean ignoressl = false) {
    URL url = new URL(destinationurl)
    String fullPath = "${url.path}/annotationEditor/addTranscript"
    def addTranscriptClient = new RESTClient(url)
    if (ignoressl) { addTranscriptClient.ignoreSSLIssues() }
    def addTranscriptResponse = addTranscriptClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: [  'username' : username, 'password' : password, 'track' : sequenceName, 'organism' : organism, 'features' : featuresArray ]
    )

    assert addTranscriptResponse.status == 200
    if (addTranscriptResponse.getData().size() == 0) {
        println "Error: Server did not respond properly while trying to call /addTranscript"
        return
    }
    else {
        return addTranscriptResponse.getData()
    }
}

ArrayList getAllSequencesForUsername(String sourceurl, String username, String password, String organism, Boolean ignoressl = false) {
    URL url = new URL(sourceurl)
    String fullPath = "${url.path}/organism/getSequencesForOrganism"
    def argumentsArray = [
            username : username,
            password : password,
            organism : organism
    ]
    def getSequencesForOrganismClient = new RESTClient(sourceurl)
    if (ignoressl) { getSequencesForOrganismClient.ignoreSSLIssues() }
    def getSequencesForOrganismResponse = getSequencesForOrganismClient.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: argumentsArray
    )
    
    assert getSequencesForOrganismResponse.status == 200
    if (getSequencesForOrganismResponse.getData().sequences) {
        return getSequencesForOrganismResponse.getData().sequences as ArrayList
    } else {
        println getSequencesForOrganismResponse.getData().error
        return
    }
}