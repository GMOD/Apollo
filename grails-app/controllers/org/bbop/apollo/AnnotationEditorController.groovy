package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.gmod.gbol.util.SequenceUtil
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo

/**
 * From the AnnotationEditorService
 */
class AnnotationEditorController {

    String REST_OPERATION = "operation"
    String REST_TRACK = "track"

    String REST_USERNAME = "username"
    String REST_PERMISSION = "permission"
    String REST_DATA_ADAPTER = "data_adapter"
    String REST_DATA_ADAPTERS = "data_adapters"
    String REST_KEY = "key"
    String REST_OPTIONS = "options"
    String REST_TRANSLATION_TABLE = "translation_table"

    def index() {
        log.debug  "bang "
    }

    private String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }

    private def findPost() {
        for (p in params) {
            String key = p.key
            if (key.contains("operation")) {
                return (JSONObject) JSON.parse(key)
            }
        }
    }

    def handleOperation(String track, String operation) {
        // TODO: this is a hack, but it should come trhough the UrlMapper
        JSONObject postObject = findPost()
        operation = postObject.get(REST_OPERATION)
        def mappedAction = underscoreToCamelCase(operation)
        log.debug  "${operation} -> ${mappedAction}"
        track = postObject.get(REST_TRACK)
        forward action: "${mappedAction}", params: [data: postObject]
    }


    def getUserPermission() {
        log.debug  "gettinguser permission !! ${params.data}"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        // TODO: wire into actual user table
        String username = session.getAttribute("username")
        log.debug  "user from ${username}"
        username = "demo"
        returnObject.put(REST_PERMISSION, 3)
        returnObject.put(REST_USERNAME, username)

        render returnObject
    }

    def getDataAdapters() {
        log.debug  "get data adapters !! ${params}"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        JSONArray dataAdaptersArray = new JSONArray();
        returnObject.put(REST_DATA_ADAPTER, dataAdaptersArray)
        for (DataAdapter dataAdapter in DataAdapter.all) {
            // data-adapters are embedded in groups
            // TODO: incorporate groups at some point, just children of the original . . .
            JSONObject dataAdapterJSON = new JSONObject()
            dataAdapterJSON.put(REST_KEY, dataAdapter.key)
            dataAdapterJSON.put(REST_PERMISSION, dataAdapter.permission)
            dataAdapterJSON.put(REST_OPTIONS, dataAdapter.options)
            JSONArray dataAdapterGroupArray = new JSONArray();
            if(dataAdapter.dataAdapters){
                dataAdapterJSON.put(REST_DATA_ADAPTERS, dataAdapterGroupArray)

                for(da in dataAdapter.dataAdapters){
                    JSONObject dataAdapterChild = new JSONObject()
                    dataAdapterChild.put(REST_KEY, da.key)
                    dataAdapterChild.put(REST_PERMISSION, da.permission)
                    dataAdapterChild.put(REST_OPTIONS, da.options)
                    dataAdapterGroupArray.put(dataAdapterChild)
                }
            }
        }

        render returnObject
    }

    def getTranslationTable() {
        log.debug  "get translation table!! ${params}"
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        SequenceUtil.TranslationTable translationTable = SequenceUtil.getDefaultTranslationTable()
        JSONObject ttable = new JSONObject();
        for (Map.Entry<String, String> t : translationTable.getTranslationTable().entrySet()) {
            ttable.put(t.getKey(), t.getValue());
        }
        returnObject.put(REST_TRANSLATION_TABLE,ttable);
        render returnObject
    }

    def getFeatures() {
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        JSONArray jsonFeatures = new JSONArray()
        returnObject.put("features",jsonFeatures)

        Sequence sequence = Sequence.findByName(returnObject.get("track"))
        List<Feature> featureList = sequence.featureLocations
        for (Feature feature : featureList) {
            if (feature instanceof Gene) {
                Gene gene = (Gene) feature
                for (Transcript transcript : gene.getTranscripts()) {
//                    jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(transcript));
                    jsonFeatures.put(transcript as JSON);
                }
            }
            else {
                jsonFeatures.put( feature as JSON)
//                jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(gbolFeature));
            }
        }

        returnObject.put("features",jsonFeatures)

        render returnObject
    }

    def getSequenceAlterations(){
        log.debug  "getting sequence alterations "
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)
        JSONArray jsonFeatures = new JSONArray()
        returnObject.put("features",jsonFeatures)

        // TODO: get alternations from session
//        for (SequenceAlteration alteration : editor.getSession().getSequenceAlterations()) {
//            jsonFeatures.put(JSONUtil.convertBioFeatureToJSON(alteration));
//        }

        render returnObject
    }

    def getOrganism() {
//        JSONObject organism = new JSONObject();
//        if (editor.getSession().getOrganism() == null) {
//            return;
//        }
//        organism.put("genus", editor.getSession().getOrganism().getGenus());
//        organism.put("species", editor.getSession().getOrganism().getSpecies());
//        out.write(organism.toString());

        // the editor is bound to the session
        log.debug  "getting sequence alterations "
        JSONObject returnObject = (JSONObject) JSON.parse(params.data)

        Organism organism = editor.getSession().getOrganism()
        render organism as JSON
//
//
//        JSONArray jsonFeatures = new JSONArray()
//        returnObject.put("features",jsonFeatures)
    }

    private JSONObject createJSONFeatureContainer(JSONObject ... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put("features", jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }


    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    protected String hello(String world) {
        log.debug  "got here! . . . "
        return "hello from controller, ${world}!"
    }

    @MessageMapping("/AnnotationEditorService")
    @SendTo("/topic/AnnotationEditorService")
    protected String annotationEditor(String inputString) {
        log.debug  " annotation editor service ${inputString}"
        return "annotationEditor ${inputString}!"
    }
}
