package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.AttributeInfoConverter;
import org.bbop.apollo.gwt.client.dto.DbXRefInfoConverter;
import org.bbop.apollo.gwt.client.dto.AttributeInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/14/15.
 */
public class AttributeRestService {

    public static void updateAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo,AttributeInfo oldAttributeInfo,AttributeInfo newAttributeInfo) {

    //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "old_dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldAttributeJsonArray = new JSONArray();
        JSONObject oldAttributeJsonObject = new JSONObject();
        oldAttributeJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(oldAttributeInfo.getTag()));
        oldAttributeJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(oldAttributeInfo.getValue()));
        oldAttributeJsonArray.set(0, oldAttributeJsonObject);
        featureObject.put(FeatureStringEnum.OLD_DBXREFS.getValue(), oldAttributeJsonArray);

//\"new_dbxrefs\":[{\"db\":\"asdfasdfaaeee\",\"accession\":\"12312\"}]}]
        JSONArray newAttributeJsonArray = new JSONArray();
        JSONObject newAttributeJsonObject = new JSONObject();
        newAttributeJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(newAttributeInfo.getTag()));
        newAttributeJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(newAttributeInfo.getValue()));
        newAttributeJsonArray.set(0, newAttributeJsonObject);
        featureObject.put(FeatureStringEnum.NEW_DBXREFS.getValue(), newAttributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateDbxref", "data=" + requestObject.toString());
    }

    public static void addAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo, AttributeInfo attributeInfo) {
        //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray attributeJsonArray = new JSONArray();
        JSONObject attributeJsonObject = new JSONObject();
        attributeJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(attributeInfo.getTag()));
        attributeJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(attributeInfo.getValue()));
        attributeJsonArray.set(0, attributeJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), attributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addDbxref", "data=" + requestObject.toString());
    }

    public static void deleteAttribute(RequestCallback requestCallback, AnnotationInfo annotationInfo, AttributeInfo attributeInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray attributeJsonArray = new JSONArray();
        JSONObject attributeJsonObject = new JSONObject();
        attributeJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(attributeInfo.getTag()));
        attributeJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(attributeInfo.getValue()));
        attributeJsonArray.set(0, attributeJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), attributeJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteDbxref", "data=" + requestObject.toString());
    }

}
