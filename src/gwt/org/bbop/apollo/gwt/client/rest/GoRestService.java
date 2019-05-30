package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Anchor;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class GoRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static void saveGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/save", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void updateGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/update", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void deleteGoAnnotation(RequestCallback requestCallback, GoAnnotation goAnnotation) {
        RestService.sendRequest(requestCallback, "goAnnotation/delete", "data=" + GoAnnotationConverter.convertToJson(goAnnotation).toString());
    }

    public static void getGoAnnotation(RequestCallback requestCallback, String featureUniqueName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uniqueName",new JSONString(featureUniqueName));
        RestService.sendRequest(requestCallback, "goAnnotation/", "data=" + jsonObject.toString());
    }

    private static void lookupTerm(RequestCallback requestCallback, String url) {
        RestService.generateBuilder(requestCallback,RequestBuilder.GET,url);
    }

    public static void lookupTerm(final Anchor anchor, String evidenceCurie) {
        RequestCallback requestCallback = new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                JSONObject returnObject = JSONParser.parseStrict(response.getText()).isObject();
                anchor.setHTML(returnObject.get("label").isString().stringValue());
                if(returnObject.containsKey("definition")){
                    anchor.setTitle(returnObject.get("definition").isString().stringValue());
                }
                else{
                    anchor.setTitle(returnObject.get("label").isString().stringValue());
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                Bootbox.alert("Failed to do lookup: "+exception.getMessage());
            }
        };

        GoRestService.lookupTerm(requestCallback,TERM_LOOKUP_SERVER + evidenceCurie);
    }
}
