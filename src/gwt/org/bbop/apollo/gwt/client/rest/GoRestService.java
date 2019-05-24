package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import grails.plugins.rest.client.RestBuilder;
import org.bbop.apollo.gwt.client.dto.GoAnnotationConverter;
import org.bbop.apollo.gwt.shared.go.GoAnnotation;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class GoRestService {

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

    public static void lookupTerm(RequestCallback requestCallback, String url) {
        RequestBuilder builder = RestService.generateBuilder(requestCallback,RequestBuilder.GET,url);
    }

}
