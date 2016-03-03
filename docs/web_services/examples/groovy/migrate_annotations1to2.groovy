#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
evaluate(new File("${scriptDir}/Apollo1Operations.groovy"))
evaluate(new File("${scriptDir}/Apollo2Operations.groovy"))


import net.sf.json.JSONArray
import net.sf.json.JSONObject


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_annotations1to2.groovy <options>" +
        "Example: \n" +
        "./migrate_annotations1to2.groovy -username1 demo -password1 demo -username2 ndunn@me.com -password2 demo  -sourceurl http://localhost:8080/Apollo1Instance/ -organism Honey2 -destinationurl http://localhost:8080/Apollo2/ -sequence_names Group1.26,Group1.3"

def cli = new CliBuilder(usage: 'migrate_annotations.groovy <options>')
cli.setStopAtNonOption(true)
cli.sourceurl('URL of Apollo 1.0.x instance from which annotations are fetched', required: true, args: 1)
cli.destinationurl('URL of Apollo 2.0.x instance to which annotations are to be loaded', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
cli.username1('username1', required: true, args: 1)
cli.password1('password1', required: true, args: 1)
cli.username2('username2', required: true, args: 1)
cli.password2('password2', required: true, args: 1)
cli.sequence_names('sequence_names', required: true, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
cli.ignore_prefix('Use this flag to NOT add the "Annotations-" prefix when pulling in Sequences', required: false)
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

String cookieFile = "${options.username2}_cookies.txt"

def responseArray = Apollo1Operations.doLogin(options.sourceurl, options.username1, options.password1,cookieFile)
if (responseArray == null) {
    println "Could not communicate with ${options.sourceurl}"
    return
}

uniqueNamesMap = [:]
featuresMap = [:]

JSONObject newArray = new JSONObject()
JSONArray addFeaturesArray = new JSONArray()
JSONArray addTranscriptArray = new JSONArray()


sequenceArray = options.sequence_names.tokenize(',')
for (String sequence in sequenceArray) {
    String sequenceName = sequence
    def featuresResponse = Apollo1Operations.getFeature(options.sourceurl,sequenceName,cookieFile,options.ignore_prefix)
    def featuresFromSource  = featuresResponse.features // contains list of mRNAs; Size == number of annotations on chromosome

    for (def entity : featuresFromSource) {
        JSONObject entityJSONObject = entity as JSONObject
        newArray.location = entityJSONObject.location
        newArray.type = entityJSONObject.type
        newArray.name = entityJSONObject.name
        //tmp.name = entityJSONObject.name.tokenize('-')[0]
        newArray.children = Apollo2Operations.assignNewUniqueName(entityJSONObject.children,uniqueNamesMap)
        if (entityJSONObject.type.name == 'repeat_region' || entityJSONObject.type.name == 'transposable_element') {
            addFeaturesArray.add(0, newArray)
        }
        else {
            addTranscriptArray.add(0, newArray)
        }
    }

    if (addFeaturesArray.size() > 0) {
        def response = Apollo2Operations.triggerAddFeature(options.destinationurl, options.username2, options.password2, options.organism, sequenceName, addFeaturesArray, options.ignoressl)
        if (response == null) { return }
        println "Migrate ${response.size()} features for ${sequence}"
    }
    if (addTranscriptArray.size() > 0) {
        //println "ADDTRANSCRIPTARRAY: ${addTranscriptArray.toString()}"
        def response = Apollo2Operations.triggerAddTranscript(options.destinationurl, options.username2, options.password2, options.organism, sequenceName, addTranscriptArray, options.ignoressl)
        if (response == null) { return }
        println "Migrate ${response.size()} transcripts for ${sequence}"
    }

    // keep stats
    featuresMap.put(sequenceName, (addFeaturesArray.size() + addTranscriptArray.size()))
    addFeaturesArray.clear()
    addTranscriptArray.clear()
}

for(f in featuresMap){
    println f.value + " found in " + f.key
}



