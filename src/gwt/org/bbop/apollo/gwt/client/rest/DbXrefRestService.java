package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Anchor;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.DbXRefInfoConverter;
import org.bbop.apollo.gwt.client.dto.DbXrefInfo;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;
import org.gwtbootstrap3.extras.bootbox.client.Bootbox;

/**
 * Created by ndunn on 1/14/15.
 */
public class DbXrefRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static void saveDbXref(RequestCallback requestCallback, DbXrefInfo dbXrefInfo) {
        RestService.sendRequest(requestCallback, "dbXrefInfo/save", "data=" + DbXRefInfoConverter.convertToJson(dbXrefInfo).toString());
    }

    public static void updateDbXref(RequestCallback requestCallback, AnnotationInfo annotationInfo,DbXrefInfo oldDbXrefInfo,DbXrefInfo newDbXrefInfo) {

    //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "old_dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldDbXrefJsonArray = new JSONArray();
        JSONObject oldDbXrefJsonObject = new JSONObject();
        oldDbXrefJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(oldDbXrefInfo.getTag()));
        oldDbXrefJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(oldDbXrefInfo.getValue()));
        oldDbXrefJsonArray.set(0, oldDbXrefJsonObject);
        featureObject.put(FeatureStringEnum.OLD_DBXREFS.getValue(), oldDbXrefJsonArray);

//\"new_dbxrefs\":[{\"db\":\"asdfasdfaaeee\",\"accession\":\"12312\"}]}]
        JSONArray newDbXrefJsonArray = new JSONArray();
        JSONObject newDbXrefJsonObject = new JSONObject();
        newDbXrefJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(newDbXrefInfo.getTag()));
        newDbXrefJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(newDbXrefInfo.getValue()));
        newDbXrefJsonArray.set(0, newDbXrefJsonObject);
        featureObject.put(FeatureStringEnum.NEW_DBXREFS.getValue(), newDbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateDbxref", "data=" + requestObject.toString());
    }

    public static void addDbXref(RequestCallback requestCallback, AnnotationInfo annotationInfo, DbXrefInfo dbXrefInfo) {
        //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(dbXrefInfo.getTag()));
        dbXrefJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(dbXrefInfo.getValue()));
        dbXrefJsonArray.set(0, dbXrefJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addDbxref", "data=" + requestObject.toString());
    }

    public static void deleteDbXref(RequestCallback requestCallback, AnnotationInfo annotationInfo, DbXrefInfo dbXrefInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonObject.put(FeatureStringEnum.DB.getValue(), new JSONString(dbXrefInfo.getTag()));
        dbXrefJsonObject.put(FeatureStringEnum.ACCESSION.getValue(), new JSONString(dbXrefInfo.getValue()));
        dbXrefJsonArray.set(0, dbXrefJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteDbxref", "data=" + requestObject.toString());
    }

    public static void getDbXrefs(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.UNIQUENAME.getValue(),new JSONString(annotationInfo.getUniqueName()));
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.getValue(),new JSONString(organismInfo.getId()));
        RestService.sendRequest(requestCallback, "annotationEditor/getDbxrefs", "data=" + jsonObject.toString());
    }

}
