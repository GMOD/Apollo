package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject

abstract class AbstractApolloController {

    public static String REST_OPERATION = "operation"
    public static String REST_TRACK = "track"
    public static String REST_FEATURES = "features"
    public static REST_USERNAME = "username"
    public static REST_PERMISSION = "permission"
    public static REST_DATA_ADAPTER = "data_adapter"
    public static REST_DATA_ADAPTERS = "data_adapters"
    public static REST_KEY = "key"
    public static REST_OPTIONS = "options"
    public static REST_TRANSLATION_TABLE = "translation_table"

    protected String underscoreToCamelCase(String underscore) {
        if (!underscore || underscore.isAllWhitespace()) {
            return ''
        }
        return underscore.replaceAll(/_\w/) { it[1].toUpperCase() }
    }


    protected JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
        JSONObject jsonFeatureContainer = new JSONObject();
        JSONArray jsonFeatures = new JSONArray();
        jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures);
        for (JSONObject feature : features) {
            jsonFeatures.put(feature);
        }
        return jsonFeatureContainer;
    }

    protected def findPost() {
        for (p in params) {
            String key = p.key
            if (key.contains("operation")) {
//                {"track":"Group9.10","operation":"get_translation_table"}
                if(BookmarkService.isProjectionString(key)){
                    key = fixProjectionJson(key)
                }
//                { "track": "{"projection":"None", "padding":50, "referenceTrack":"Official Gene Set v3.2", "sequences":[{"name":"Group11.18"},{"name":"Group9.10"}]}", "operation": "get_user_permission" }
                // have to remove:
//                :-1..-1
                // in some cases the JSON starts as a string '"{' and in other cases it does not '{' and need to handle both cases
                key = fixTrackString(key)

                try{
                    return (JSONObject) JSON.parse(key)
                }
                catch (e){
                    return (JSONObject) JSON.parse(key)
                }
            }
        }
    }

    protected def fixTrackString(String key){
        key = key.replaceAll("\\\\\"","\"")
        Integer firstIndex = key.indexOf("}:")
        Integer lastIndex = key.indexOf("\"",firstIndex)
        if(firstIndex>0 && lastIndex>0){
//                    :-1..-1"
            def replaceString
//                    if(key.contains("\"{")){
//                        replaceString = key.substring(firstIndex+1,lastIndex)
//                    }
//                    else{
            replaceString = key.substring(firstIndex+1,lastIndex+1)
//                    }
            key = key.replaceAll(replaceString,"")
            key = key.replaceAll("\"\\{\"","{\"")
        }
        return key
    }

    String fixProjectionJson(String s) {
        s = s.replaceAll("\"\\{\"projection","\\{\"projection")
        s = s.replaceAll("\\}\", \"operation\"","\\}, \"operation\"")
//        s = s.replaceAll("\"{\"projection","{\"projection")
    }
}
