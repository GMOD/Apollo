package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.MainPanel;
import org.bbop.apollo.gwt.client.dto.*;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/14/15.
 */
public class CommentRestService {

    public static void updateComment(RequestCallback requestCallback, AnnotationInfo annotationInfo,CommentInfo oldCommentInfo,CommentInfo newCommentInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldCommentJsonArray = new JSONArray();
        oldCommentJsonArray.set(0, new JSONString(oldCommentInfo.getComment()));
        featureObject.put(FeatureStringEnum.OLD_COMMENTS.getValue(), oldCommentJsonArray);

        JSONArray newCommentJsonArray = new JSONArray();
        newCommentJsonArray.set(0, new JSONString(newCommentInfo.getComment()));
        featureObject.put(FeatureStringEnum.NEW_COMMENTS.getValue(), newCommentJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateComments", "data=" + requestObject.toString());
    }

    public static void addComment(RequestCallback requestCallback, AnnotationInfo annotationInfo, CommentInfo commentInfo) {
        //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray commentsJsonArray = new JSONArray();
        commentsJsonArray.set(0, new JSONString(commentInfo.getComment()));
        featureObject.put(FeatureStringEnum.COMMENTS.getValue(), commentsJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addComments", "data=" + requestObject.toString());
    }

    public static void deleteComment(RequestCallback requestCallback, AnnotationInfo annotationInfo, CommentInfo commentInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonArray.set(0, new JSONString(commentInfo.getComment()));
        featureObject.put(FeatureStringEnum.COMMENTS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteComments", "data=" + requestObject.toString());
    }

    public static void getComments(RequestCallback requestCallback, AnnotationInfo annotationInfo, OrganismInfo organismInfo) {
        JSONObject dataObject = new JSONObject();
        JSONArray featuresArray = new JSONArray();
        dataObject.put(FeatureStringEnum.FEATURES.getValue(),featuresArray);
        JSONObject featureObject= new JSONObject();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(),new JSONString(annotationInfo.getUniqueName()));
        featureObject.put(FeatureStringEnum.ORGANISM_ID.getValue(),new JSONString(organismInfo.getId()));
        featuresArray.set(0,featureObject);
        RestService.sendRequest(requestCallback, "annotationEditor/getComments", "data=" + dataObject.toString());
    }

    public static void getCannedComments(RequestCallback requestCallback, AnnotationInfo internalAnnotationInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.TYPE.getValue(),new JSONString(internalAnnotationInfo.getType()));
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.getValue(), new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));
        RestService.sendRequest(requestCallback, "annotationEditor/getCannedComments", "data=" +jsonObject.toString() );
    }

    public static void getCannedKeys(RequestCallback requestCallback, AnnotationInfo internalAnnotationInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.TYPE.getValue(),new JSONString(internalAnnotationInfo.getType()));
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.getValue(), new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));
        RestService.sendRequest(requestCallback, "annotationEditor/getCannedKeys", "data=" +jsonObject.toString() );
    }

    public static void getCannedValues(RequestCallback requestCallback, AnnotationInfo internalAnnotationInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.TYPE.getValue(),new JSONString(internalAnnotationInfo.getType()));
        jsonObject.put(FeatureStringEnum.ORGANISM_ID.getValue(), new JSONString(MainPanel.getInstance().getCurrentOrganism().getId()));
        RestService.sendRequest(requestCallback, "annotationEditor/getCannedValues", "data=" +jsonObject.toString() );
    }
}
