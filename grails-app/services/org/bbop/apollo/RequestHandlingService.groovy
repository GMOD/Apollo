package org.bbop.apollo

//import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional
import org.bbop.apollo.event.AnnotationEvent
import org.bbop.apollo.event.AnnotationListener
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
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

    // TODO: make a grails singleton
    DataListenerHandler dataListenerHandler = DataListenerHandler.getInstance()

    public RequestHandlingService(){
        dataListenerHandler.addDataStoreChangeListener(this);
    }


    JSONObject addTranscript(JSONObject inputObject) {
        JSONArray featuresArray = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)

        JSONObject returnObject = createJSONFeatureContainer()

        println "adding transcript return object ${inputObject}"
        String trackName = fixTrackHeader(inputObject.track)
        println "PRE featuresArray ${featuresArray}"
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
        println "return operations sent . . ${returnString}"
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
