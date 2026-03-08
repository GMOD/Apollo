package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/14/15.
 */
public class AttributeRestService {

    public static void updateAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo,AttributeInfo oldAttributeInfo,AttributeInfo newAttributeInfo) {

//        0: "SEND↵destination:/app/AnnotationNotification↵content-length:328↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\"old_non_reserved_properties\":[{\"tag\":\"2222\",\"value\":\"3333\"}],\"new_non_reserved_properties\":[{\"tag\":\"777\",\"value\":\"3333\"}]}],\"operation\":\"update_non_reserved_properties\",\"clientToken\":\"18068643442091616983\"}""
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldAttributeJsonArray = new JSONArray();
        JSONObject oldAttributeJsonObject = new JSONObject();
        oldAttributeJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(oldAttributeInfo.getTag()));
        oldAttributeJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(oldAttributeInfo.getValue()));
        oldAttributeJsonArray.set(0, oldAttributeJsonObject);
        featureObject.put(FeatureStringEnum.OLD_NON_RESERVED_PROPERTIES.getValue(), oldAttributeJsonArray);

        JSONArray newAttributeJsonArray = new JSONArray();
        JSONObject newAttributeJsonObject = new JSONObject();
        newAttributeJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(newAttributeInfo.getTag()));
        newAttributeJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(newAttributeInfo.getValue()));
        newAttributeJsonArray.set(0, newAttributeJsonObject);
        featureObject.put(FeatureStringEnum.NEW_NON_RESERVED_PROPERTIES.getValue(), newAttributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateAttribute", "data=" + requestObject.toString());
    }

    public static void addAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo, AttributeInfo attributeInfo) {
//        {"track":"ctgA", "features":[{"uniquename":"fd57cc6a-8e29-4a48-9832-82c06bcc869c", "dbxrefs":[{"db":"asdf", "accession":"zzz"}]}]}
        // 0: "SEND↵destination:/app/AnnotationNotification↵content-length:249↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\"non_reserved_properties\":[{\"tag\":\"1111\",\"value\":\"222\"}]}],\"operation\":\"add_non_reserved_properties\",\"clientToken\":\"18068643442091616983\"}""
        GWT.log("Adding attribute");
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray attributeJsonArray = new JSONArray();
        JSONObject attributeJsonObject = new JSONObject();
        attributeJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(attributeInfo.getTag()));
        attributeJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(attributeInfo.getValue()));
        attributeJsonArray.set(0, attributeJsonObject);
        featureObject.put(FeatureStringEnum.NON_RESERVED_PROPERTIES.getValue(), attributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addAttribute", "data=" + requestObject.toString());
    }

    public static void deleteAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo, AttributeInfo attributeInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray attributeJsonArray = new JSONArray();
        JSONObject attributeJsonObject = new JSONObject();
        attributeJsonObject.put(FeatureStringEnum.TAG.getValue(), new JSONString(attributeInfo.getTag()));
        attributeJsonObject.put(FeatureStringEnum.VALUE.getValue(), new JSONString(attributeInfo.getValue()));
        attributeJsonArray.set(0, attributeJsonObject);
        featureObject.put(FeatureStringEnum.NON_RESERVED_PROPERTIES.getValue(), attributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteAttribute", "data=" + requestObject.toString());
    }

    public static void getAttributes(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject featureObject= new JSONObject();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(),new JSONString(annotationInfo.getUniqueName()));
        featureObject.put(FeatureStringEnum.ORGANISM_ID.getValue(),new JSONString(organismInfo.getId()));
        RestService.sendRequest(requestCallback, "annotationEditor/getAttributes", "data=" + featureObject.toString());
    }
}
