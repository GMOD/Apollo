package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.StatusInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/14/15.
 */
public class AvailableStatusRestService {

    public static void updateStatus(RequestCallback requestCallback, AnnotationInfo annotationInfo,StatusInfo oldStatusInfo,StatusInfo newStatusInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldStatusJsonArray = new JSONArray();
        oldStatusJsonArray.set(0, new JSONString(oldStatusInfo.getStatus()));
        featureObject.put(FeatureStringEnum.OLD_COMMENTS.getValue(), oldStatusJsonArray);

        JSONArray newStatusJsonArray = new JSONArray();
        newStatusJsonArray.set(0, new JSONString(newStatusInfo.getStatus()));
        featureObject.put(FeatureStringEnum.NEW_COMMENTS.getValue(), newStatusJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateStatuses", "data=" + requestObject.toString());
    }

    public static void addStatus(RequestCallback requestCallback, AnnotationInfo annotationInfo, StatusInfo commentInfo) {
        //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray commentsJsonArray = new JSONArray();
        commentsJsonArray.set(0, new JSONString(commentInfo.getStatus()));
        featureObject.put(FeatureStringEnum.COMMENTS.getValue(), commentsJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addStatuses", "data=" + requestObject.toString());
    }

    public static void deleteStatus(RequestCallback requestCallback, AnnotationInfo annotationInfo, StatusInfo commentInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonArray.set(0, new JSONString(commentInfo.getStatus()));
        featureObject.put(FeatureStringEnum.COMMENTS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteStatuses", "data=" + requestObject.toString());
    }

    public static void getAvailableStatuses(RequestCallback requestCallback, AnnotationInfo internalAnnotationInfo) {
        getAvailableStatuses(requestCallback,internalAnnotationInfo.getType(),MainPanel.getInstance().getCurrentOrganism().getId());
    }

    public static void getAvailableStatuses(RequestCallback requestCallback) {
        getAvailableStatuses(requestCallback,null,MainPanel.getInstance().getCurrentOrganism().getId());
    }

    public static void getAvailableStatuses(RequestCallback requestCallback, String type,String organismId) {
        JSONObject jsonObject = new JSONObject();
        if(type!=null){
            jsonObject.put(FeatureStringEnum.TYPE.getValue(),new JSONString(type));
        }
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.getValue(), new JSONString(organismId));
        RestService.sendRequest(requestCallback, "annotationEditor/getAvailableStatuses", "data=" +jsonObject.toString() );
    }

}
