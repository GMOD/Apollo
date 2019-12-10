package org.bbop.apollo.gwt.client.rest;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.bbop.apollo.gwt.client.dto.AnnotationInfo;
import org.bbop.apollo.gwt.client.dto.CommentInfoConverter;
import org.bbop.apollo.gwt.client.dto.DbXRefInfoConverter;
import org.bbop.apollo.gwt.client.dto.CommentInfo;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 1/14/15.
 */
public class CommentRestService {

    static String TERM_LOOKUP_SERVER = "http://api.geneontology.org/api/ontology/term/"; // ECO%3A0000315

    public static void saveComment(RequestCallback requestCallback, CommentInfo commentInfo) {
        RestService.sendRequest(requestCallback, "commentInfo/save", "data=" + CommentInfoConverter.convertToJson(commentInfo).toString());
    }

    public static void updateComment(RequestCallback requestCallback, AnnotationInfo annotationInfo,CommentInfo oldCommentInfo,CommentInfo newCommentInfo) {

    //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "old_dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray oldCommentJsonArray = new JSONArray();
        JSONObject oldCommentJsonObject = new JSONObject();
        oldCommentJsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(oldCommentInfo.getComment()));
        oldCommentJsonArray.set(0, oldCommentJsonObject);
        featureObject.put(FeatureStringEnum.OLD_DBXREFS.getValue(), oldCommentJsonArray);

//\"new_dbxrefs\":[{\"db\":\"asdfasdfaaeee\",\"accession\":\"12312\"}]}]
        JSONArray newCommentJsonArray = new JSONArray();
        JSONObject newCommentJsonObject = new JSONObject();
        newCommentJsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(newCommentInfo.getComment()));
        newCommentJsonArray.set(0, newCommentJsonObject);
        featureObject.put(FeatureStringEnum.NEW_DBXREFS.getValue(), newCommentJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);

        RestService.sendRequest(requestCallback, "annotationEditor/updateDbxref", "data=" + requestObject.toString());
    }

    public static void addComment(RequestCallback requestCallback, AnnotationInfo annotationInfo, CommentInfo commentInfo) {
        //            0: "SEND↵destination:/app/AnnotationNotification↵content-length:310↵↵"{\"track\":\"ctgA\",\"features\":[{\"uniquename\":\"fd57cc6a-8e29-4a48-9832-82c06bcc869c\",\,\"operation\":\"update_non_primary_dbxrefs\",\"clientToken\":\"18068643442091616983\"}""
//        "dbxrefs\":[{\"db\":\"aasd\",\"accession\":\"12312\"}],
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(commentInfo.getComment()));
        dbXrefJsonArray.set(0, dbXrefJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/addDbxref", "data=" + requestObject.toString());
    }

    public static void deleteComment(RequestCallback requestCallback, AnnotationInfo annotationInfo, CommentInfo commentInfo) {
        JSONArray featuresArray = new JSONArray();
        JSONObject featureObject = new JSONObject();
        String featureUniqueName = annotationInfo.getUniqueName();
        featureObject.put(FeatureStringEnum.UNIQUENAME.getValue(), new JSONString(featureUniqueName));
        JSONArray dbXrefJsonArray = new JSONArray();
        JSONObject dbXrefJsonObject = new JSONObject();
        dbXrefJsonObject.put(FeatureStringEnum.COMMENT.getValue(), new JSONString(commentInfo.getComment()));
        dbXrefJsonArray.set(0, dbXrefJsonObject);
        featureObject.put(FeatureStringEnum.DBXREFS.getValue(), dbXrefJsonArray);
        featuresArray.set(0, featureObject);

        JSONObject requestObject = new JSONObject();
        requestObject.put(FeatureStringEnum.TRACK.getValue(), new JSONString(annotationInfo.getSequence()));
        requestObject.put(FeatureStringEnum.FEATURES.getValue(), featuresArray);
        RestService.sendRequest(requestCallback, "annotationEditor/deleteDbxref", "data=" + requestObject.toString());
    }

}
