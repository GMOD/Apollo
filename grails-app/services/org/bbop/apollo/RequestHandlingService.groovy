package org.bbop.apollo

import grails.converters.JSON

//import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

/**
 * This class is responsible for handling JSON requests from the AnnotationEditorController and routing
 * to the proper service classes.
 *
 * Its goal is to replace a a lot of the layers in AnnotationEditorController
 */
//@GrailsCompileStatic
@Transactional
class RequestHandlingService implements  AnnotationListener{

    def featureService
    def transcriptService
    def nameService

    // TODO: make a grails singleton
    DataListenerHandler dataListenerHandler = DataListenerHandler.getInstance()

    public RequestHandlingService(){
        dataListenerHandler.addDataStoreChangeListener(this);
    }

    private String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }

    @MessageMapping("/AnnotationNotification")
    @SendTo("/topic/AnnotationNotification")
    protected String annotationEditor(String inputString) {
        println "Input String:  annotation editor service ${inputString}"
        JSONObject rootElement = (JSONObject) JSON.parse(inputString)

        println "root element: ${rootElement}"
        String track = ((JSONObject) rootElement).get(AnnotationEditorController.REST_TRACK)
        String operation = ((JSONObject) rootElement).get(AnnotationEditorController.REST_OPERATION)
        def params = []
//        for(String key in rootElement.keySet()) {
//            if(key!=REST_TRACK && key!=REST_OPERATION){
//                params[key] = rootElement.get(key)
//            }
//        }

        String operationName = underscoreToCamelCase(operation)
//        handleOperation(track,operation)
        def p = task {
            switch (operationName) {
                case "addTranscript": addTranscript(rootElement)
                    break
                default: nameService.generateUniqueName()
                    break
            }
        }
        def results = p.get()
        println "completling result ${results}"
//        return "returning annotationEditor ${inputString}!"
        return results

//        p.onComplete([p]){ List results ->
//            println "completling result ${results}"
//            return "returning annotationEditor ${inputString}!"
//        }
//        p.onError([p]){ List results ->
//            println "error ${results}"
//            return "ERROR returning annotationEditor ${inputString}!"
//        }

    }

    JSONObject getFeatures(JSONObject returnObject){
//        String trackName = returnObject.get(REST_TRACK)
        String trackName = fixTrackHeader(returnObject.track)
        println "sequenceName: ${trackName}"
        println "Sequence count:${Sequence.count}"
//        println "sequecne all ${Sequence.all.get(0).name}"
        Sequence sequence = Sequence.findByName(trackName)
        println "sequence found for name ${sequence}"
//        Set<FeatureLocation> featureLocations = sequence.featureLocations

        Set<Feature> featureSet = new HashSet<>()

//        println "# of features locations for sequence ${sequence?.featureLocations?.size()}"
//        println "# of features for sequence ${sequence?.featureLocations*.feature?.size()}"

//        List<Feature> topLevelFeatures = sequence?.featureLocations*.feature

        /**
         * TODO: this should be one single query
         */
        // 1. - handle genes
        List<Gene> topLevelGenes = Gene.executeQuery("select f from Gene f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        for (Gene gene : topLevelGenes) {
            for (Transcript transcript : transcriptService.getTranscripts(gene)) {
                println " getting transcript ${transcript.uniqueName} for gene ${gene.uniqueName} "
//                    jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
                featureSet.add(transcript)
//                    jsonFeatures.put(transcript as JSON);
            }
        }

        // 1b. - handle psuedogenes
        List<Pseudogene> listOfPseudogenes = Pseudogene.executeQuery("select f from Pseudogene f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        for (Gene gene : listOfPseudogenes) {
            for (Transcript transcript : transcriptService.getTranscripts(gene)) {
                println " getting transcript ${transcript.uniqueName} for gene ${gene.uniqueName} "
//                    jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
                featureSet.add(transcript)
//                    jsonFeatures.put(transcript as JSON);
            }
        }

        // 2. - handle transcripts
        List<Transcript> topLevelTranscripts = Transcript.executeQuery("select f from Transcript f join f.featureLocations fl where fl.sequence = :sequence and f.childFeatureRelationships is empty ", [sequence: sequence])
        println "# of top level features ${topLevelTranscripts.size()}"
        for (Transcript transcript1 in topLevelTranscripts) {
            featureSet.add(transcript1)
        }

        println "feature set size: ${featureSet.size()}"

        JSONArray jsonFeatures = new JSONArray()
        featureSet.each { feature ->
            JSONObject jsonObject = featureService.convertFeatureToJSON(feature, false)
            jsonFeatures.put(jsonObject)
        }

        returnObject.put(AnnotationEditorController.REST_FEATURES, jsonFeatures)


        println "final return objecct ${returnObject}"

//        if (fireUpdateChange) {
//            fireDataStoreChange(featureContainer, track, DataStoreChangeEvent.Operation.ADD);
//        }

        fireAnnotationEvent(new AnnotationEvent(
                features: returnObject
                , operation: AnnotationEvent.Operation.ADD
                , sequence: sequence
        ))


        return returnObject

    }

    JSONObject addTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        JSONObject returnObject = createJSONFeatureContainer()

        println "RHS::adding transcript return object ${inputObject?.size()}"
        String trackName = fixTrackHeader(inputObject.track)
        println "RHS::PRE featuresArray ${featuresArray?.size()}"
        if (featuresArray.size() == 1) {
            JSONObject object = featuresArray.getJSONObject(0)
            println "object ${object}"
        } else {
            println "what is going on?"
        }
        println "POST featuresArray ${featuresArray}"
        Sequence sequence = Sequence.findByName(trackName)
        println "trackName ${trackName}"
        println "sequence ${sequence}"
        println "features Array size ${featuresArray.size()}"
        println "features Array ${featuresArray}"

        List<Transcript> transcriptList = new ArrayList<>()
        for (int i = 0; i < featuresArray.size(); i++) {
            JSONObject jsonTranscript = featuresArray.getJSONObject(i)
            println "${i} jsonTranscript ${jsonTranscript}"
            println "featureService ${featureService} ${trackName}"
            Transcript transcript = featureService.generateTranscript(jsonTranscript, trackName)

            // should automatically write to history
            transcript.save(insert: true, flush: true)
//            sequence.addFeatureLotranscript)
            transcriptList.add(transcript)


        }

        sequence.save(flush: true)
        // do I need to put it back in?
//        returnObject.putJSONArray("features",featuresArray)
        transcriptList.each { transcript ->
            returnObject.getJSONArray(FeatureStringEnum.FEATURES.value).put(featureService.convertFeatureToJSON(transcript, false));
//            featuresArray.put(featureService.convertFeatureToJSON(transcript,false))
        }

        println "return addTranscript featuer ${returnObject}"
//        println "VS - ${featuresArray}"

        AnnotationEvent annotationEvent = new AnnotationEvent(
                features: returnObject
                , sequence: sequence
                , operation: AnnotationEvent.Operation.ADD
        )

        fireAnnotationEvent(annotationEvent)

        return returnObject

    }

    def fireAnnotationEvent(AnnotationEvent annotationEvent) {
        dataListenerHandler.fireDataStoreChange(annotationEvent)
    }

    @SendTo("/topic/AnnotationNotification")
    protected static String sendAnnotationEvent(String returnString){
        println "RHS::return operations sent . . ${returnString?.size()}"
        return returnString
    }

    synchronized void handleChangeEvent(AnnotationEvent... events) {
        println "handingling event ${events.length}"
        if (events.length == 0) {
            return;
        }
//        sendAnnotationEvent(events)
        // TODO: this is more than a bit of a hack
//        String sequenceName = "Annotations-${events[0].sequence.name}"
//        Queue<AsyncContext> contexts = queue.get(sequenceName);
//        Queue<AsyncContext> contexts = queue.get(events[0].getSequence());
//        if (contexts == null) {
//            return;
//        }
        JSONArray operations = new JSONArray();
        for (AnnotationEvent event : events) {
            JSONObject features = event.getFeatures();
            try {
                features.put("operation", event.getOperation().name());
                features.put("sequenceAlterationEvent", event.isSequenceAlterationEvent());
                operations.put(features);
            }
            catch (JSONException e) {
                log.error("error handling change event ${event}: ${e}")
            }
        }

        sendAnnotationEvent(operations.toString())
//        ??
//        for (AsyncContext asyncContext : contexts) {
//            ServletResponse response = asyncContext.getResponse();
//            try {
//                response.getWriter().write(operations.toString());
//                response.flushBuffer();
//            }
//            catch (IOException e) {
//                log.error(e)
//            }
////            asyncContext.complete();
//        }

    }

    private static JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    private static String fixTrackHeader(String trackInput) {
        return !trackInput.startsWith("Annotations-") ? trackInput : trackInput.substring("Annotations-".size())
    }
}
